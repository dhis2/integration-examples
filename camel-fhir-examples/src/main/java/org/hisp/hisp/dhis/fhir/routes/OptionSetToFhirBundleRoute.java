package org.hisp.hisp.dhis.fhir.routes;

import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.hisp.hisp.dhis.fhir.config.properties.DhisProperties;
import org.hisp.hisp.dhis.fhir.config.properties.FhirProperties;
import org.hisp.hisp.dhis.fhir.domain.OptionSets;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OptionSetToFhirBundleRoute extends RouteBuilder
{
    private final DhisProperties dhisProperties;

    private final FhirProperties fhirProperties;

    @Override
    public void configure()
    {
        String sourceUrl = dhisProperties.getBaseUrl()
            + "/api/optionSets.json?fields=id,code,name,description,version,options[id,code,name]&paging=false&filter=id:eq:HB33RvLvVZe";
        String basicAuth = HttpHeaders.encodeBasicAuth( dhisProperties.getUsername(), dhisProperties.getPassword(), StandardCharsets.UTF_8 );

        JacksonDataFormat jacksonDataFormat = new JacksonDataFormat( OptionSets.class );
        jacksonDataFormat.setPrettyPrint( true );

        from( "timer://foo?repeatCount=1" ).routeId( "dhis2-os-to-fhir-bundle" )
            .setHeader( "Authorization", constant( String.format( "Basic %s", basicAuth ) ) )
            .to( sourceUrl )
            .unmarshal( jacksonDataFormat )
            .log( "Converting ${body.optionSets.size()} option sets." )
            .convertBodyTo( Bundle.class )
            // .to( "fhir://transaction/withBundle?client=#fhirClient" )
            .marshal().fhirJson( fhirProperties.getFhirVersion().name(), true )
            .to( "file:data/fhir-output?filename=optionSets.json" )
            .log( "Done." );
    }
}
