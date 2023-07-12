package org.hisp.dhis.fhir.routes;

import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@CamelSpringBootTest
@UseAdviceWith
public class OrgUnitToFhirBundleRouteTestCase extends AbstractRouteTestCase
{
    @Test
    public void testRoute()
    {
        producerTemplate.sendBody( "direct:ou2fhir", null );
        Bundle bundle = fhirClient.search().forResource( Organization.class ).returnBundle(Bundle.class).totalMode( SearchTotalModeEnum.ACCURATE ).execute();
        assertEquals(1332, bundle.getTotal());
    }
}
