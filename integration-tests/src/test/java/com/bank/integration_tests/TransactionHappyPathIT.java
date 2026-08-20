package com.bank.integration_tests;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Happy-path integration test for the transfer flow.
 *
 * Prerequisites (all services must be running locally):
 *   - transaction-service : localhost:8082
 *   - accounts-service    : localhost:8081
 *
 * The test:
 *   1. Snapshots sender and receiver balances before the transfer.
 *   2. Calls POST /transfer.
 *   3. Asserts the transaction is saved with status PENDING or PROCESSING.
 *   4. Polls GET /{transactionId} until status is COMPLETED (up to 15 s).
 *   5. Asserts the receiver balance has increased by the transferred amount.
 *   6. Asserts the sender balance has decreased by the transferred amount.
 */
@Slf4j
class TransactionHappyPathIT {

    private static final String TRANSACTION_SERVICE_URL = "http://localhost:8082";
    private static final String ACCOUNTS_SERVICE_URL    = "http://localhost:8081";

    private static final String SENDER_ACCOUNT   = "740736531443220";
    private static final String RECEIVER_ACCOUNT = "935006034670194";
    private static final double TRANSFER_AMOUNT  = 10.0;

    // Poll every 2 s, give up after 15 s
    private static final int POLL_INTERVAL_MS = 2_000;
    private static final int MAX_WAIT_MS      = 30_000;

    @Test
    void transfer_happyPath_transactionCompletedAndReceiverCredited() throws InterruptedException {

        // ── 1. Snapshot balances before transfer ─────────────────────────────
        double receiverBalanceBefore = getBalance(RECEIVER_ACCOUNT);
        double senderBalanceBefore   = getBalance(SENDER_ACCOUNT);
        log.info("[SETUP] Sender balance before: {} | Receiver balance before: {}", senderBalanceBefore, receiverBalanceBefore);

        // ── 2. POST /transfer ─────────────────────────────────────────────────
        log.info("[STEP 2] Initiating transfer of {} from {} to {}", TRANSFER_AMOUNT, SENDER_ACCOUNT, RECEIVER_ACCOUNT);
        String body = """
                {
                  "senderAccountNumber": "%s",
                  "receiverAccountNumber": "%s",
                  "amount": %s
                }
                """.formatted(SENDER_ACCOUNT, RECEIVER_ACCOUNT, TRANSFER_AMOUNT);

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
        log.info("[STEP 2] Transfer response — transactionId: {}, status: {}", transactionId, initialStatus);

        assertThat(transactionId).isNotNull();
        assertThat(initialStatus).isIn("PENDING", "PROCESSING");

        // ── 3. Poll until COMPLETED ───────────────────────────────────────────
        log.info("[STEP 3] Polling for COMPLETED status (max {}ms, interval {}ms)...", MAX_WAIT_MS, POLL_INTERVAL_MS);
        String finalStatus = pollUntilCompleted(transactionId);
        log.info("[STEP 3] Final transaction status: {}", finalStatus);
        assertThat(finalStatus)
                .as("Transaction should reach COMPLETED within %d ms", MAX_WAIT_MS)
                .isEqualTo("COMPLETED");

        // ── 4. Poll until receiver balance reflects the credit ────────────────
        // transaction.completed event still has to be delivered to accounts-service
        // after the transaction status flips to COMPLETED — poll until it lands.
        log.info("[STEP 4] Polling for receiver balance to reflect credit (expected: {})...", receiverBalanceBefore + TRANSFER_AMOUNT);
        double receiverBalanceAfter = pollUntilBalanceEquals(RECEIVER_ACCOUNT, receiverBalanceBefore + TRANSFER_AMOUNT);
        log.info("[STEP 4] Receiver balance after: {} (expected: {})", receiverBalanceAfter, receiverBalanceBefore + TRANSFER_AMOUNT);
        assertThat(receiverBalanceAfter)
                .as("Receiver balance should have increased by %.2f", TRANSFER_AMOUNT)
                .isEqualTo(receiverBalanceBefore + TRANSFER_AMOUNT);

        // ── 5. Assert sender balance decreased by transfer amount ─────────────
        // Sender deduction happens synchronously in transfer() before the saga,
        // so no additional wait needed here.
        double senderBalanceAfter = getBalance(SENDER_ACCOUNT);
        log.info("[STEP 5] Sender balance after: {} (expected: {})", senderBalanceAfter, senderBalanceBefore - TRANSFER_AMOUNT);
        assertThat(senderBalanceAfter)
                .as("Sender balance should have decreased by %.2f", TRANSFER_AMOUNT)
                .isEqualTo(senderBalanceBefore - TRANSFER_AMOUNT);

        log.info("[DONE] Happy path transfer test PASSED for transactionId: {}", transactionId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double pollUntilBalanceEquals(String accountNumber, double expectedBalance) throws InterruptedException {
        long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
        double balance = getBalance(accountNumber);

        while (System.currentTimeMillis() < deadline) {
            if (balance == expectedBalance) {
                break;
            }
            log.info("[BALANCE POLL] account: {} — current: {}, waiting for: {}", accountNumber, balance, expectedBalance);
            Thread.sleep(POLL_INTERVAL_MS);
            balance = getBalance(accountNumber);
        }
        return balance;
    }


    private String pollUntilCompleted(String transactionId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
        String status = "PENDING";

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

            if ("COMPLETED".equals(status) || "FAILED".equals(status) || "FLAGGED".equals(status)) {
                break;
            }
        }
        return status;
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
