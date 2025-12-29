package com.apporte.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class NotificationResourceTest {
    @Test
    public void testNotificationEndpoint() {
        given()
          .when().get("/api/notifications")
          .then()
             .statusCode(200)
             .body("message", equalTo("Notification Engine is running!"));
    }
}