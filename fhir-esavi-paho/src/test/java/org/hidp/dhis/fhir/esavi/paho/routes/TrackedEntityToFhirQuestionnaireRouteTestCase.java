package org.hidp.dhis.fhir.esavi.paho.routes;

import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.hisp.dhis.api.model.v2_36_11.Attribute__1;
import org.hisp.dhis.api.model.v2_36_11.DescriptiveWebMessage;
import org.hisp.dhis.api.model.v2_36_11.Enrollment;
import org.hisp.dhis.api.model.v2_36_11.Event;
import org.hisp.dhis.api.model.v2_36_11.EventChart;
import org.hisp.dhis.api.model.v2_36_11.OrganisationUnit;
import org.hisp.dhis.api.model.v2_36_11.OrganisationUnitLevel;
import org.hisp.dhis.api.model.v2_36_11.TrackedEntityInstance;
import org.hisp.dhis.api.model.v2_36_11.WebMessage;
import org.hisp.dhis.integration.sdk.Dhis2ClientBuilder;
import org.hisp.dhis.integration.sdk.api.Dhis2Client;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@CamelSpringBootTest
@UseAdviceWith
public class TrackedEntityToFhirQuestionnaireRouteTestCase
{
    @Autowired
    protected CamelContext camelContext;

    @Autowired
    protected ProducerTemplate producerTemplate;

    @Autowired
    protected IGenericClient fhirClient;

    @Container
    public static PostgreSQLContainer<?> POSTGRESQL_CONTAINER;

    @Container
    public static GenericContainer<?> DHIS2_CONTAINER;

    @Container
    public static GenericContainer<?> FHIR_CONTAINER;

    private static final String ADMIN_USER_ID = "M5zQapPyTZI";

    private static PostgreSQLContainer<?> newPostgreSqlContainer()
    {
        return new PostgreSQLContainer<>( DockerImageName.parse( "postgis/postgis:12-3.3-alpine" )
            .asCompatibleSubstituteFor( "postgres" ) )
            .withDatabaseName( "dhis2" )
            .withNetworkAliases( "db" )
            .withUsername( "dhis" )
            .withStartupTimeout( Duration.of( 15, ChronoUnit.MINUTES ) )
            .withPassword( "dhis" ).withNetwork( Network.newNetwork() );
    }

    private static GenericContainer<?> newDhis2Container( PostgreSQLContainer<?> postgreSqlContainer )
    {
        return new GenericContainer<>( DockerImageName.parse( "dhis2/core:2.36.9" ) )
            .dependsOn( postgreSqlContainer )
            .withClasspathResourceMapping( "dhis.conf", "/DHIS2_home/dhis.conf", BindMode.READ_WRITE )
            .withNetwork( postgreSqlContainer.getNetwork() ).withExposedPorts( 8080 )
            .waitingFor( new HttpWaitStrategy().forStatusCode( 200 ).withStartupTimeout( Duration.ofSeconds( 120 ) ) )
            .withEnv( "WAIT_FOR_DB_CONTAINER", "db" + ":" + 5432 + " -t 0" );
    }

    private static GenericContainer<?> newFhirContainer()
    {
        return new GenericContainer<>( DockerImageName.parse( "hapiproject/hapi:v6.6.0" ) ).withExposedPorts( 8080 )
            .waitingFor( new HttpWaitStrategy().forStatusCode( 200 ).withStartupTimeout( Duration.ofSeconds( 120 ) ) );
    }

