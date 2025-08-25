package com.example.store.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import io.restassured.specification.RequestSpecification;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@ActiveProfiles("integration")
@Import({GlobalDataSetup.class})
@EnableAutoConfiguration(exclude = {LiquibaseAutoConfiguration.class})
public abstract class AbstractIntegrationTestBase {

    private static final String[] DELETE_FROM_TABLES = {"order", "customer"};
    private static final String[] SCHEMA_DATA = {"/db/changelog/schema.sql", "/db/changelog/product-schema.sql"};

    @Autowired
    protected PlatformTransactionManager platformTransactionManager;

    @Autowired
    @SuppressWarnings("unused")
    private GlobalDataSetup globalDataSetup;

    @LocalServerPort
    protected int port;

    private static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16.2")
            .withDatabaseName("store")
            .withPassword("admin")
            .withUsername("admin");

    @BeforeAll
    protected static void setUpGlobal() throws Exception {
        Path tempFile = Files.createTempFile("schema", "sql");
        Arrays.stream(SCHEMA_DATA).forEach(r -> {
            InputStream inputStream = AbstractIntegrationTestBase.class.getResourceAsStream(r);
            try {
                Files.write(tempFile, inputStream.readAllBytes(), StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        postgreSQLContainer.withCopyFileToContainer(
                MountableFile.forHostPath(tempFile), "/docker-entrypoint-initdb.d/init.sql");
        if (!postgreSQLContainer.isRunning()) {
            postgreSQLContainer.start();
        }
    }

    @AfterAll
    protected static void tearDownGlobal() throws Exception {

        if (postgreSQLContainer.isRunning()) {
            Class.forName(postgreSQLContainer.getDriverClassName());
            String url = postgreSQLContainer.getJdbcUrl() + "/" + postgreSQLContainer.getDatabaseName()
                    + "?currentSchema=public";
            try (Connection connection = DriverManager.getConnection(
                            url, postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword());
                    Statement statement = connection.createStatement()) {
                Arrays.stream(DELETE_FROM_TABLES).forEach(tableName -> {
                    try {
                        statement.execute("DELETE FROM \"" + tableName + "\"");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    @BeforeEach
    void configureRestAssured() {
        RestAssured.reset();
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.defaultParser = Parser.JSON;
        runInTransaction(() -> globalDataSetup.createCustomers());
    }

    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.username", () -> postgreSQLContainer.getUsername());
        registry.add("spring.datasource.password", () -> postgreSQLContainer.getPassword());
        registry.add(
                "spring.datasource.url",
                () -> postgreSQLContainer.getJdbcUrl() + "/" + postgreSQLContainer.getDatabaseName()
                        + "?currentSchema=public");
    }

    protected RequestSpecification given() {
        return RestAssured.given()
                .relaxedHTTPSValidation()
                .baseUri("http://localhost")
                .port(port)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    }

    protected void runInTransaction(InTransaction inTransaction) {
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        transactionDefinition.setIsolationLevel(ISOLATION_REPEATABLE_READ);
        transactionDefinition.setTimeout(60);
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);

        try {
            inTransaction.execute();
            platformTransactionManager.commit(transactionStatus);
        } catch (Exception e) {
            try {
                platformTransactionManager.rollback(transactionStatus);
            } catch (Exception innerEx) {
                fail(innerEx.getMessage());
            }
        }
    }

    protected <T> T runInTransaction(InTransactionSupplier<T> inTransaction) {
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        transactionDefinition.setIsolationLevel(ISOLATION_REPEATABLE_READ);
        transactionDefinition.setTimeout(60);
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);

        try {
            T result = inTransaction.execute();
            platformTransactionManager.commit(transactionStatus);
            return result;
        } catch (Exception e) {
            try {
                platformTransactionManager.rollback(transactionStatus);
            } catch (Exception inner) {
                fail(e.getMessage() + " -> " + inner.getMessage());
            }
            fail(e.getMessage());
        }
        throw new RuntimeException("Should not get here");
    }
}
