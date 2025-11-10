# Hands-On: Extending the DHIS2 to FHIR Mapping

The Patient resource is already implemented and working. Your job is to complete the mapping by adding the missing pieces. This gives you hands-on experience with the transformation pattern while achieving a lossless mapping.

## What You'll Build

Right now, the mapping only creates Patient resources. But we're losing data:
- Civil status ("Single or widow") doesn't fit in Patient
- Allergies ("NSAIDS, Penicillin") need proper clinical resources
- The DHIS2 user who created the record isn't captured

You'll add:
1. **Practitioner resource** - Map the DHIS2 user
2. **Observation resource** - Capture civil status
3. **AllergyIntolerance resources** - One per allergy (split comma-separated values)

## Setup

```bash
cd fhir-sync-agent
mvn clean test -Dtest=IpsPatientMappingTestCase
```

**Current state:** 7 tests pass, 5 tests fail. Your job is to get all 12 passing.

## The Pattern

Look at `patientResource.libsonnet` to see how it's done:

1. Import helpers
2. Extract values with `helpers.getAttrValue(tei, 'attributeId')`
3. Transform using helper functions
4. Build the resource with `std.prune()` to remove nulls
5. Return `{fullUrl, resource, request}`

Every resource follows this pattern.

## Task 1: Add Practitioner

The IPS Patient profile expects `generalPractitioner` to reference who's managing the patient. We can map the DHIS2 user who created the tracked entity.

**What you will do:**

1. Use `practitionerResource.todo.libsonnet` (has guided TODOs) or create from scratch
2. Extract user info from `tei.createdBy` (uid, firstName, surname)
3. Build a Practitioner resource with identifier and name
4. Use conditional PUT so the same user doesn't get duplicated
5. Update Patient to reference the Practitioner
6. Wire it into the bundle

The TODO file walks you through each step. Key things to remember:
- Use `helpers.parseName()` for the name
- The identifier system is `urn:dhis2:user:uid`
- The `fullUrl` is `urn:uuid:practitioner-{uid}` so Patient can reference it
- Conditional PUT: `Practitioner?identifier=urn:dhis2:user:uid|{uid}`

**Test it:**
```bash
mvn test -Dtest=IpsPatientMappingTestCase#testPractitionerResourceExists
```

## Task 2: Civil Status Observation

The tracked entity has a "Civil status" attribute (`ciq2USN94oJ`) with values like "Single or widow". There's no field for this in the Patient resource.

**The solution:** Create an Observation resource. Observations are perfect for capturing facts that don't have a dedicated resource or field.

**What you'll do:**

Use `observationResources.todo.libsonnet` - it has TODOs 2-13 for civil status.

Key concepts:
- Extract with `helpers.getAttrValue(tei, ATTR_CIVIL_STATUS)`
- Return null if no data (gets pruned from bundle)
- Create unique identifier: `attributeId + '-' + tei.trackedEntity`
- Use LOINC code `45404-1` (standard code for marital status)
- Category is `social-history`
- Subject references the Patient
- Use PUT with identifier query for conditional updates

Why PUT instead of POST? Again, when the tracked entity updates in DHIS2 and we sync again, PUT will update the existing Observation instead of creating a duplicate.

**Test it:**
```bash
mvn test -Dtest=IpsPatientMappingTestCase#testCivilStatusObservationExists
```

## Task 3: AllergyIntolerance Resources (Advanced)

The allergies attribute (`gu1fqsmoU8r`) stores comma-separated values: `"NSAIDS, Penicillin, ..."`. We need to create separate AllergyIntolerance resources for each allergy.

**What you will do:**

Continue in `observationResources.todo.libsonnet` - TODOs 14-26 cover allergies.

This is more advanced because it involves:

1. **Splitting the multi-value field:**
   ```jsonnet
   local allergiesList = std.split(allergiesValue, ',');
   ```

2. **List comprehension** to create multiple resources:
   ```jsonnet
   [
     { /* resource definition */ }
     for allergy in allergiesList
     if std.length(std.stripChars(allergy, ' ')) > 0
   ]
   ```

3. **Unique ID per allergy:**
   ```jsonnet
   local allergyText = std.stripChars(allergy, ' ');
   local allergyId = ATTR_ALLERGIES + '-' + tei.trackedEntity + '-' + allergyText;
   ```

The TODO file guides you through building the AllergyIntolerance resource structure. Key points:
- `clinicalStatus`: 'active' (currently relevant)
- `verificationStatus`: 'unconfirmed' (self-reported, not clinically verified)
- `patient`: reference to Patient
- `reaction.manifestation`: the allergy text
- Conditional PUT for each allergy

**Wiring it up:**

In `fhirBundle.ds`, note the `+` operator to concatenate arrays:

```jsonnet
entry: std.prune([
  patient.patient_entry(body),
  practitioner.practitioner_entry(body),
  observations.civil_status_observation(body),
] + observations.allergies_resources(body))
```

The `allergies_resources()` function returns an array, so we concatenate it with the other single entries.

**Test everything:**
```bash
mvn test -Dtest=IpsPatientMappingTestCase  # All 12 should pass!
```

## What You Learned

By completing this exercise, you will have seen:

**Complete mapping** - When data doesn't fit the target profile, use additional resources rather than discarding it. Observations work for generic facts, but prefer specialized resources (AllergyIntolerance) when they exist. Following this approach ensures that we both capture all of the `MustSupport` elements for the IPS Profile while still including additional elements captured in DHIS2. 

**Conditional updates** - Using `PUT` with identifier queries (`Resource?identifier=system|value`) enables idempotent operations. Push the same data twice and you get updates to existing FHIR resources.

**The transformation pattern** - Each resource that is included in the final FHIR bundle is created using the same pattern: Extract, Transform, Build, insert in Bundle.

**List comprehension** - Jsonnet's `[{item} for x in array if condition]` lets you create multiple resources from a single multi-value attribute.

## Next Steps

- Check out the solution files in `src/main/resources/solutions/` to compare approaches
- Try adding more attributes to your DHIS2 metadata, and try to capture the changes in the  Datasonnet transformations (age, next of kin, etc.)
- Add support for program stage events
- Experiment with other FHIR resources (Condition, Medication, etc.)

It is important to remember that the concepts you learned here apply to any DHIS2-to-FHIR mapping, not just IPS Patient profiles.
