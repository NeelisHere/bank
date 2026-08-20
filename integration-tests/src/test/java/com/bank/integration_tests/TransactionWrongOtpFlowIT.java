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
 * Fraud + wrong OTP flow integration test.
 *
 * Test 1 — fraudTransfer_90PercentBalance_pendingVerification:
 *   Fetches sender balance, calculates 90%+ of it as the transfer amount.
 *   Sends the transfer — fraud detection triggers → PENDING_VERIFICATION.
 *   Asserts sender was deducted immediately.
 *
 * Test 2 — wrongOtp_refundsAndBlocksSender:
 *   Submits OTP "000000" (wrong) to the verify endpoint.
 *   Asserts transaction reaches FLAGGED (refund path).
 *   Polls until sender balance is refunded.
 *   Asserts sender account status is BLOCKED.
 *
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransactionWrongOtpFlowIT {

    private static final String TRANSACTION_SERVICE_URL = "http://localhost:8082";
    private static final String ACCOUNTS_SERVICE_URL    = "http://localhost:8081";

    private static final String SENDER_ACCOUNT   = "740736531443220";
    private static final String RECEIVER_ACCOUNT = "935006034670194";
    private static final String WRONG_OTP        = "000000";

    private static final int    POLL_INTERVAL_MS = 2_000;
    private static final int    MAX_WAIT_MS      = 30_000;

    // Shared across both tests in the same JVM run
    private static String transactionId;
    private static double transferAmount;
    private static double senderBalanceBeforeTransfer;

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 1 — Transfer >90% of sender balance → PENDING_VERIFICATION
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @Order(1)
    void fraudTransfer_90PercentBalance_pendingVerification() throws InterruptedException {

        // ── 1. Fetch sender balance and compute >90% amount ───────────────────
        senderBalanceBeforeTransfer = getBalance(SENDER_ACCOUNT);
        transferAmount = Math.floor(senderBalanceBeforeTransfer * 0.91 * 100) / 100; // 91%, 2dp
        log.info("[SETUP] Sender balance: {} | Transfer amount (91%%): {}", senderBalanceBeforeTransfer, transferAmount);
        assertThat(transferAmount).as("Transfer amount must be > 0").isGreaterThan(0);

        // ── 2. POST /transfer ─────────────────────────────────────────────────
        log.info("[STEP 1] Initiating transfer of {} from {} to {}", transferAmount, SENDER_ACCOUNT, RECEIVER_ACCOUNT);
        String body = """
                {
                  "senderAccountNumber": "%s",
                  "receiverAccountNumber": "%s",
                  "amount": %s
                }
                """.formatted(SENDER_ACCOUNT, RECEIVER_ACCOUNT, transferAmount);

        Response transferResponse = given()
                .baseUri(TRANSACTION_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/transactions/transfer")
                .then()
                .statusCode(201)
                .extract().response();

        transactionId        = transferResponse.jsonPath().getString("id");
        String initialStatus = transferResponse.jsonPath().getString("transactionStatus");
        log.info("[STEP 1] transactionId: {} | initial status: {}", transactionId, initialStatus);
        assertThat(transactionId).isNotNull();

        // ── 3. Poll until PENDING_VERIFICATION ───────────────────────────────
        log.info("[STEP 2] Polling until PENDING_VERIFICATION...");
        String status = pollUntilStatus(transactionId, "PENDING_VERIFICATION");
        log.info("[STEP 2] Status: {}", status);
        assertThat(status)
                .as("High-balance transaction should reach PENDING_VERIFICATION")
                .isEqualTo("PENDING_VERIFICATION");

        // ── 4. Poll until sender balance reflects deduction ───────────────────
        double expectedSenderBalance = senderBalanceBeforeTransfer - transferAmount;
        log.info("[STEP 3] Polling for sender deduction (expected: {})...", expectedSenderBalance);
        double senderBalanceAfterDeduction = pollUntilBalanceEquals(SENDER_ACCOUNT, expectedSenderBalance);
        log.info("[STEP 3] Sender balance after deduction: {}", senderBalanceAfterDeduction);
        assertThat(senderBalanceAfterDeduction)
                .as("Sender should have been deducted by %.2f", transferAmount)
                .isEqualTo(expectedSenderBalance);

        log.info("[DONE] Test 1 PASSED — transactionId {} is awaiting OTP", transactionId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 2 — Submit wrong OTP → FLAGGED, sender refunded and blocked
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @Order(2)
    void wrongOtp_refundsAndBlocksSender() throws InterruptedException {
        // ── 1. Submit wrong OTP ───────────────────────────────────────────────
        log.info("[STEP 1] Submitting wrong OTP '{}' for transactionId: {}", WRONG_OTP, transactionId);
        Response verifyResponse = given()
                .baseUri(TRANSACTION_SERVICE_URL)
                .when()
                .post("/api/v1/transactions/{transactionId}/verify?otp={otp}", transactionId, WRONG_OTP)
                .then()
                .statusCode(200)
                .extract().response();
        log.info("[STEP 1] Status after wrong OTP: {}", verifyResponse.jsonPath().getString("transactionStatus"));

        // ── 2. Poll until FLAGGED ─────────────────────────────────────────────
        log.info("[STEP 2] Polling for FLAGGED status...");
        String finalStatus = pollUntilStatus(transactionId, "FLAGGED");
        log.info("[STEP 2] Final transaction status: {}", finalStatus);
        assertThat(finalStatus)
                .as("Transaction should be FLAGGED after wrong OTP")
                .isEqualTo("FLAGGED");

        // ── 3. Poll until sender balance is refunded ──────────────────────────
        // Wrong OTP triggers compensateTransaction() which credits back the sender
        log.info("[STEP 3] Polling for sender refund (expected: {})...", senderBalanceBeforeTransfer);
        double senderBalanceAfterRefund = pollUntilBalanceEquals(SENDER_ACCOUNT, senderBalanceBeforeTransfer);
        log.info("[STEP 3] Sender balance after refund: {} (expected: {})", senderBalanceAfterRefund, senderBalanceBeforeTransfer);
        assertThat(senderBalanceAfterRefund)
                .as("Sender should be fully refunded back to %.2f after wrong OTP", senderBalanceBeforeTransfer)
                .isEqualTo(senderBalanceBeforeTransfer);

        // ── 4. Poll until sender account is BLOCKED ───────────────────────────
        log.info("[STEP 4] Polling for sender account status to be BLOCKED...");
        String accountStatus = pollUntilAccountStatus(SENDER_ACCOUNT, "BLOCKED");
        log.info("[STEP 4] Sender account status: {}", accountStatus);
        assertThat(accountStatus)
                .as("Sender account should be BLOCKED after wrong OTP")
                .isEqualTo("BLOCKED");

        // ── 5. Log receiver balance — should be unchanged ─────────────────────
        double receiverBalance = getBalance(RECEIVER_ACCOUNT);
        log.info("[INFO] Receiver balance (should be unchanged): {}", receiverBalance);

        log.info("[DONE] Wrong OTP test PASSED — sender refunded and blocked for transactionId: {}", transactionId);
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

    private String pollUntilAccountStatus(String accountNumber, String targetStatus) throws InterruptedException {
        long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
        String status = "";
        while (System.currentTimeMillis() < deadline) {
            status = given()
                    .baseUri(ACCOUNTS_SERVICE_URL)
                    .when()
                    .get("/api/v1/accounts/{accountNumber}", accountNumber)
                    .then()
                    .statusCode(200)
                    .extract().jsonPath().getString("accountStatus");
            log.info("[ACCOUNT POLL] account: {} — status: {}", accountNumber, status);
            if (status.equals(targetStatus)) break;
            Thread.sleep(POLL_INTERVAL_MS);
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
