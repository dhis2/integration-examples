package org.hisp.dhis.fhir.routes;

import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.hisp.dhis.fhir.config.properties.DhisProperties;
import org.hisp.dhis.fhir.domain.OrganisationUnits;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OrgUnitToFhirBundleRoute extends RouteBuilder
{
    private final DhisProperties dhisProperties;

    @Override
    public void configure()
    {
        String sourceUrl = dhisProperties.getBaseUrl() + "/api/organisationUnits.json?fields=id,code,name,description,parent[id]&paging=false";
        String basicAuth = HttpHeaders.encodeBasicAuth( dhisProperties.getUsername(), dhisProperties.getPassword(), StandardCharsets.UTF_8 );

        JacksonDataFormat jacksonDataFormat = new JacksonDataFormat( OrganisationUnits.class );
        jacksonDataFormat.setPrettyPrint( true );

        from( "timer://foo?repeatCount=1" ).routeId( "dhis2-ou-to-fhir-bundle" )
            .setHeader( "Authorization", constant( String.format( "Basic %s", basicAuth ) ) )
            .to( sourceUrl )
            .transform( datasonnet( "resource:classpath:bundle.ds", Map.class, "application/json",
                "application/x-java-object" ) )
            .marshal().json()
            .convertBodyTo( String.class )
            .to( "fhir://transaction/withBundle?inBody=stringBundle&client=#fhirClient" )
            .log( "Done." );
    }
}
