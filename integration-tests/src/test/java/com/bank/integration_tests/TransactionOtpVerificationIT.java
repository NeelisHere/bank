package com.bank.integration_tests;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * OTP verification test for the fraud flow.
 *
 * How to use:
 *   1. Run TransactionFraudFlowIT — copy the transactionId from the logs.
 *   2. Check notification-service logs for the OTP.
 *   3. Paste both values into TRANSACTION_ID and OTP below.
 *   4. Run this class.
 */
@Slf4j
class TransactionOtpVerificationIT {

    private static final String TRANSACTION_SERVICE_URL = "http://localhost:8082";
    private static final String ACCOUNTS_SERVICE_URL    = "http://localhost:8081";

    private static final String SENDER_ACCOUNT   = "740736531443220";
    private static final String RECEIVER_ACCOUNT = "935006034670194";

    private static final int    POLL_INTERVAL_MS = 2_000;
    private static final int    MAX_WAIT_MS      = 30_000;
    private static final double AMOUNT           = 50_000.0;

    // ✏️ PASTE VALUES HERE BEFORE RUNNING
    private static final String TRANSACTION_ID = "24cda354-4d3f-4189-916e-0033fbe09a58";
    private static final String OTP            = "246015";

    @Test
    void otpVerification_correctOtp_completesTransactionAndCreditsReceiver() throws InterruptedException {
        // ── 1. Snapshot current balances (sender already deducted) ────────────
        double senderBalanceBefore   = getBalance(SENDER_ACCOUNT);
        double receiverBalanceBefore = getBalance(RECEIVER_ACCOUNT);
        log.info("[SETUP] transactionId: {} | OTP: {}", TRANSACTION_ID, OTP);
        log.info("[SETUP] Sender balance: {} | Receiver balance: {}", senderBalanceBefore, receiverBalanceBefore);

        // ── 2. POST /verify with OTP ──────────────────────────────────────────
        log.info("[STEP 1] Submitting OTP...");
        Response verifyResponse = given()
                .baseUri(TRANSACTION_SERVICE_URL)
                .when()
                .post("/api/v1/transactions/{transactionId}/verify?otp={otp}", TRANSACTION_ID, OTP)
                .then()
                .statusCode(200)
                .extract().response();
        log.info("[STEP 1] Status after OTP submit: {}", verifyResponse.jsonPath().getString("transactionStatus"));

        // ── 3. Poll until COMPLETED ───────────────────────────────────────────
        log.info("[STEP 2] Polling for COMPLETED...");
        String finalStatus = pollUntilStatus(TRANSACTION_ID, "COMPLETED");
        log.info("[STEP 2] Final status: {}", finalStatus);
        assertThat(finalStatus)
                .as("Transaction should reach COMPLETED after correct OTP")
                .isEqualTo("COMPLETED");

        // ── 4. Poll until receiver balance is credited ────────────────────────
        double expectedReceiverBalance = receiverBalanceBefore + AMOUNT;
        log.info("[STEP 3] Polling for receiver balance (expected: {})...", expectedReceiverBalance);
        double receiverBalanceAfter = pollUntilBalanceEquals(RECEIVER_ACCOUNT, expectedReceiverBalance);
        log.info("[STEP 3] Receiver balance after: {} (expected: {})", receiverBalanceAfter, expectedReceiverBalance);
        assertThat(receiverBalanceAfter)
                .as("Receiver balance should have increased by %.2f", AMOUNT)
                .isEqualTo(expectedReceiverBalance);

        // ── 5. Log sender balance for visibility (already deducted earlier) ───
        double senderBalanceAfter = getBalance(SENDER_ACCOUNT);
        log.info("[INFO] Sender balance after: {} (unchanged since deduction happened in TransactionFraudFlowIT)", senderBalanceAfter);

        log.info("[DONE] OTP verification test PASSED for transactionId: {}", TRANSACTION_ID);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String pollUntilStatus(String transactionId, String targetStatus) throws InterruptedException {
        long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
        String status = "";
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_INTERVAL_MS);
            status = given()
                    .baseUri(TRANSACTION_SERVICE_URL)
                    .when()
                    .get("/api/v1/transactions/{id}", transactionId)
                    .then()
                    .statusCode(200)
                    .extract().jsonPath().getString("transactionStatus");
            log.info("[POLL] transactionId: {} — current status: {}", transactionId, status);
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
