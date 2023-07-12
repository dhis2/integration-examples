package org.hisp.dhis.fhir.routes;

import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.hisp.dhis.fhir.config.properties.DhisProperties;
import org.hisp.dhis.fhir.config.properties.FhirProperties;
import org.hisp.dhis.fhir.domain.TrackedEntities;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class TrackedEntityToFhirBundleRoute extends RouteBuilder
{
    private final DhisProperties dhisProperties;

    private final FhirProperties fhirProperties;

    @Override
    public void configure()
    {
        String sourceUrl = dhisProperties.getBaseUrl() + "/api/trackedEntityInstances.json?ou=DiszpKrYNg8&program=IpHINAT79UW";
        String basicAuth = HttpHeaders.encodeBasicAuth( dhisProperties.getUsername(), dhisProperties.getPassword(), StandardCharsets.UTF_8 );

        JacksonDataFormat jacksonDataFormat = new JacksonDataFormat( TrackedEntities.class );
        jacksonDataFormat.setPrettyPrint( true );

        from( "timer://foo?repeatCount=1" ).routeId( "dhis2-te-to-fhir-bundle" )
            .setHeader( "Authorization", constant( String.format( "Basic %s", basicAuth ) ) )
            .to( sourceUrl )
            .unmarshal( jacksonDataFormat )
            .log( "Converting ${body.trackedEntities.size()} tracked entities." )
            .convertBodyTo( Bundle.class )
            .marshal().fhirJson( fhirProperties.getFhirVersion().name(), true )
            .to( "fhir://transaction/withBundle?client=#fhirClient" )
            .log( "Done." );
    }
}
