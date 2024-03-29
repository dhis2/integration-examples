package org.hisp.dhis.fhir.routes;

import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@CamelSpringBootTest
@UseAdviceWith
public class OrgUnitToFhirBundleRouteTestCase
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

    private static PostgreSQLContainer<?> newPostgreSqlContainer()
    {
        return new PostgreSQLContainer<>( DockerImageName.parse( "postgis/postgis:12-3.3-alpine" )
            .asCompatibleSubstituteFor( "postgres" ) )
            .withFileSystemBind( "../db-dump", "/docker-entrypoint-initdb.d/", BindMode.READ_WRITE )
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
        File file = new File( "../db-dump/dhis2-db-sierra-leone.sql.gz" );
        if ( !file.exists() )
        {
            System.out.println( "Downloading database dump..." );
            FileUtils.copyURLToFile(
                new URL( "https://databases.dhis2.org/sierra-leone/2.36.9/dhis2-db-sierra-leone.sql.gz" ),
                file );
        }
        POSTGRESQL_CONTAINER = newPostgreSqlContainer();
        POSTGRESQL_CONTAINER.start();
        DHIS2_CONTAINER = newDhis2Container( POSTGRESQL_CONTAINER );
        DHIS2_CONTAINER.start();
        FHIR_CONTAINER = newFhirContainer();
        FHIR_CONTAINER.start();

        System.setProperty( "dhis2-to-fhir.dhis2.base-url",
            String.format( "http://localhost:%s", DHIS2_CONTAINER.getFirstMappedPort() ) );
        System.setProperty( "dhis2-to-fhir.fhir.server-url",
            String.format( "http://localhost:%s/fhir", FHIR_CONTAINER.getFirstMappedPort() ) );
    }

    @Test
    public void testRoute()
        throws
        Exception
    {
        AdviceWith.adviceWith( camelContext, "dhis2-ou-to-fhir-bundle", r -> r.replaceFromWith( "direct:ou2fhir" ) );
        camelContext.start();
        producerTemplate.sendBody( "direct:ou2fhir", null );
        Bundle bundle = fhirClient.search().forResource( Organization.class ).returnBundle( Bundle.class )
            .totalMode( SearchTotalModeEnum.ACCURATE ).execute();
        assertEquals( 1332, bundle.getTotal() );
    }
}
