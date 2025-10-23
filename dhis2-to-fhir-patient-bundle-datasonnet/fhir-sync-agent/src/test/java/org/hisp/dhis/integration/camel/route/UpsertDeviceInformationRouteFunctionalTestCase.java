/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.integration.camel.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.hisp.dhis.integration.camel.AbstractFunctionalTestCase;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.hisp.dhis.integration.camel.util.FhirValidatorUtil;

public class UpsertDeviceInformationRouteFunctionalTestCase extends AbstractFunctionalTestCase {
  @Autowired private CamelContext camelContext;

  @BeforeEach
  public void beforeEach() throws Exception {
    startMockAuthorisationServer();
  }

  @AfterEach
  public void afterEach() throws Exception {
    stopMockAuthorisationServer();
  }

  @Test
  public void testUpsertDeviceInformation() throws Exception {
    AdviceWith.adviceWith(camelContext, "fetchDeviceInformation", r -> r.weaveAddLast().to("mock:spy"));
    MockEndpoint spyEndpoint = camelContext.getEndpoint("mock:spy", MockEndpoint.class);
    spyEndpoint.setExpectedCount(1);

    camelContext.start();

    spyEndpoint.assertIsSatisfied(30000);

    Bundle deviceBundle = (Bundle) fhirClient.search().forResource(Device.class).execute();
    assertTrue(deviceBundle.getEntry().size() > 0);
    Device device = (Device) deviceBundle.getEntry().get(0).getResource();
    assertEquals("2.42.1", device.getVersion().get(0).getValue());

    device.getMeta().addProfile("http://fhir.health.gov.lk/ips/StructureDefinition/device-information");
        org.hl7.fhir.r4.model.Parameters params = new org.hl7.fhir.r4.model.Parameters();
    
    params.addParameter().setName("resource").setResource(device);

    org.hl7.fhir.r4.model.Parameters resultParams = fhirClient
        .operation()
        .onType(Patient.class)
        .named("validate")
        .withParameters(params)
        .execute();

    org.hl7.fhir.r4.model.OperationOutcome outcome = (org.hl7.fhir.r4.model.OperationOutcome) resultParams.getParameterFirstRep().getResource();
    String errorDetails = FhirValidatorUtil.extractValidationErrors(outcome);
    boolean hasError = !errorDetails.isEmpty();
    assertEquals(false, hasError, errorDetails);
  }
}
