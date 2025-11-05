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
package org.hisp.dhis.integration.camel;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.language.DatasonnetExpression;
import org.apache.camel.support.DefaultExchange;
import org.hisp.dhis.integration.camel.util.FhirValidatorUtil;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class IpsPatientMappingTestCase {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String IPS_PATIENT_PROFILE = "http://hl7.org/fhir/uv/ips/StructureDefinition/Patient-uv-ips";

    private FhirContext fhirContext;
    private FhirValidator validator;
    private DefaultExchange exchange;
    private DatasonnetExpression dsExpression;

    private Map<String, Object> loadTrackedEntity(String filename) throws IOException {
        return OBJECT_MAPPER.readValue(
            StreamUtils.copyToString(
                Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(filename),
                Charset.defaultCharset()),
            Map.class);
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        fhirContext = FhirContext.forR4();
        
        // Load IPS profile from classpath
        PrePopulatedValidationSupport prePopulatedSupport = new PrePopulatedValidationSupport(fhirContext);
        String ipsProfileJson = StreamUtils.copyToString(
            new ClassPathResource("StructureDefinition-Patient-uv-ips.json").getInputStream(),
            Charset.defaultCharset()
        );
        org.hl7.fhir.r4.model.StructureDefinition ipsProfile = 
            fhirContext.newJsonParser().parseResource(
                org.hl7.fhir.r4.model.StructureDefinition.class, 
                ipsProfileJson
            );
        prePopulatedSupport.addStructureDefinition(ipsProfile);
        
        ValidationSupportChain validationSupportChain = new ValidationSupportChain(
            new DefaultProfileValidationSupport(fhirContext),
            prePopulatedSupport,
            new InMemoryTerminologyServerValidationSupport(fhirContext),
            new CommonCodeSystemsTerminologyService(fhirContext),
            new SnapshotGeneratingValidationSupport(fhirContext)
        );
        
        // Create and configure validator
        validator = fhirContext.newValidator();
        FhirInstanceValidator instanceValidator = new FhirInstanceValidator(validationSupportChain);
        validator.registerValidatorModule(instanceValidator);
        
        // Set up DataSonnet expression for transformation
        dsExpression = new DatasonnetExpression("resource:classpath:fhirBundle.ds");
        dsExpression.setResultType(Map.class);
        dsExpression.setBodyMediaType("application/x-java-object");
        dsExpression.setOutputMediaType("application/x-java-object");

        CamelContext camelContext = new DefaultCamelContext();
        exchange = new DefaultExchange(camelContext);
    }

    @Test
    public void testBundleStructure() throws IOException {
        Map<String, Object> trackedEntity = loadTrackedEntity("trackedEntity.json");
        exchange.getMessage().setBody(trackedEntity);


        // Perform datasonnet transformation
        Map<String, Object> fhirBundle = new ValueBuilder(dsExpression).evaluate(exchange, Map.class);

        assertEquals("Bundle", fhirBundle.get("resourceType"), "Should produce a Bundle resource");
        assertEquals("transaction", fhirBundle.get("type"), "Bundle type should be transaction");
        
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> entries = (java.util.List<Map<String, Object>>) fhirBundle.get("entry");
        assertNotNull(entries, "Bundle should have entries");
        assertFalse(entries.isEmpty(), "Bundle should have at least one entry");
        
        Map<String, Object> firstEntry = entries.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> resource = (Map<String, Object>) firstEntry.get("resource");
        assertEquals("Patient", resource.get("resourceType"), "First entry should be a Patient resource");
    }

    @Test
    public void testPatientConformsToIpsProfile() throws Exception {
        Map<String, Object> trackedEntity = loadTrackedEntity("trackedEntity.json");
        exchange.getMessage().setBody(trackedEntity);

        Map<String, Object> fhirBundle = new ValueBuilder(dsExpression).evaluate(exchange, Map.class);
        
        String bundleJson = OBJECT_MAPPER.writeValueAsString(fhirBundle);
        
        Bundle bundle = fhirContext.newJsonParser().parseResource(Bundle.class, bundleJson);
        
        assertFalse(bundle.getEntry().isEmpty(), "Bundle should have entries");
        Patient patient = (Patient) bundle.getEntryFirstRep().getResource();
        
        assertTrue(
            patient.getMeta().getProfile().stream()
                .anyMatch(p -> p.getValue().equals(IPS_PATIENT_PROFILE)),
            "Patient should declare IPS profile"
        );
        
        // Validate against IPS profile
        ValidationResult result = validator.validateWithResult(patient);
        
        // Assert validation success
        assertTrue(
            result.isSuccessful(), 
            "Patient should conform to IPS profile. Validation errors:\n" + 
            FhirValidatorUtil.extractValidationErrors((org.hl7.fhir.r4.model.OperationOutcome) result.toOperationOutcome())
        );
    }

    @Test
    public void testPatientNameConstraint() throws Exception {
        Map<String, Object> trackedEntity = loadTrackedEntity("trackedEntity.json");
        exchange.getMessage().setBody(trackedEntity);

        Map<String, Object> fhirBundle = new ValueBuilder(dsExpression).evaluate(exchange, Map.class);
        
        Bundle bundle = fhirContext.newJsonParser().parseResource(
            Bundle.class, 
            OBJECT_MAPPER.writeValueAsString(fhirBundle)
        );
        Patient patient = (Patient) bundle.getEntryFirstRep().getResource();
        
        assertFalse(patient.getName().isEmpty(), "Patient must have at least one name");
        
        // Verify ips-pat-1: at least one of given, family, or text
        org.hl7.fhir.r4.model.HumanName name = patient.getNameFirstRep();
        boolean hasGiven = !name.getGiven().isEmpty();
        boolean hasFamily = name.hasFamily();
        boolean hasText = name.hasText();
        
        assertTrue(
            hasGiven || hasFamily || hasText,
            "Name must have at least one of: given, family, or text (ips-pat-1 constraint)"
        );
    }

    @Test
    public void testPatientIdentifier() throws Exception {
        Map<String, Object> trackedEntity = loadTrackedEntity("trackedEntity.json");
        exchange.getMessage().setBody(trackedEntity);

        Map<String, Object> fhirBundle = new ValueBuilder(dsExpression).evaluate(exchange, Map.class);
        
        Bundle bundle = fhirContext.newJsonParser().parseResource(
            Bundle.class, 
            OBJECT_MAPPER.writeValueAsString(fhirBundle)
        );
        Patient patient = (Patient) bundle.getEntryFirstRep().getResource();
        
        assertFalse(patient.getIdentifier().isEmpty(), "Patient should have at least one identifier");
        
        org.hl7.fhir.r4.model.Identifier identifier = patient.getIdentifierFirstRep();
        assertTrue(identifier.hasSystem(), "Identifier must have a system");
        assertTrue(identifier.hasValue(), "Identifier must have a value");
        assertEquals("official", identifier.getUse().toCode(), "Primary identifier should be official use");
        
        assertEquals("8437107", identifier.getValue(), "Identifier value should match DHIS2 Unique ID");
    }

    @Test
    public void testDemographicDataMapping() throws Exception {
        Map<String, Object> trackedEntity = loadTrackedEntity("trackedEntity.json");
        exchange.getMessage().setBody(trackedEntity);

        Map<String, Object> fhirBundle = new ValueBuilder(dsExpression).evaluate(exchange, Map.class);
        
        Bundle bundle = fhirContext.newJsonParser().parseResource(
            Bundle.class, 
            OBJECT_MAPPER.writeValueAsString(fhirBundle)
        );
        Patient patient = (Patient) bundle.getEntryFirstRep().getResource();
        
        assertTrue(patient.hasBirthDate(), "Patient should have birth date");
        assertEquals("1997-04-18", patient.getBirthDateElement().getValueAsString());
        
        assertFalse(patient.getAddress().isEmpty(), "Patient should have address");
        org.hl7.fhir.r4.model.Address address = patient.getAddressFirstRep();
        assertTrue(address.hasLine(), "Address should have line");
        assertTrue(address.hasCity(), "Address should have city");
        assertTrue(address.hasPostalCode(), "Address should have postal code");
        
        assertFalse(patient.getTelecom().isEmpty(), "Patient should have telecom");
        
        assertTrue(
            patient.getTelecom().stream().anyMatch(t -> t.getSystem().toCode().equals("phone")) ||
            patient.getTelecom().stream().anyMatch(t -> t.getSystem().toCode().equals("email")),
            "Patient should have phone or email contact"
        );
    }

    @Test
    public void testConditionalUpdateRequest() throws Exception {
        Map<String, Object> trackedEntity = loadTrackedEntity("trackedEntity.json");
        exchange.getMessage().setBody(trackedEntity);

        Map<String, Object> fhirBundle = new ValueBuilder(dsExpression).evaluate(exchange, Map.class);
        
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> entries = 
            (java.util.List<Map<String, Object>>) fhirBundle.get("entry");
        Map<String, Object> firstEntry = entries.get(0);
        
        assertTrue(firstEntry.containsKey("request"), "Entry should have request element");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) firstEntry.get("request");
        assertEquals("PUT", request.get("method"), "Request method should be PUT for conditional update");
        
        String url = (String) request.get("url");
        assertTrue(url.startsWith("Patient?identifier="), "Request URL should include identifier query");
        assertTrue(url.contains("8437107"), "Request URL should include the patient's unique ID");
    }

    @Test
    public void testMinimalDataHandling() throws Exception {
        Map<String, Object> minimalTei = loadTrackedEntity("minimalTrackedEntity.json");
        exchange.getMessage().setBody(minimalTei);

        Map<String, Object> fhirBundle = new ValueBuilder(dsExpression).evaluate(exchange, Map.class);
        
        Bundle bundle = fhirContext.newJsonParser().parseResource(
            Bundle.class, 
            OBJECT_MAPPER.writeValueAsString(fhirBundle)
        );
        Patient patient = (Patient) bundle.getEntryFirstRep().getResource();
        
        assertFalse(patient.getName().isEmpty(), "Even minimal patient should have a name");
        
        ValidationResult result = validator.validateWithResult(patient);
        assertTrue(
            result.isSuccessful(),
            "Minimal patient should still conform to IPS profile. Errors:\n" +
            FhirValidatorUtil.extractValidationErrors((org.hl7.fhir.r4.model.OperationOutcome) result.toOperationOutcome())
        );
    }

    // EXERCISE TEST 1
    @Test
    public void testPractitionerResourceExists() throws Exception {
        Map<String, Object> trackedEntity = loadTrackedEntity("trackedEntity.json");
        exchange.getMessage().setBody(trackedEntity);

        Map<String, Object> fhirBundle = new ValueBuilder(dsExpression).evaluate(exchange, Map.class);
        
        Bundle bundle = fhirContext.newJsonParser().parseResource(
            Bundle.class, 
            OBJECT_MAPPER.writeValueAsString(fhirBundle)
        );
        
        // Find Practitioner resource
        org.hl7.fhir.r4.model.Practitioner practitioner = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r.getResourceType().name().equals("Practitioner"))
            .map(r -> (org.hl7.fhir.r4.model.Practitioner) r)
            .findFirst()
            .orElse(null);
        
        assertNotNull(practitioner, "Bundle should contain a Practitioner resource");
        
        assertFalse(practitioner.getIdentifier().isEmpty(), "Practitioner should have identifier");
        assertTrue(
            practitioner.getIdentifier().stream()
                .anyMatch(id -> id.getSystem().equals("urn:dhis2:user:uid") && id.getValue().equals("xE7jOejl9FI")),
            "Practitioner should have DHIS2 user UID identifier"
        );
        
        assertFalse(practitioner.getName().isEmpty(), "Practitioner should have name");
    }

    // EXERCISE TEST 2
    @Test
    public void testPatientReferencesPractitioner() throws Exception {
        Map<String, Object> trackedEntity = loadTrackedEntity("trackedEntity.json");
        exchange.getMessage().setBody(trackedEntity);

        Map<String, Object> fhirBundle = new ValueBuilder(dsExpression).evaluate(exchange, Map.class);
        
        Bundle bundle = fhirContext.newJsonParser().parseResource(
            Bundle.class, 
            OBJECT_MAPPER.writeValueAsString(fhirBundle)
        );
        
        Patient patient = (Patient) bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r.getResourceType().name().equals("Patient"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Patient resource not found"));
        
        assertFalse(
            patient.getGeneralPractitioner().isEmpty(), 
            "Patient should have generalPractitioner reference"
        );
        
        org.hl7.fhir.r4.model.Reference practitionerRef = patient.getGeneralPractitionerFirstRep();
        assertTrue(
            practitionerRef.getReference().contains("practitioner-xE7jOejl9FI"),
            "Patient should reference Practitioner with correct UID"
        );
        assertTrue(
            practitionerRef.hasDisplay(),
            "Practitioner reference should have display name"
        );
    }

    // EXERCISE TEST 3
    @Test
    public void testCivilStatusObservation() throws Exception {
        Map<String, Object> trackedEntity = loadTrackedEntity("trackedEntity.json");
        exchange.getMessage().setBody(trackedEntity);

        Map<String, Object> fhirBundle = new ValueBuilder(dsExpression).evaluate(exchange, Map.class);
        
        Bundle bundle = fhirContext.newJsonParser().parseResource(
            Bundle.class, 
            OBJECT_MAPPER.writeValueAsString(fhirBundle)
        );
        
        org.hl7.fhir.r4.model.Observation observation = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r.getResourceType().name().equals("Observation"))
            .map(r -> (org.hl7.fhir.r4.model.Observation) r)
            .filter(obs -> obs.getCode().getCoding().stream()
                .anyMatch(coding -> coding.getCode().equals("45404-1")))
            .findFirst()
            .orElse(null);
        
        assertNotNull(observation, "Bundle should contain civil status Observation");
        
        assertTrue(
            observation.getCode().getCoding().stream()
                .anyMatch(c -> c.getSystem().equals("http://loinc.org") && c.getCode().equals("45404-1")),
            "Observation should have LOINC code 45404-1 (Marital status)"
        );
        
        assertTrue(
            observation.getCategory().stream()
                .flatMap(cat -> cat.getCoding().stream())
                .anyMatch(c -> c.getCode().equals("social-history")),
            "Observation should have social-history category"
        );
        
        assertTrue(
            observation.getSubject().getReference().contains("QfFVkOL8ixj"),
            "Observation should reference Patient"
        );
        
        assertTrue(
            observation.hasValueStringType(),
            "Observation should have valueString"
        );
        assertEquals(
            "Single or widow",
            observation.getValueStringType().getValue(),
            "Observation value should match civil status from DHIS2"
        );
    }

    // EXERCISE TEST 4
    @Test
    public void testAllergiesResource() throws Exception {
        Map<String, Object> trackedEntity = loadTrackedEntity("trackedEntity.json");
        exchange.getMessage().setBody(trackedEntity);

        Map<String, Object> fhirBundle = new ValueBuilder(dsExpression).evaluate(exchange, Map.class);
        
        Bundle bundle = fhirContext.newJsonParser().parseResource(
            Bundle.class, 
            OBJECT_MAPPER.writeValueAsString(fhirBundle)
        );
        
        java.util.List<org.hl7.fhir.r4.model.AllergyIntolerance> allergies = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r.getResourceType().name().equals("AllergyIntolerance"))
            .map(r -> (org.hl7.fhir.r4.model.AllergyIntolerance) r)
            .collect(java.util.stream.Collectors.toList());
        
        assertFalse(allergies.isEmpty(), "Bundle should contain at least one AllergyIntolerance resource");
        
        org.hl7.fhir.r4.model.AllergyIntolerance allergy = allergies.get(0);
        
        assertTrue(
            allergy.getClinicalStatus().getCoding().stream()
                .anyMatch(c -> c.getCode().equals("active")),
            "AllergyIntolerance should have active clinical status"
        );
        
        assertTrue(
            allergy.getVerificationStatus().getCoding().stream()
                .anyMatch(c -> c.getCode().equals("unconfirmed")),
            "AllergyIntolerance should have unconfirmed verification status"
        );
        
        assertTrue(
            allergy.getPatient().getReference().contains("QfFVkOL8ixj"),
            "AllergyIntolerance should reference Patient"
        );
        
        assertFalse(allergy.getReaction().isEmpty(), "AllergyIntolerance should have reaction");
        assertFalse(
            allergy.getReactionFirstRep().getManifestation().isEmpty(),
            "AllergyIntolerance reaction should have manifestation"
        );
        
        String manifestationText = allergy.getReactionFirstRep().getManifestationFirstRep().getText();
        assertTrue(
            manifestationText.contains("NSAID") || manifestationText.equals("NSAIDS"),
            "AllergyIntolerance manifestation should contain allergy from DHIS2"
        );
    }

    // EXERCISE TEST 5
    @Test
    public void testBundleContainsAllResources() throws Exception {
        Map<String, Object> trackedEntity = loadTrackedEntity("trackedEntity.json");
        exchange.getMessage().setBody(trackedEntity);

        Map<String, Object> fhirBundle = new ValueBuilder(dsExpression).evaluate(exchange, Map.class);
        
        Bundle bundle = fhirContext.newJsonParser().parseResource(
            Bundle.class, 
            OBJECT_MAPPER.writeValueAsString(fhirBundle)
        );
        
        java.util.Map<String, Long> resourceCounts = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .collect(java.util.stream.Collectors.groupingBy(
                r -> r.getResourceType().name(),
                java.util.stream.Collectors.counting()
            ));
        
        // Verify all expected resource types exist
        assertTrue(resourceCounts.containsKey("Patient"), "Bundle should contain Patient");
        assertTrue(resourceCounts.containsKey("Practitioner"), "Bundle should contain Practitioner");
        assertTrue(resourceCounts.containsKey("Observation"), "Bundle should contain Observation");
        assertTrue(resourceCounts.containsKey("AllergyIntolerance"), "Bundle should contain AllergyIntolerance");
        
        assertEquals(1L, resourceCounts.get("Patient"), "Bundle should have exactly 1 Patient");
        assertEquals(1L, resourceCounts.get("Practitioner"), "Bundle should have exactly 1 Practitioner");
        assertTrue(resourceCounts.get("Observation") >= 1, "Bundle should have at least 1 Observation");
        assertTrue(resourceCounts.get("AllergyIntolerance") >= 1, "Bundle should have at least 1 AllergyIntolerance");
        
        assertEquals("transaction", bundle.getType().toCode(), "Bundle type should be transaction");
        
        bundle.getEntry().forEach(entry -> {
            assertTrue(entry.hasRequest(), "All bundle entries should have request element");
            assertTrue(
                entry.getRequest().getMethod().toCode().equals("PUT") || 
                entry.getRequest().getMethod().toCode().equals("POST"),
                "Request method should be PUT or POST"
            );
        });
    }
}
