package org.dhis2;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Testcontainers
public class IntegrationAppTestCase
{
    @Container
    public static final PostgreSQLContainer<?> SOURCE_POSTGRESQL_CONTAINER = newPostgreSqlContainer();

    @Container
    public static final GenericContainer<?> SOURCE_DHIS2_CONTAINER = newDhis2Container( SOURCE_POSTGRESQL_CONTAINER );

    @Container
    public static final PostgreSQLContainer<?> TARGET_POSTGRESQL_CONTAINER = newPostgreSqlContainer();

    @Container
    public static final GenericContainer<?> TARGET_DHIS2_CONTAINER = newDhis2Container( TARGET_POSTGRESQL_CONTAINER );

    private static final String DHIS2_API_USERNAME = "admin";

    private static final String DHIS2_API_PASSWORD = "district";

    private static final String MALARIA_STOCK_DATA_SET_ID = "PRiItBuYx0e";

    private static final String ADMIN_USER_ID = "M5zQapPyTZI";

    private static String sourceDhis2ApiUrl;

    private static String targetDhis2ApiUrl;

    private static PostgreSQLContainer<?> newPostgreSqlContainer()
    {
        return new PostgreSQLContainer<>( DockerImageName.parse( "postgis/postgis:12-3.2-alpine" )
            .asCompatibleSubstituteFor( "postgres" ) )
            .withDatabaseName( "dhis2" )
            .withNetworkAliases( "db" )
            .withUsername( "dhis" )
            .withPassword( "dhis" ).withNetwork( Network.newNetwork() );
    }

    private static GenericContainer<?> newDhis2Container( PostgreSQLContainer<?> postgreSqlContainer )
    {
        return new GenericContainer<>( DockerImageName.parse( "dhis2/core:2.36.7" ) )
            .dependsOn( postgreSqlContainer )
            .withClasspathResourceMapping( "dhis.conf", "/DHIS2_home/dhis.conf", BindMode.READ_WRITE )
            .withNetwork( postgreSqlContainer.getNetwork() ).withExposedPorts( 8080 )
            .waitingFor( new HttpWaitStrategy().forStatusCode( 200 ).withStartupTimeout( Duration.ofSeconds( 120 ) ) )
            .withEnv( "WAIT_FOR_DB_CONTAINER", "db" + ":" + 5432 + " -t 0" );
    }

    private static RequestSpecification sourceRequestSpec;

    private static RequestSpecification targetRequestSpec;

    private static String sourceOrgUnitId;

    private static String targetOrgUnitId;

    @BeforeAll
    public static void beforeAll()
        throws IOException
    {
        sourceDhis2ApiUrl = String.format( "http://localhost:%s/api", SOURCE_DHIS2_CONTAINER.getFirstMappedPort() );
        targetDhis2ApiUrl = String.format( "http://localhost:%s/api", TARGET_DHIS2_CONTAINER.getFirstMappedPort() );

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        sourceRequestSpec = new RequestSpecBuilder().setBaseUri( sourceDhis2ApiUrl ).build()
            .contentType( ContentType.JSON ).auth().preemptive().basic( DHIS2_API_USERNAME, DHIS2_API_PASSWORD );

        targetRequestSpec = new RequestSpecBuilder().setBaseUri( targetDhis2ApiUrl ).build()
            .contentType( ContentType.JSON ).auth().preemptive().basic( DHIS2_API_USERNAME, DHIS2_API_PASSWORD );

        sourceOrgUnitId = createOrgUnit( sourceRequestSpec );
        targetOrgUnitId = createOrgUnit( targetRequestSpec );

        createOrgUnitLevel( sourceRequestSpec );
        createOrgUnitLevel( targetRequestSpec );

        addOrgUnitToUser( sourceOrgUnitId, ADMIN_USER_ID, sourceRequestSpec );
        addOrgUnitToUser( targetOrgUnitId, ADMIN_USER_ID, targetRequestSpec );

        importMetaData( sourceRequestSpec );
        importMetaData( targetRequestSpec );

        addOrgUnitToDataSet( sourceOrgUnitId, MALARIA_STOCK_DATA_SET_ID, sourceRequestSpec );
        addOrgUnitToDataSet( targetOrgUnitId, MALARIA_STOCK_DATA_SET_ID, targetRequestSpec );

        createDataValueSets( sourceOrgUnitId, MALARIA_STOCK_DATA_SET_ID, sourceRequestSpec );
    }

