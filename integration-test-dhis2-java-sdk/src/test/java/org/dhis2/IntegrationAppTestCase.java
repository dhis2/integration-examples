package org.dhis2;

import org.hisp.dhis.api.model.v2_36_11.DataValueSet;
import org.hisp.dhis.api.model.v2_36_11.DataValue__1;
import org.hisp.dhis.api.model.v2_36_11.OrganisationUnit;
import org.hisp.dhis.api.model.v2_36_11.OrganisationUnitLevel;
import org.hisp.dhis.api.model.v2_36_11.WebMessage;
import org.hisp.dhis.integration.sdk.Dhis2ClientBuilder;
import org.hisp.dhis.integration.sdk.api.Dhis2Client;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.Lists;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private static Dhis2Client sourceDhis2Client;

    private static Dhis2Client targetDhis2Client;

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
            .waitingFor( new HttpWaitStrategy().forStatusCode( 200 ).withStartupTimeout( Duration.ofSeconds( 60 ) ) )
            .withEnv( "WAIT_FOR_DB_CONTAINER", "db" + ":" + 5432 + " -t 0" );
    }

    private static String sourceOrgUnitId;

    private static String targetOrgUnitId;

    @BeforeAll
    public static void beforeAll()
        throws
        IOException
    {
        sourceDhis2ApiUrl = String.format( "http://localhost:%s/api", SOURCE_DHIS2_CONTAINER.getFirstMappedPort() );
        targetDhis2ApiUrl = String.format( "http://localhost:%s/api", TARGET_DHIS2_CONTAINER.getFirstMappedPort() );

        sourceDhis2Client = Dhis2ClientBuilder.newClient( sourceDhis2ApiUrl, DHIS2_API_USERNAME, DHIS2_API_PASSWORD )
            .build();
        targetDhis2Client = Dhis2ClientBuilder.newClient( targetDhis2ApiUrl, DHIS2_API_USERNAME, DHIS2_API_PASSWORD )
            .build();

        sourceOrgUnitId = createOrgUnit( sourceDhis2Client );
        targetOrgUnitId = createOrgUnit( targetDhis2Client );

        createOrgUnitLevel( sourceDhis2Client );
        createOrgUnitLevel( targetDhis2Client );

        addOrgUnitToUser( sourceOrgUnitId, ADMIN_USER_ID, sourceDhis2Client );
        addOrgUnitToUser( targetOrgUnitId, ADMIN_USER_ID, targetDhis2Client );

        importMetaData( sourceDhis2Client );
        importMetaData( targetDhis2Client );

        addOrgUnitToDataSet( sourceOrgUnitId, MALARIA_STOCK_DATA_SET_ID, sourceDhis2Client );
        addOrgUnitToDataSet( targetOrgUnitId, MALARIA_STOCK_DATA_SET_ID, targetDhis2Client );

        createDataValueSets( sourceOrgUnitId, MALARIA_STOCK_DATA_SET_ID, sourceDhis2Client );
    }

    private static void importMetaData( Dhis2Client dhis2Client )
        throws
        IOException
    {
        String metaData = new String(
            Thread.currentThread().getContextClassLoader().getResourceAsStream( "MLAG00_1.2.1_DHIS2.37.json" )
                .readAllBytes(),
            Charset.defaultCharset() );

        dhis2Client.post( "metadata" ).withResource( metaData ).withParameter( "atomicMode", "NONE" ).transfer()
            .close();
    }

    private static void createOrgUnitLevel( Dhis2Client dhis2Client )
        throws
        IOException
    {
        dhis2Client.post( "filledOrganisationUnitLevels" )
            .withResource( new OrganisationUnitLevel().withName( "Level 1" ).withLevel( 1 ) ).transfer().close();
    }

    private static String createOrgUnit( Dhis2Client dhis2Client )
    {
        return (String) ((Map) dhis2Client.post( "organisationUnits" )
            .withResource(
                new OrganisationUnit().withName( "Acme" ).withShortName( "Acme" ).withOpeningDate( new Date() ) )
            .transfer().returnAs(
                WebMessage.class ).getResponse().get()).get( "uid" );
    }

    private static void addOrgUnitToDataSet( String orgUnitId, String dataSetId, Dhis2Client dhis2Client )
        throws
        IOException
    {
        dhis2Client.post( "/dataSets/{dataSetId}/organisationUnits/{organisationUnitId}", dataSetId, orgUnitId )
            .transfer().close();
    }

    private static void addOrgUnitToUser( String orgUnitId, String userId, Dhis2Client dhis2Client )
        throws
        IOException
    {
        dhis2Client.post( "/users/{userId}/organisationUnits/{organisationUnitId}", userId, orgUnitId ).transfer()
            .close();
    }

    private static void createDataValueSets( String orgUnitId, String dataSetId, Dhis2Client dhis2Client )
        throws
        IOException
    {

        List<DataValue__1> dataValues = List.of( new DataValue__1().withDataElement( "CBKXL15dSwQ" )
                .withValue( String.valueOf( ThreadLocalRandom.current().nextInt( 0, Integer.MAX_VALUE ) ) ),
            new DataValue__1().withDataElement( "BdRI37FNDJs" )
                .withValue( String.valueOf( ThreadLocalRandom.current().nextInt( 0, Integer.MAX_VALUE ) ) ),
            new DataValue__1().withDataElement( "RRA1O37nLn0" )
                .withValue( String.valueOf( ThreadLocalRandom.current().nextInt( 0, Integer.MAX_VALUE ) ) ),
            new DataValue__1().withDataElement( "CPBuuIiDnn8" )
                .withValue( String.valueOf( ThreadLocalRandom.current().nextInt( 0, Integer.MAX_VALUE ) ) ),
            new DataValue__1().withDataElement( "HOEMlLX5SMC" )
                .withValue( String.valueOf( ThreadLocalRandom.current().nextInt( 0, Integer.MAX_VALUE ) ) ),
            new DataValue__1().withDataElement( "f7z0IhHVWBT" )
                .withValue( String.valueOf( ThreadLocalRandom.current().nextInt( 0, Integer.MAX_VALUE ) ) ) );

        DataValueSet dataValueSet = new DataValueSet().withDataSet( dataSetId ).withCompleteDate( "2022-02-03" )
            .withPeriod( "202201" ).withOrgUnit( orgUnitId ).withDataValues( dataValues );

        dhis2Client.post( "dataValueSets" ).withResource( dataValueSet ).transfer().close();
    }

    @Test
    public void test()
        throws
        IOException
    {
        IntegrationApp.main(
            new String[] { sourceDhis2ApiUrl,
                DHIS2_API_USERNAME,
                DHIS2_API_PASSWORD, sourceOrgUnitId,
                targetDhis2ApiUrl,
                DHIS2_API_USERNAME,
                DHIS2_API_PASSWORD, targetOrgUnitId, MALARIA_STOCK_DATA_SET_ID, "202201" } );

        Iterable<DataValueSet> dataValueSets = targetDhis2Client.get(
                "/dataValueSets" ).withoutPaging().withParameter( "dataSet", MALARIA_STOCK_DATA_SET_ID )
            .withParameter( "period", "202201" ).withParameter( "orgUnit", targetOrgUnitId ).transfer()
            .returnAs( DataValueSet.class, "dataValues" );
        assertEquals( 6, Lists.newArrayList( dataValueSets ).size() );
    }
}
