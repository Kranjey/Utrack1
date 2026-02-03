package tests.config;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.testng.annotations.BeforeSuite;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class YouTrackConfig {
    
    public static String BASE_URL;
    public static String ADMIN_USERNAME;
    public static String ADMIN_PASSWORD;
    public static String DEFAULT_PROJECT_ID;
    public static String AUTH_TOKEN;
    
    @BeforeSuite(alwaysRun = true)
    public void setUp() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream("src/test/resources/config.properties"));
        
        BASE_URL = properties.getProperty("youtrack.base.url", "http://localhost:8080");
        ADMIN_USERNAME = properties.getProperty("youtrack.admin.username", "admin");
        ADMIN_PASSWORD = properties.getProperty("youtrack.admin.password", "admin123");
        DEFAULT_PROJECT_ID = properties.getProperty("youtrack.default.project.id", "TEST");
        
        // Настройка RestAssured
        RequestSpecBuilder builder = new RequestSpecBuilder();
        builder.setBaseUri(BASE_URL);
        builder.setContentType(ContentType.JSON);
        builder.addHeader("Accept", "application/json");
        builder.addFilter(new RequestLoggingFilter());
        builder.addFilter(new ResponseLoggingFilter());
        
        RestAssured.requestSpecification = builder.build();
        
        // Получение токена аутентификации
        AUTH_TOKEN = getAuthToken();
        
        System.out.println("=== YouTrack Test Configuration ===");
        System.out.println("Base URL: " + BASE_URL);
        System.out.println("Project: " + DEFAULT_PROJECT_ID);
        System.out.println("================================");
    }
    
    private String getAuthToken() {
        try {
            return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(new tests.dto.AuthRequest(ADMIN_USERNAME, ADMIN_PASSWORD))
                .when()
                .post("/api/users/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");
        } catch (Exception e) {
            System.err.println("Failed to get auth token: " + e.getMessage());
            return null;
        }
    }
}