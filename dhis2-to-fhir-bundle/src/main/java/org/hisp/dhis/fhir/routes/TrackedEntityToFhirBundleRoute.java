package org.hisp.dhis.fhir.routes;

import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.hisp.dhis.api.model.v2_39_1.TrackedEntityInstance;
import org.hisp.dhis.fhir.aggregate.PatientBundleAggregationStrategy;
import org.hisp.dhis.fhir.config.properties.FhirProperties;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TrackedEntityToFhirBundleRoute extends RouteBuilder
{
    private final FhirProperties fhirProperties;

    @Override
    public void configure()
    {
        from( "timer://foo?repeatCount=1" ).routeId( "dhis2-te-to-fhir-bundle" )
            .setHeader( "CamelDhis2.queryParams", () -> Map.of( "ou", "DiszpKrYNg8", "program", "IpHINAT79UW" ) )
            .to("dhis2://get/collection?path=trackedEntityInstances&arrayName=trackedEntityInstances&client=#dhis2Client")
            .log( "Converting DHIS2 tracked entities to FHIR patients..." )
            .split().body().aggregationStrategy( new PatientBundleAggregationStrategy() )
                .convertBodyTo( TrackedEntityInstance.class )
                .convertBodyTo( Patient.class )
            .end()
            .marshal().fhirJson( fhirProperties.getFhirVersion().name(), true )
            .to( "fhir://transaction/withBundle?client=#fhirClient" )
            .log( "Saved patients FHIR bundle" );
    }
}
