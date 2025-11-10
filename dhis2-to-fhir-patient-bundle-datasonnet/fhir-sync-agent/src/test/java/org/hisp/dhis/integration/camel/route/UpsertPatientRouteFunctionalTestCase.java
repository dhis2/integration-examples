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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.hisp.dhis.api.model.v40_2_2.AttributeInfo;
import org.hisp.dhis.api.model.v40_2_2.EnrollmentInfo;
import org.hisp.dhis.api.model.v40_2_2.EventInfo;
import org.hisp.dhis.api.model.v40_2_2.TrackedEntityInfo;
import org.hisp.dhis.api.model.v40_2_2.TrackerImportReport;
import org.hisp.dhis.integration.camel.AbstractFunctionalTestBase;
import org.hisp.dhis.integration.camel.util.FhirValidatorUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@UseAdviceWith
public class UpsertPatientRouteFunctionalTestCase extends AbstractFunctionalTestBase {
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
  public void testUpsertPatient() throws Exception {
    AdviceWith.adviceWith(camelContext, "upsertPatient", r -> r.weaveAddLast().to("mock:spy"));
    MockEndpoint spyEndpoint = camelContext.getEndpoint("mock:spy", MockEndpoint.class);
    spyEndpoint.setExpectedCount(1);

    camelContext.start();
    String orgUnit = "DiszpKrYNg8";
    TrackedEntityInfo trackedEntity = new TrackedEntityInfo()
        .withOrgUnit(orgUnit)
        .withTrackedEntityType("nEenWmSyUEp")
        .withAttributes(List.of(
            new AttributeInfo().withAttribute("lZGmxYbs97q").withValue("8437107"),  // Unique ID
            new AttributeInfo().withAttribute("VqEFza8wbwA").withValue("Madison Avenue 12"),  // Address
            new AttributeInfo().withAttribute("w75KJ2mc4zz").withValue("Jane"),  // First name
            new AttributeInfo().withAttribute("zDhUuAYrxNC").withValue("Doe"),  // Last name
            new AttributeInfo().withAttribute("FO4sWYJ64LQ").withValue("New York")  // City
        ))
        .withEnrollments(
            addEnrollment(
                orgUnit,
                List.of("WZbXY0S00lP")));

    dhis2Client
        .post("tracker")
        .withResource(Map.of("trackedEntities", List.of(trackedEntity)))
        .withParameter("async", "false")
        .transfer()
        .returnAs(TrackerImportReport.class)
        .getBundleReport()
        .get()
        .getTypeReportMap()
        .get()
        .getAdditionalProperties()
        .get("ENROLLMENT")
        .getObjectReports()
        .get()
        .get(0)
        .getUid()
        .get()
        .toString();

    spyEndpoint.assertIsSatisfied(30000);
    Bundle patientBundle = (Bundle) fhirClient.search().forResource(Patient.class).execute();
    List<Bundle.BundleEntryComponent> entries = patientBundle.getEntry();
    assertEquals(1, entries.size());
    Patient patient = (Patient) entries.get(0).getResource();
    assertEquals("8437107", patient.getIdentifier().get(0).getValue());
    
    org.hl7.fhir.r4.model.Parameters params = new org.hl7.fhir.r4.model.Parameters();
    params.addParameter().setName("resource").setResource(patient);

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

  public List<EnrollmentInfo> addEnrollment(String orgUnitId, List<String> programStageIds) {
    List<EventInfo> events = new ArrayList<>();

    String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    for (String programStage : programStageIds) {
      events.add(
          new EventInfo()
              .withProgramStage(programStage)
              .withOrgUnit(orgUnitId)
              .withScheduledAt(today)
              .withProgram("WSGAb5XwJ3Y") 
              .withStatus(EventInfo.StatusRef.SCHEDULE));
    }

    return List.of(
        new EnrollmentInfo()
            .withOrgUnit(orgUnitId)
            .withProgram("WSGAb5XwJ3Y")
            .withEnrolledAt(today)
            .withAttributes(
                List.of(
                    new AttributeInfo().withAttribute("Agywv2JGwuq").withValue("+13052065294"),
                    new AttributeInfo().withAttribute("ZcBPrXKahq2").withValue("10022"),
                    new AttributeInfo().withAttribute("KmEUg2hHEtx").withValue("jane@doe.com"),
                    new AttributeInfo().withAttribute("ciq2USN94oJ").withValue("Single or widow"),
                    new AttributeInfo().withAttribute("w75KJ2mc4zz").withValue("Jane"),
                    new AttributeInfo().withAttribute("zDhUuAYrxNC").withValue("Doe"),
                    new AttributeInfo().withAttribute("FO4sWYJ64LQ").withValue("New York"),
                    new AttributeInfo().withAttribute("gHGyrwKPzej").withValue("1997-04-18"),
                    new AttributeInfo().withAttribute("VqEFza8wbwA").withValue("Madison Avenue 12"),
                    new AttributeInfo().withAttribute("lZGmxYbs97q").withValue("8437107"),
                    new AttributeInfo().withAttribute("gu1fqsmoU8r").withValue("NSAIDS")
                ))
            .withOccurredAt(today)
            .withStatus(EnrollmentInfo.StatusRef.ACTIVE)
            .withEvents(events));
  }
}
