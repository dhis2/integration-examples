package org.hisp.dhis.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.fhir.config.properties.DhisProperties;
import org.hisp.dhis.fhir.config.properties.FhirProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@RequiredArgsConstructor
@EnableConfigurationProperties( { DhisProperties.class, FhirProperties.class } )
public class MainApplication
{
    private final FhirProperties fhirProperties;

    @Bean
    public FhirContext fhirContext()
    {
        return fhirProperties.getFhirVersion().newContext();
    }

    @Bean
    public IGenericClient fhirClient( FhirContext fhirContext )
    {
        fhirContext.getRestfulClientFactory().setSocketTimeout( 50000 );
        return fhirContext.newRestfulGenericClient( fhirProperties.getServerUrl() );
    }

    public static void main( String[] args )
    {
        SpringApplication.run( MainApplication.class, args );
    }
}
