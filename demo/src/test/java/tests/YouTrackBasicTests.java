package tests;

import tests.config.YouTrackConfig;
import tests.dto.IssueRequest;
import io.restassured.http.ContentType;
import org.testng.Assert;
import org.testng.annotations.*;
import tests.utils.CSVReaderUtil;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class YouTrackBasicTests extends YouTrackConfig {
    
    private String createdIssueId;
    
    @BeforeMethod
    public void printTestStart(org.testng.ITestResult result) {
        System.out.println("\n=== STARTING TEST: " + result.getMethod().getMethodName() + " ===");
    }
    
    @AfterMethod
    public void printTestResult(org.testng.ITestResult result) {
        String status = result.isSuccess() ? "PASSED" : "FAILED";
        System.out.println("=== TEST " + status + ": " + result.getMethod().getMethodName() + " ===");
        
        if (!result.isSuccess() && result.getThrowable() != null) {
            System.err.println("ERROR: " + result.getThrowable().getMessage());
        }
    }
    
    @AfterClass
    public void cleanup() {
        if (createdIssueId != null) {
            System.out.println("Cleaning up test issue: " + createdIssueId);
            given()
                .header("Authorization", "Bearer " + AUTH_TOKEN)
                .when()
                .delete("/api/issues/" + createdIssueId)
                .then()
                .statusCode(200);
        }
    }
    
    // ==================== ПОЗИТИВНЫЕ ТЕСТЫ ====================
    
    @Test(priority = 1, description = "Позитивный тест: Проверка доступности YouTrack сервера")
    public void testYouTrackServerIsAccessible() {
        System.out.println("Тестирование доступности YouTrack сервера...");
        
        given()
            .when()
            .get("/api/admin/telemetry/status")
            .then()
            .statusCode(200)
            .body("healthy", equalTo(true));
        
        System.out.println("✓ YouTrack сервер доступен и работает корректно");
    }
    
    @Test(priority = 2, description = "Позитивный тест: Создание и получение задачи")
    public void testCreateAndGetIssue() {
        System.out.println("Тестирование создания и получения задачи...");
        
        // Создание задачи
        IssueRequest issueRequest = new IssueRequest(
            DEFAULT_PROJECT_ID,
            "Test Issue: API Automation",
            "This issue was created by automated tests"
        );
        
        String issueId = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType(ContentType.JSON)
            .body(issueRequest)
            .when()
            .post("/api/issues")
            .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("idReadable", containsString(DEFAULT_PROJECT_ID + "-"))
            .body("summary", equalTo(issueRequest.getSummary()))
            .extract()
            .path("id");
        
        createdIssueId = issueId;
        System.out.println("✓ Задача создана: " + issueId);
        
        // Получение созданной задачи
        given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .when()
            .get("/api/issues/" + issueId)
            .then()
            .statusCode(200)
            .body("id", equalTo(issueId))
            .body("projectId", equalTo(DEFAULT_PROJECT_ID))
            .body("summary", equalTo(issueRequest.getSummary()));
        
        System.out.println("✓ Задача успешно получена по ID");
    }
    
    // ==================== НЕГАТИВНЫЕ ТЕСТЫ ====================
    
    @Test(priority = 3, description = "Негативный тест: Создание задачи без авторизации")
    public void testCreateIssueWithoutAuthorization() {
        System.out.println("Тестирование создания задачи без авторизации...");
        
        IssueRequest issueRequest = new IssueRequest(
            DEFAULT_PROJECT_ID,
            "Unauthorized Test Issue",
            "This should fail without authorization"
        );
        
        given()
            .header("Authorization", "") // Убираем токен авторизации
            .contentType(ContentType.JSON)
            .body(issueRequest)
            .when()
            .post("/api/issues")
            .then()
            .statusCode(401)
            .body("error", containsString("Unauthorized"));
        
        System.out.println("✓ Создание задачи без авторизации отклонено с кодом 401");
    }
    
    @Test(priority = 4, description = "Негативный тест: Создание задачи без обязательного поля summary")
    public void testCreateIssueWithoutRequiredFields() {
        System.out.println("Тестирование создания задачи без обязательных полей...");
        
        // Создаем запрос без summary (обязательное поле)
        IssueRequest issueRequest = IssueRequest.builder()
            .projectId(DEFAULT_PROJECT_ID)
            .description("Task without summary - should fail")
            .build();
        
        given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType(ContentType.JSON)
            .body(issueRequest)
            .when()
            .post("/api/issues")
            .then()
            .statusCode(400)
            .body("error", containsString("summary"));
        
        System.out.println("✓ Создание задачи без summary отклонено с кодом 400");
    }
    
    // ==================== DDT ТЕСТ ====================
    
    @Test(priority = 5, 
          dataProvider = "issueTestData", 
          dataProviderClass = CSVReaderUtil.class,
          description = "DDT тест: Создание задач с различными данными из CSV")
    public void testCreateIssueWithDifferentData(String projectId, String summary, 
                                                 String description, boolean expectedSuccess) {
        System.out.println(String.format(
            "DDT тест: Project='%s', Summary='%s', ExpectedSuccess=%b", 
            projectId, summary, expectedSuccess
        ));
        
        IssueRequest issueRequest = new IssueRequest(projectId, summary, description);
        
        var response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType(ContentType.JSON)
            .body(issueRequest)
            .when()
            .post("/api/issues");
        
        if (expectedSuccess) {
            // Ожидаем успех - задача должна создаться
            response.then()
                .statusCode(200)
                .body("idReadable", containsString(projectId + "-"))
                .body("summary", equalTo(summary));
            
            String issueId = response.extract().path("id");
            System.out.println("✓ Задача успешно создана: " + issueId);
            
            // Очищаем созданную задачу
            given()
                .header("Authorization", "Bearer " + AUTH_TOKEN)
                .when()
                .delete("/api/issues/" + issueId)
                .then()
                .statusCode(200);
            
        } else {
            // Ожидаем ошибку - задача не должна создаться
            int statusCode = response.extract().statusCode();
            Assert.assertTrue(statusCode >= 400 && statusCode < 500, 
                "Для некорректных данных ожидается ошибка 4xx");
            
            System.out.println("✓ Создание задачи отклонено как и ожидалось (код " + statusCode + ")");
        }
    }
}