package expense_tracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import expense_tracker.repository.ExpenseRepository;
import expense_tracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExpenseControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        expenseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRequireTokenForProtectedExpenseEndpoint() throws Exception {
        HttpResponse<String> response = sendJsonRequest(
                "GET",
                "/expenses",
                null,
                null
        );

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("Unauthorized", body.get("message").asText());
    }

    @Test
    void shouldCreateAndFetchExpensesWithValidJwt() throws Exception {
        TestAuthContext auth = registerAndLogin();

        HttpResponse<String> createExpenseResponse = createExpense(
                auth.token(),
                new BigDecimal("25000"),
                "SHOPPING",
                "integration test expense",
                LocalDate.now()
        );

        assertEquals(HttpStatus.OK.value(), createExpenseResponse.statusCode());
        JsonNode createdExpense = objectMapper.readTree(createExpenseResponse.body());
        assertEquals("SHOPPING", createdExpense.get("category").asText());
        assertEquals("integration test expense", createdExpense.get("description").asText());
        assertEquals(new BigDecimal("25000"), createdExpense.get("amount").decimalValue());

        HttpResponse<String> fetchExpensesResponse = sendJsonRequest(
                "GET",
                "/expenses",
                null,
                auth.token()
        );

        assertEquals(HttpStatus.OK.value(), fetchExpensesResponse.statusCode());
        JsonNode fetchBody = objectMapper.readTree(fetchExpensesResponse.body());
        assertEquals(1, fetchBody.get("page").asInt());
        assertEquals(1, fetchBody.get("totalElements").asInt());
        assertTrue(fetchBody.get("data").isArray());
        assertEquals(1, fetchBody.get("data").size());
        assertEquals("SHOPPING", fetchBody.get("data").get(0).get("category").asText());
    }

    @Test
    void shouldUpdateExpenseWithValidJwt() throws Exception {
        TestAuthContext auth = registerAndLogin();
        HttpResponse<String> createExpenseResponse = createExpense(
                auth.token(),
                new BigDecimal("25000"),
                "SHOPPING",
                "before update",
                LocalDate.now().minusDays(1)
        );

        JsonNode createdExpense = objectMapper.readTree(createExpenseResponse.body());
        String expenseId = createdExpense.get("id").asText();

        HttpResponse<String> updateResponse = sendJsonRequest(
                "PATCH",
                "/expenses/" + expenseId,
                """
                {
                  "amount": 30000,
                  "category": "FOOD",
                  "description": "after update",
                  "expenseDate": "%s"
                }
                """.formatted(LocalDate.now()),
                auth.token()
        );

        assertEquals(HttpStatus.OK.value(), updateResponse.statusCode());
        JsonNode updatedExpense = objectMapper.readTree(updateResponse.body());
        assertEquals(new BigDecimal("30000"), updatedExpense.get("amount").decimalValue());
        assertEquals("FOOD", updatedExpense.get("category").asText());
        assertEquals("after update", updatedExpense.get("description").asText());
        assertEquals(LocalDate.now().toString(), updatedExpense.get("expenseDate").asText());
    }

    @Test
    void shouldDeleteExpenseWithValidJwt() throws Exception {
        TestAuthContext auth = registerAndLogin();
        HttpResponse<String> createExpenseResponse = createExpense(
                auth.token(),
                new BigDecimal("18000"),
                "UTILITIES",
                "to be deleted",
                LocalDate.now().minusDays(1)
        );

        JsonNode createdExpense = objectMapper.readTree(createExpenseResponse.body());
        String expenseId = createdExpense.get("id").asText();

        HttpResponse<String> deleteResponse = sendJsonRequest(
                "DELETE",
                "/expenses/" + expenseId,
                null,
                auth.token()
        );

        assertEquals(HttpStatus.NO_CONTENT.value(), deleteResponse.statusCode());

        HttpResponse<String> fetchExpensesResponse = sendJsonRequest(
                "GET",
                "/expenses",
                null,
                auth.token()
        );

        assertEquals(HttpStatus.OK.value(), fetchExpensesResponse.statusCode());
        JsonNode fetchBody = objectMapper.readTree(fetchExpensesResponse.body());
        assertEquals(0, fetchBody.get("totalElements").asInt());
        assertEquals(0, fetchBody.get("data").size());
    }

    @Test
    void shouldRejectInvalidJwtToken() throws Exception {
        HttpResponse<String> response = sendJsonRequest(
                "GET",
                "/expenses",
                null,
                "this-is-not-a-valid-jwt"
        );

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("Invalid token", body.get("message").asText());
    }

    @Test
    void shouldRejectInvalidExpensePayload() throws Exception {
        TestAuthContext auth = registerAndLogin();

        HttpResponse<String> response = sendJsonRequest(
                "POST",
                "/expenses",
                """
                {
                  "amount": -1000,
                  "category": "SHOPPING",
                  "description": "invalid expense",
                  "expenseDate": "%s"
                }
                """.formatted(LocalDate.now()),
                auth.token()
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertTrue(body.get("message").asText().contains("Amount must be greater than zero"));
        assertEquals("/expenses", body.get("path").asText());
    }

    @Test
    void shouldRejectUpdatingExpenseOwnedByAnotherUser() throws Exception {
        TestAuthContext owner = registerAndLogin();
        HttpResponse<String> createExpenseResponse = createExpense(
                owner.token(),
                new BigDecimal("22000"),
                "SHOPPING",
                "owner expense",
                LocalDate.now().minusDays(1)
        );

        String expenseId = objectMapper.readTree(createExpenseResponse.body()).get("id").asText();
        TestAuthContext attacker = registerAndLogin();

        HttpResponse<String> updateResponse = sendJsonRequest(
                "PATCH",
                "/expenses/" + expenseId,
                """
                {
                  "amount": 99999,
                  "category": "FOOD",
                  "description": "hacked",
                  "expenseDate": "%s"
                }
                """.formatted(LocalDate.now()),
                attacker.token()
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), updateResponse.statusCode());
        JsonNode body = objectMapper.readTree(updateResponse.body());
        assertEquals("Expense not found for user: " + attacker.username(), body.get("message").asText());
        assertEquals("/expenses/" + expenseId, body.get("path").asText());
    }

    @Test
    void shouldRejectDeletingExpenseOwnedByAnotherUser() throws Exception {
        TestAuthContext owner = registerAndLogin();
        HttpResponse<String> createExpenseResponse = createExpense(
                owner.token(),
                new BigDecimal("17000"),
                "UTILITIES",
                "owner expense",
                LocalDate.now().minusDays(1)
        );

        String expenseId = objectMapper.readTree(createExpenseResponse.body()).get("id").asText();
        TestAuthContext attacker = registerAndLogin();

        HttpResponse<String> deleteResponse = sendJsonRequest(
                "DELETE",
                "/expenses/" + expenseId,
                null,
                attacker.token()
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), deleteResponse.statusCode());
        JsonNode body = objectMapper.readTree(deleteResponse.body());
        assertEquals("Expense not found for user: " + attacker.username(), body.get("message").asText());
        assertEquals("/expenses/" + expenseId, body.get("path").asText());

        HttpResponse<String> ownerFetchResponse = sendJsonRequest(
                "GET",
                "/expenses",
                null,
                owner.token()
        );

        JsonNode fetchBody = objectMapper.readTree(ownerFetchResponse.body());
        assertEquals(1, fetchBody.get("totalElements").asInt());
    }

    private TestAuthContext registerAndLogin() throws Exception {
        String username = "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = username + "@example.com";
        String password = "password123";

        HttpResponse<String> signUpResponse = sendJsonRequest(
                "POST",
                "/auth/signup",
                """
                {
                  "username": "%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(username, email, password),
                null
        );

        assertEquals(HttpStatus.OK.value(), signUpResponse.statusCode());

        HttpResponse<String> loginResponse = sendJsonRequest(
                "POST",
                "/auth/login",
                """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password),
                null
        );

        assertEquals(HttpStatus.OK.value(), loginResponse.statusCode());
        JsonNode loginBody = objectMapper.readTree(loginResponse.body());
        String token = loginBody.get("token").asText();
        assertNotNull(token);
        assertFalse(token.isBlank());

        return new TestAuthContext(username, password, token);
    }

    private HttpResponse<String> createExpense(
            String token,
            BigDecimal amount,
            String category,
            String description,
            LocalDate expenseDate
    ) throws Exception {
        return sendJsonRequest(
                "POST",
                "/expenses",
                """
                {
                  "amount": %s,
                  "category": "%s",
                  "description": "%s",
                  "expenseDate": "%s"
                }
                """.formatted(amount.toPlainString(), category, description, expenseDate),
                token
        );
    }

    private HttpResponse<String> sendJsonRequest(
            String method,
            String path,
            String body,
            String bearerToken
    ) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }

        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private record TestAuthContext(String username, String password, String token) {
    }
}
