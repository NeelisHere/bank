package com.bank.integration_tests;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rapid-fire fraud + OTP expiry flow integration test.
 *
 * Test 1 — rapidTransactions_triggerFraud:
 *   Fires N (>5) rapid transfers of 10 each in quick succession.
 *   The velocity fraud rule flags the last one → PENDING_VERIFICATION.
 *   Asserts sender was deducted N * 10 (all N deductions happen upfront).
 *
 * Test 2 — expiredOtp_refundsSenderAndFlagsTransaction:
 *   Waits 2+ minutes for the OTP to expire in Redis (TTL = 60s, wait = 125s).
 *   Calls POST /verify with a dummy OTP — OTP key is gone from Redis.
 *   Transaction-service compensates: only the Nth (flagged) transaction's
 *   amount is refunded to the sender. The N-1 completed transactions are
 *   NOT reversed — receiver keeps those credits.
 *
 *   Sender assertion : balance = initial - (N-1) * amount  (1 refund applied)
 *   Receiver assertion: balance = initial + (N-1) * amount  (only N-1 credited)
 *
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransactionOtpExpiryFlowIT {

    private static final String TRANSACTION_SERVICE_URL = "http://localhost:8082";
    private static final String ACCOUNTS_SERVICE_URL    = "http://localhost:8081";

    private static final String SENDER_ACCOUNT   = "740736531443220";
    private static final String RECEIVER_ACCOUNT = "935006034670194";

    private static final int    N                  = 6;    // number of rapid transfers — must be > 5
    private static final double TRANSFER_AMOUNT    = 10.0;
    private static final int    POLL_INTERVAL_MS   = 2_000;
    private static final int    MAX_WAIT_MS        = 30_000;
    private static final int    OTP_EXPIRY_WAIT_MS = 125_000; // 125s — OTP TTL is 60s, extra buffer

    // Shared across both tests in the same JVM run
    private static String flaggedTransactionId;
    private static double senderBalanceBeforeTransfers;
    private static double receiverBalanceBeforeTransfers;

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 1 — Fire N rapid transfers → velocity fraud rule → PENDING_VERIFICATION
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @Order(1)
    void rapidTransactions_triggerFraud_pendingVerification() throws InterruptedException {

        // ── 1. Snapshot sender and receiver balances before any transfers ──────
        senderBalanceBeforeTransfers   = getBalance(SENDER_ACCOUNT);
        receiverBalanceBeforeTransfers = getBalance(RECEIVER_ACCOUNT);
        log.info("[SETUP] Sender balance before transfers: {} | Receiver balance before transfers: {}",
                senderBalanceBeforeTransfers, receiverBalanceBeforeTransfers);

        // ── 2. Fire N transfers rapidly ───────────────────────────────────────
        log.info("[STEP 1] Firing {} rapid transfers of {} each...", N, TRANSFER_AMOUNT);
        String lastTransactionId = null;

        for (int i = 1; i <= N; i++) {
            String body = """
                    {
                      "senderAccountNumber": "%s",
                      "receiverAccountNumber": "%s",
                      "amount": %s
                    }
                    """.formatted(SENDER_ACCOUNT, RECEIVER_ACCOUNT, TRANSFER_AMOUNT);

            Response response = given()
                    .baseUri(TRANSACTION_SERVICE_URL)
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post("/api/v1/transactions/transfer")
                    .then()
                    .statusCode(201)
                    .extract().response();

            lastTransactionId = response.jsonPath().getString("id");
            log.info("[STEP 1] Transfer {}/{} — transactionId: {}, status: {}",
                    i, N, lastTransactionId, response.jsonPath().getString("transactionStatus"));
        }

        assertThat(lastTransactionId).isNotNull();

        // ── 3. Poll the last transaction until PENDING_VERIFICATION ───────────
        // Velocity fraud check fires after enough rapid transactions — the last
        // one in the burst should get flagged
        log.info("[STEP 2] Polling last transaction {} for PENDING_VERIFICATION...", lastTransactionId);
        String status = pollUntilStatus(lastTransactionId, "PENDING_VERIFICATION");
        log.info("[STEP 2] Status: {}", status);
        assertThat(status)
                .as("Last transaction in rapid burst should reach PENDING_VERIFICATION")
                .isEqualTo("PENDING_VERIFICATION");

        flaggedTransactionId = lastTransactionId;

        // ── 4. Assert sender was deducted for the flagged transaction ─────────
        double expectedBalance = senderBalanceBeforeTransfers - (N * TRANSFER_AMOUNT);
        log.info("[STEP 3] Polling for sender balance deduction (expected: {})...", expectedBalance);
        double senderBalanceAfter = pollUntilBalanceEquals(SENDER_ACCOUNT, expectedBalance);
        log.info("[STEP 3] Sender balance after {} deductions: {}", N, senderBalanceAfter);
        assertThat(senderBalanceAfter)
                .as("Sender should have been deducted %.2f x %d = %.2f", TRANSFER_AMOUNT, N, N * TRANSFER_AMOUNT)
                .isEqualTo(expectedBalance);

        log.info("[DONE] Test 1 PASSED — flaggedTransactionId: {} | OTP generated, waiting for expiry...", flaggedTransactionId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 2 — Wait for OTP expiry → call verify → sender refunded, FLAGGED
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @Order(2)
    void expiredOtp_refundsSenderAndFlagsTransaction() throws InterruptedException {
        assertThat(flaggedTransactionId).as("Test 1 must run first — flaggedTransactionId is null").isNotNull();

        // ── 1. Wait for OTP to expire ─────────────────────────────────────────
        log.info("[STEP 1] Waiting {}s for OTP to expire in Redis...", OTP_EXPIRY_WAIT_MS / 1000);
        for (int elapsed = 0; elapsed < OTP_EXPIRY_WAIT_MS; elapsed += 10_000) {
            Thread.sleep(10_000);
            log.info("[STEP 1] Waited {}s / {}s...", (elapsed + 10_000) / 1000, OTP_EXPIRY_WAIT_MS / 1000);
        }
        log.info("[STEP 1] OTP should now be expired.");

        // ── 2. Call verify with any OTP — Redis key is gone, triggers refund ──
        log.info("[STEP 2] Calling verify endpoint — OTP expired, refund should trigger...");
        Response verifyResponse = given()
                .baseUri(TRANSACTION_SERVICE_URL)
                .when()
                .post("/api/v1/transactions/{transactionId}/verify?otp={otp}", flaggedTransactionId, "000000")
                .then()
                .statusCode(200)
                .extract().response();
        log.info("[STEP 2] Status after expired OTP verify: {}", verifyResponse.jsonPath().getString("transactionStatus"));

        // ── 3. Poll until transaction is FLAGGED ─────────────────────────────
        log.info("[STEP 3] Polling for FLAGGED status...");
        String finalStatus = pollUntilStatus(flaggedTransactionId, "FLAGGED");
        log.info("[STEP 3] Final transaction status: {}", finalStatus);
        assertThat(finalStatus)
                .as("Transaction should be FLAGGED after expired OTP")
                .isEqualTo("FLAGGED");

        // ── 4. Poll until sender balance is refunded ──────────────────────────
        // Only the Nth (flagged) transaction's amount is refunded — the N-1
        // completed transactions are not reversed.
        double expectedSenderBalance = senderBalanceBeforeTransfers - ((N - 1) * TRANSFER_AMOUNT);
        log.info("[STEP 4] Polling for sender refund (expected: {})...", expectedSenderBalance);
        double senderBalanceAfterRefund = pollUntilBalanceEquals(SENDER_ACCOUNT, expectedSenderBalance);
        log.info("[STEP 4] Sender balance after refund: {} (expected: {})", senderBalanceAfterRefund, expectedSenderBalance);
        assertThat(senderBalanceAfterRefund)
                .as("Sender should be refunded only the flagged transaction amount (%.2f); " +
                    "net deduction = (N-1) * %.2f = %.2f",
                    TRANSFER_AMOUNT, TRANSFER_AMOUNT, (N - 1) * TRANSFER_AMOUNT)
                .isEqualTo(expectedSenderBalance);

        // ── 5. Poll until receiver balance reflects exactly N-1 credits ───────
        // The flagged (Nth) transaction never completed → its credit event was
        // never published → receiver balance must settle at initial + (N-1)*amount.
        // We poll here because the N-1 transaction.completed events are delivered
        // asynchronously via Pub/Sub; the last few may still be in-flight.
        double expectedReceiverBalance = receiverBalanceBeforeTransfers + ((N - 1) * TRANSFER_AMOUNT);
        log.info("[STEP 5] Polling for receiver balance (expected: {})...", expectedReceiverBalance);
        double receiverBalanceAfterRefund = pollUntilBalanceEquals(RECEIVER_ACCOUNT, expectedReceiverBalance);
        log.info("[STEP 5] Receiver balance: {} (expected: {})", receiverBalanceAfterRefund, expectedReceiverBalance);
        assertThat(receiverBalanceAfterRefund)
                .as("Receiver should have been credited only N-1 (=%d) times at %.2f each; " +
                    "flagged transaction was never completed so no credit was issued",
                    N - 1, TRANSFER_AMOUNT)
                .isEqualTo(expectedReceiverBalance);

        log.info("[DONE] OTP expiry test PASSED for transactionId: {}", flaggedTransactionId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String pollUntilStatus(String txnId, String targetStatus) throws InterruptedException {
        long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
        String status = "";
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_INTERVAL_MS);
            status = given()
                    .baseUri(TRANSACTION_SERVICE_URL)
                    .when()
                    .get("/api/v1/transactions/{id}", txnId)
                    .then()
                    .statusCode(200)
                    .extract().jsonPath().getString("transactionStatus");
            log.info("[POLL] transactionId: {} — status: {}", txnId, status);
            if (status.equals(targetStatus) || "COMPLETED".equals(status) || "FLAGGED".equals(status) || "FAILED".equals(status)) {
                break;
            }
        }
        return status;
    }

    private double pollUntilBalanceEquals(String accountNumber, double expectedBalance) throws InterruptedException {
        long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
        double balance = getBalance(accountNumber);
        while (System.currentTimeMillis() < deadline) {
            if (balance == expectedBalance) break;
            log.info("[BALANCE POLL] account: {} — current: {}, waiting for: {}", accountNumber, balance, expectedBalance);
            Thread.sleep(POLL_INTERVAL_MS);
            balance = getBalance(accountNumber);
        }
        return balance;
    }

    private double getBalance(String accountNumber) {
        double balance = given()
                .baseUri(ACCOUNTS_SERVICE_URL)
                .when()
                .get("/api/v1/accounts/{accountNumber}/balance", accountNumber)
                .then()
                .statusCode(200)
                .extract().jsonPath().getDouble("balance");
        log.info("[BALANCE] account: {} — balance: {}", accountNumber, balance);
        return balance;
    }
}
