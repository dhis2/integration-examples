package org.hisp.dhis.fhir.routes;

import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@CamelSpringBootTest
@UseAdviceWith
public class TrackedEntityToFhirBundleRouteTestCase extends AbstractRouteTestCase
{
    @Test
    public void testRoute()
    {
        producerTemplate.sendBody( "direct:ou2fhir", null );
        producerTemplate.sendBody( "direct:te2fhir", null );
        Bundle bundle = fhirClient.search().forResource( Patient.class ).returnBundle(Bundle.class).totalMode( SearchTotalModeEnum.ACCURATE ).execute();
        assertEquals(36, bundle.getTotal());
    }
}
