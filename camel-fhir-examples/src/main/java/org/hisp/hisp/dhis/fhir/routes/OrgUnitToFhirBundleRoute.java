package org.hisp.hisp.dhis.fhir.routes;

import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.hisp.hisp.dhis.fhir.config.properties.DhisProperties;
import org.hisp.hisp.dhis.fhir.config.properties.FhirProperties;
import org.hisp.hisp.dhis.fhir.domain.OrganisationUnits;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OrgUnitToFhirBundleRoute extends RouteBuilder
{
    private final DhisProperties dhisProperties;

    private final FhirProperties fhirProperties;

    @Override
    public void configure()
    {
        String sourceUrl = dhisProperties.getBaseUrl() + "/api/organisationUnits.json?fields=id,code,name,description,parent[id]&paging=false";
        String basicAuth = HttpHeaders.encodeBasicAuth( dhisProperties.getUsername(), dhisProperties.getPassword(), StandardCharsets.UTF_8 );

        from( "timer://foo?repeatCount=1" ).routeId( "dhis2-ou-to-fhir-bundle" )
            .setHeader( "Authorization", constant( String.format( "Basic %s", basicAuth ) ) )
            .to( sourceUrl )
            .unmarshal( new JacksonDataFormat( OrganisationUnits.class ) )
            .log( "Converting ${body.organisationUnits.size()} organisation units." )
            .convertBodyTo( Bundle.class )
            // .to( "fhir://transaction/withBundle?client=#fhirClient" )
            .marshal().fhirJson( fhirProperties.getFhirVersion().name(), true )
            .to( "file:data/fhir-output?filename=orgUnits.json" )
            .log( "Done." );
    }
}
