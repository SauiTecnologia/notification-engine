package com.apporte.api;

import com.apporte.test.TestDataHelper;
import com.apporte.test.TestDataHelper.TestData;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestSecurity(user = "admin", roles = {"admin", "notification-admin"}) 
public class AdminNotificationControllerTest {

    private Long sentNotificationId;
    private Long errorNotificationId;
    private Long pendingNotificationId;

    @BeforeEach
    @Transactional
    public void setupTestData() {
        // Usando o TestDataHelper específico para Admin Controller
        TestData testData = TestDataHelper.setupAdminControllerTestData();
        
        sentNotificationId = testData.sentNotification.id;
        errorNotificationId = testData.errorNotification.id;
        pendingNotificationId = testData.pendingNotification.id;
    }

    @Test
    @Transactional
    public void testGetNotification_Success() {
        given()
            .when()
                .get("/api/admin/notifications/" + sentNotificationId)
            .then()
                .statusCode(200)
                .body("id", equalTo(sentNotificationId.intValue()))
                .body("eventType", equalTo("PROJECT_READY_REVIEW"))
                .body("status", equalTo("sent"));
    }

    @Test
    @Transactional
    public void testGetNotification_NotFound() {
        given()
            .when()
                .get("/api/admin/notifications/99999")
            .then()
                .statusCode(404)
                .body("error", containsString("Notification not found"));
    }

    @Test
    @Transactional
    public void testGetPendingNotification() {
        given()
            .when()
                .get("/api/admin/notifications/" + pendingNotificationId)
            .then()
                .statusCode(200)
                .body("id", equalTo(pendingNotificationId.intValue()))
                .body("status", equalTo("pending"))
                .body("channel", equalTo("whatsapp"));
    }

    @Test
    @Transactional
    public void testDeleteNotification_Success() {
        given()
            .when()
                .delete("/api/admin/notifications/" + sentNotificationId)
            .then()
                .statusCode(200)
                .body("message", containsString("Notification deleted"));
    }

    @Test
    @Transactional
    public void testDeletePendingNotification() {
        given()
            .when()
                .delete("/api/admin/notifications/" + pendingNotificationId)
            .then()
                .statusCode(200)
                .body("message", containsString("Notification deleted"));
    }

    @Test
    @Transactional
    public void testDeleteNotification_NotFound() {
        given()
            .when()
                .delete("/api/admin/notifications/99999")
            .then()
                .statusCode(404)
                .body("error", containsString("Notification not found"));
    }

    @Test
    @Transactional
    public void testRetryNotification_Success() {
        given()
            .contentType(ContentType.JSON)
            .when()
                .post("/api/admin/notifications/" + errorNotificationId + "/retry")
            .then()
                .statusCode(200)
                .body("message", containsString("retry"))
                .body("newStatus", equalTo("retrying"));
    }

    @Test
    @Transactional
    public void testRetryNotification_NotFound() {
        given()
            .contentType(ContentType.JSON)
            .when()
                .post("/api/admin/notifications/99999/retry")
            .then()
                .statusCode(404)
                .body("error", equalTo("Notification not found"));
    }

    @Test
    @Transactional
    public void testRetryNotification_NotInErrorState() {
        given()
            .contentType(ContentType.JSON)
            .when()
                .post("/api/admin/notifications/" + sentNotificationId + "/retry")
            .then()
                .statusCode(400)
                .body("error", equalTo("Notification is not in error state"));
    }

    @Test
    @Transactional
    public void testRetryPendingNotification_NotInErrorState() {
        given()
            .contentType(ContentType.JSON)
            .when()
                .post("/api/admin/notifications/" + pendingNotificationId + "/retry")
            .then()
                .statusCode(400)
                .body("error", equalTo("Notification is not in error state"));
    }

    @Test
    @Transactional
    public void testListNotifications_Success() {
        given()
            .when()
                .get("/api/admin/notifications")
            .then()
                .statusCode(200)
                .body("notifications.size()", equalTo(3))
                .body("total", equalTo(3));
    }

    @Test
    @Transactional
    public void testListNotifications_WithFilters() {
        given()
            .queryParam("status", "error")
            .queryParam("channel", "email")
            .queryParam("page", "0")
            .queryParam("size", "20")
            .when()
                .get("/api/admin/notifications")
            .then()
                .statusCode(200)
                .body("notifications.size()", equalTo(1))
                .body("notifications[0].status", equalTo("error"))
                .body("notifications[0].channel", equalTo("email"));
    }

    @Test
    @Transactional
    public void testListNotifications_FilterPending() {
        given()
            .queryParam("status", "pending")
            .queryParam("channel", "whatsapp")
            .when()
                .get("/api/admin/notifications")
            .then()
                .statusCode(200)
                .body("notifications.size()", equalTo(1))
                .body("notifications[0].status", equalTo("pending"))
                .body("notifications[0].channel", equalTo("whatsapp"));
    }

    @Test
    @Transactional
    public void testListNotifications_FilterSent() {
        given()
            .queryParam("status", "sent")
            .queryParam("channel", "email")
            .when()
                .get("/api/admin/notifications")
            .then()
                .statusCode(200)
                .body("notifications.size()", equalTo(1))
                .body("notifications[0].status", equalTo("sent"))
                .body("notifications[0].channel", equalTo("email"));
    }

    @Test
    @Transactional
    public void testHealthCheck() {
        given()
            .when()
                .get("/api/admin/notifications/health")
            .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("totalNotifications", equalTo(3))
                .body("pendingNotifications", equalTo(1))
                .body("errorNotifications", equalTo(1))
                .body("sentNotifications", equalTo(1))
                .body("database", equalTo("connected"));
    }

    @Test
    @Transactional
    public void testGetStatistics() {
        given()
            .queryParam("days", "7")
            .when()
                .get("/api/admin/notifications/stats")
            .then()
                .statusCode(200)
                .body("periodDays", equalTo(7))
                .body("total", greaterThanOrEqualTo(3)) // Corrigido: usa greaterThanOrEqualTo do Hamcrest
                .body("sent", greaterThanOrEqualTo(1))
                .body("error", greaterThanOrEqualTo(1))
                .body("pending", greaterThanOrEqualTo(1));
    }
}