    private static void importMetaData( RequestSpecification requestSpec )
        throws IOException
    {
        String metaData = new String(
            Thread.currentThread().getContextClassLoader().getResourceAsStream( "MLAG00_1.2.1_DHIS2.37.json" )
                .readAllBytes(),
            Charset.defaultCharset() );
        given( requestSpec ).queryParam( "atomicMode", "NONE" ).body( metaData ).when().post( "/metadata" ).then()
            .statusCode( 200 );
    }

    private static void createOrgUnitLevel( RequestSpecification requestSpec )
    {
        Map<String, List<Map<String, ? extends Serializable>>> orgUnitLevels = Map.of(
            "organisationUnitLevels", List.of( Map.of( "name", "Level 1", "level", 1 ) ) );
        given( requestSpec ).body( orgUnitLevels ).when().post( "/filledOrganisationUnitLevels" ).then()
            .statusCode( 201 );
    }

    private static String createOrgUnit( RequestSpecification requestSpec )
    {
        Map<String, ? extends Serializable> orgUnit = Map.of( "name", "Acme",
            "shortName", "Acme",
            "openingDate", new Date().getTime() );

        return given( requestSpec ).body( orgUnit )
            .when().post( "/organisationUnits" )
            .then().statusCode( 201 )
            .extract().path( "response.uid" );
    }

    private static void addOrgUnitToDataSet( String orgUnitId, String dataSetId, RequestSpecification requestSpec )
    {
        given( requestSpec ).when()
            .post( "/dataSets/{dataSetId}/organisationUnits/{organisationUnitId}", dataSetId, orgUnitId )
            .then()
            .statusCode( 204 );
    }

    private static void addOrgUnitToUser( String orgUnitId, String userId, RequestSpecification requestSpec )
    {
        given( requestSpec ).when()
            .post( "/users/{userId}/organisationUnits/{organisationUnitId}", userId, orgUnitId )
            .then()
            .statusCode( 204 );
    }

    private static void createDataValueSets( String orgUnitId, String dataSetId, RequestSpecification requestSpec )
    {
        List<Map<String, String>> dataValues = List.of( Map.of( "dataElement", "CBKXL15dSwQ", "value",
                String.valueOf( ThreadLocalRandom.current().nextInt( 0, Integer.MAX_VALUE ) ) ),
            Map.of( "dataElement", "BdRI37FNDJs", "value",
                String.valueOf( ThreadLocalRandom.current().nextInt( 0, Integer.MAX_VALUE ) ) ),
            Map.of( "dataElement", "RRA1O37nLn0", "value",
                String.valueOf( ThreadLocalRandom.current().nextInt( 0, Integer.MAX_VALUE ) ) ),
            Map.of( "dataElement", "CPBuuIiDnn8", "value",
                String.valueOf( ThreadLocalRandom.current().nextInt( 0, Integer.MAX_VALUE ) ) ),
            Map.of( "dataElement", "HOEMlLX5SMC", "value",
                String.valueOf( ThreadLocalRandom.current().nextInt( 0, Integer.MAX_VALUE ) ) ),
            Map.of( "dataElement", "f7z0IhHVWBT", "value",
                String.valueOf( ThreadLocalRandom.current().nextInt( 0, Integer.MAX_VALUE ) ) ) );

        Map<String, Object> dataValueSet = Map.of( "dataSet", dataSetId,
            "completeDate", "2022-02-03",
            "period", "202201",
            "orgUnit", orgUnitId,
            "dataValues", dataValues );

        given( requestSpec ).body( dataValueSet ).
            when().post( "/dataValueSets" ).
            then().statusCode( 200 );
    }

    @Test
    public void test()
    {
        IntegrationApp.main(
            new String[] { sourceDhis2ApiUrl,
                DHIS2_API_USERNAME,
                DHIS2_API_PASSWORD, sourceOrgUnitId,
                targetDhis2ApiUrl,
                DHIS2_API_USERNAME,
                DHIS2_API_PASSWORD, targetOrgUnitId, MALARIA_STOCK_DATA_SET_ID, "202201" } );

        given( targetRequestSpec ).get(
                "/dataValueSets?dataSet={dataSetId}&period={period}&orgUnit={orgUnitId}", MALARIA_STOCK_DATA_SET_ID,
                "202201",
                targetOrgUnitId ).
            then().statusCode( 200 ).body( "dataValues.size()", equalTo( 6 ) );
    }
}
