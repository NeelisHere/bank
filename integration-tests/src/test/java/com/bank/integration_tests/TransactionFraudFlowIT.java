package com.bank.integration_tests;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fraud-path integration test.
 *
 * Prerequisites (all services must be running locally):
 *   - transaction-service : localhost:8082
 *   - accounts-service    : localhost:8081
 *
 * Sends a suspiciously high amount (50_000) to trigger fraud detection.
 * Asserts the transaction reaches PENDING_VERIFICATION.
 * Asserts sender balance has been deducted.
 * Log the transactionId — needed for TransactionOtpVerificationIT.
 */
@Slf4j
class TransactionFraudFlowIT {

    private static final String TRANSACTION_SERVICE_URL = "http://localhost:8082";
    private static final String ACCOUNTS_SERVICE_URL    = "http://localhost:8081";

    static final String SENDER_ACCOUNT   = "740736531443220";
    static final String RECEIVER_ACCOUNT = "935006034670194";
    private static final double AMOUNT = 50_000.0;

    private static final int POLL_INTERVAL_MS = 2_000;
    private static final int MAX_WAIT_MS      = 30_000;

    @Test
    void fraudTransfer_highAmount_generatesOtp() throws InterruptedException {

        // ── 1. Snapshot sender balance before transfer ────────────────────────
        double senderBalanceBefore = getBalance(SENDER_ACCOUNT);
        log.info("[SETUP] Sender balance before: {}", senderBalanceBefore);

        // ── 2. POST /transfer with suspiciously high amount ───────────────────
        log.info("[STEP 1] Initiating HIGH-AMOUNT transfer of {} from {} to {}", AMOUNT, SENDER_ACCOUNT, RECEIVER_ACCOUNT);
        String body = """
                {
                  "senderAccountNumber": "%s",
                  "receiverAccountNumber": "%s",
                  "amount": %s
                }
                """.formatted(SENDER_ACCOUNT, RECEIVER_ACCOUNT, AMOUNT);

        Response transferResponse = given()
                .baseUri(TRANSACTION_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/transactions/transfer")
                .then()
                .statusCode(201)
                .extract().response();

        String transactionId = transferResponse.jsonPath().getString("id");
        String initialStatus = transferResponse.jsonPath().getString("transactionStatus");
        log.info("[STEP 1] Transfer response — transactionId: {}, status: {}", transactionId, initialStatus);
        assertThat(transactionId).isNotNull();

        // ── 3. Poll until PENDING_VERIFICATION ───────────────────────────────
        log.info("[STEP 2] Polling until PENDING_VERIFICATION...");
        String finalStatus = pollUntilStatus(transactionId, "PENDING_VERIFICATION");
        log.info("[STEP 2] Final status: {}", finalStatus);
        assertThat(finalStatus)
                .as("High-amount transaction should reach PENDING_VERIFICATION")
                .isEqualTo("PENDING_VERIFICATION");
        log.info("[INFO] OTP generated — check notification-service logs. transactionId: {}", transactionId);

        // ── 4. Poll until sender balance reflects deduction ───────────────────
        double expectedSenderBalance = senderBalanceBefore - AMOUNT;
        log.info("[STEP 3] Polling for sender balance deduction (expected: {})...", expectedSenderBalance);
        double senderBalanceAfter = pollUntilBalanceEquals(SENDER_ACCOUNT, expectedSenderBalance);
        log.info("[STEP 3] Sender balance after: {} (expected: {})", senderBalanceAfter, expectedSenderBalance);
        assertThat(senderBalanceAfter)
                .as("Sender balance should have been deducted by %.2f", AMOUNT)
                .isEqualTo(expectedSenderBalance);
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