    @BeforeAll
    public static void beforeAll()
        throws
        Exception
    {
        POSTGRESQL_CONTAINER = newPostgreSqlContainer();
        POSTGRESQL_CONTAINER.start();
        DHIS2_CONTAINER = newDhis2Container( POSTGRESQL_CONTAINER );
        DHIS2_CONTAINER.start();
        FHIR_CONTAINER = newFhirContainer();
        FHIR_CONTAINER.start();

        String dhis2Url = String.format( "http://localhost:%s", DHIS2_CONTAINER.getFirstMappedPort() );

        Dhis2Client dhis2Client = Dhis2ClientBuilder.newClient( dhis2Url + "/api", "admin", "district" )
            .build();

        String orgUnitId = createOrgUnit( dhis2Client );

        createOrgUnitLevel( dhis2Client );
        addOrgUnitToUser( orgUnitId, ADMIN_USER_ID, dhis2Client );
        importMetaData( orgUnitId, dhis2Client );
        createAefiEvent( orgUnitId, dhis2Client );

        System.setProperty( "dhis2-to-fhir.dhis2.base-url", dhis2Url );
        System.setProperty( "dhis2-to-fhir.dhis2.username", "admin" );
        System.setProperty( "dhis2-to-fhir.dhis2.password", "district" );
        System.setProperty( "dhis2-to-fhir.fhir.server-url",
            String.format( "http://localhost:%s/fhir", FHIR_CONTAINER.getFirstMappedPort() ) );
        System.setProperty( "dhis2-to-fhir.dhis2.org-unit-id", orgUnitId );
    }

    private static void createAefiEvent( String orgUnitId, Dhis2Client dhis2Client )
        throws
        ParseException
    {
        TrackedEntityInstance tei = new TrackedEntityInstance().withAttributes(
                List.of( new Attribute__1().withAttribute( "KSr2yTdu1AI" ).withValue( "EPI_71755900" ),
                    new Attribute__1().withAttribute( "Xhdn49gUd52" ).withValue( "Wonderland" ),
                    new Attribute__1().withAttribute( "oindugucx72" ).withValue( "MALE" ),
                    new Attribute__1().withAttribute( "sB1IHYu2xQT" ).withValue( "John" ),
                    new Attribute__1().withAttribute( "NI0QRzJvQ0k" ).withValue( "2022-01-19" ) ) )
            .withEnrollments( List.of(
                new Enrollment().withEnrollmentDate( new SimpleDateFormat( "yyyy-MM-dd" ).parse( "2022-01-19" ) )
                    .withProgram( "EZkN8vYZwjR" ).withOrgUnit( orgUnitId )
                    .withStatus( Event.EnrollmentStatus.ACTIVE )
                    .withEvents( List.of(
                        new Event().withStatus( EventChart.EventStatus.SCHEDULE ).withDueDate( "2022-01-19" )
                            .withEventDate( "2022-01-19" ).withProgramStage( "so8YZ9J3MeO" )
                            .withProgram( "EZkN8vYZwjR" ).withOrgUnit( orgUnitId ) ) ) ) )
            .withOrgUnit( orgUnitId )
            .withTrackedEntityType( "MCPQUTHX1Ze" );

        WebMessage webMessage = dhis2Client.post( "trackedEntityInstances" )
            .withResource( tei ).transfer().returnAs(
                WebMessage.class );

        if ( !webMessage.getStatus().get().equals( DescriptiveWebMessage.Status.OK ) )
        {
            throw new RuntimeException();
        }
    }

    private static void addOrgUnitToUser( String orgUnitId, String userId, Dhis2Client dhis2Client )
        throws
        IOException
    {
        dhis2Client.post( "/users/{userId}/organisationUnits/{organisationUnitId}", userId, orgUnitId ).transfer()
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

    private static void importMetaData( String orgUnitId, Dhis2Client dhis2Client )
        throws
        IOException
    {
        String metaData = new String(
            Thread.currentThread().getContextClassLoader()
                .getResourceAsStream( "IMM_AEFI_COMPLETE_1.1.0_DHIS2.36.json" )
                .readAllBytes(),
            Charset.defaultCharset() );

        dhis2Client.post( "metadata" ).withResource( metaData.replaceAll( "<OU_UID>", orgUnitId ) )
            .withParameter( "atomicMode", "NONE" ).transfer()
            .close();
    }

    @Test
    public void testRoute()
        throws
        Exception
    {
        AdviceWith.adviceWith( camelContext, "dhis2-te-to-fhir-ques", r -> r.replaceFromWith( "direct:te2fhir" ) );
        camelContext.start();
        producerTemplate.sendBody( "direct:te2fhir", null );
        Bundle bundle = fhirClient.search().forResource( QuestionnaireResponse.class ).returnBundle( Bundle.class )
            .totalMode( SearchTotalModeEnum.ACCURATE ).execute();
        assertEquals( 1, bundle.getTotal() );
    }
}
