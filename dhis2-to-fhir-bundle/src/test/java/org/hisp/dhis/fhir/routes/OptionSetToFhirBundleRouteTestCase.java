package org.hisp.dhis.fhir.routes;

import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@CamelSpringBootTest
@UseAdviceWith
public class OptionSetToFhirBundleRouteTestCase extends AbstractRouteTestCase
{
    @Test
    public void testRoute()
    {
        producerTemplate.sendBody( "direct:os2fhir", null );
        Bundle codeSystemBundle = fhirClient.search().forResource( CodeSystem.class ).returnBundle(Bundle.class).totalMode( SearchTotalModeEnum.ACCURATE ).execute();
        assertEquals(1, codeSystemBundle.getTotal());
        Bundle valueSetBundle = fhirClient.search().forResource( ValueSet.class ).returnBundle(Bundle.class).totalMode( SearchTotalModeEnum.ACCURATE ).execute();
        assertEquals(1, valueSetBundle.getTotal());
    }
}
