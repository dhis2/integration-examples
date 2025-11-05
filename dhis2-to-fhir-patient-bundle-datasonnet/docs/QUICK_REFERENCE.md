# References for Exercise

## Test Commands

```bash
# Verify setup (should have 7 passing tests)
mvn clean test -Dtest=IpsPatientMappingTestCase

# Task 1: Practitioner
mvn test -Dtest=IpsPatientMappingTestCase#testPatientReferencesPractitioner

# Task 2: Civil Status
mvn test -Dtest=IpsPatientMappingTestCase#testCivilStatusObservation

# Task 3: Allergies
mvn test -Dtest=IpsPatientMappingTestCase#testAllergiesResource

# Final verification (should have 12 passing tests)
mvn test -Dtest=IpsPatientMappingTestCase
```

## File Locations

**Participants Create/Edit:**
- `src/main/resources/practitionerResource.libsonnet`
- `src/main/resources/observationResources.libsonnet`
- `src/main/resources/fhirBundle.ds` (add imports)
- `src/main/resources/patientResource.libsonnet` (add generalPractitioner)

**Solution Files (if stuck):**
- `src/main/resources/solutions/practitionerResource.solution.libsonnet`
- `src/main/resources/solutions/observationResources.solution.libsonnet`

**TODO Templates (for guidance):**
- `src/main/resources/practitionerResource.todo.libsonnet`
- `src/main/resources/observationResources.todo.libsonnet`

**Test Data:**
- `src/test/resources/trackedEntity.json` (complete data)
- `src/test/resources/minimalTrackedEntity.json` (minimal data)

## Key Attribute IDs from trackedEntity.json

```javascript
const ATTRIBUTES = {
  UNIQUE_ID: 'lZGmxYbs97q',      // "8437107"
  FIRST_NAME: 'w75KJ2mc4zz',     // "Jane"
  LAST_NAME: 'zDhUuAYrxNC',      // "Doe"
  BIRTH_DATE: 'gHGyrwKPzej',     // "1997-04-18"
  ADDRESS: 'VqEFza8wbwA',        // "Madison Avenue 12"
  CITY: 'FO4sWYJ64LQ',           // "New York"
  POSTAL: 'ZcBPrXKahq2',         // "10022"
  MOBILE: 'Agywv2JGwuq',         // "+13052065294"
  EMAIL: 'KmEUg2hHEtx',          // "jane@doe.com"
  CIVIL_STATUS: 'ciq2USN94oJ',   // "Single or widow" unmapped
  ALLERGIES: 'gu1fqsmoU8r'       // "NSAIDS" unmapped
};
```

## Code Snippets for Jsonnet/Datasonnet

### Import Pattern
```jsonnet
local helpers = import 'helperFunctions.libsonnet';
```

### Extract Attribute
```jsonnet
local value = helpers.getAttrValue(tei, 'attribute-id');
```

### Build "Name" FHIR DataType
```jsonnet
local name = helpers.parseName(firstName, lastName);
```

### Practitioner Pattern
```jsonnet
{
  fullUrl: 'urn:uuid:practitioner-' + userId,
  resource: {
    resourceType: 'Practitioner',
    identifier: [{ system: 'urn:dhis2:user:uid', value: userId }],
    name: [practitionerName]
  },
  request: {
    method: 'PUT',
    url: 'Practitioner?identifier=urn:dhis2:user:uid|' + userId
  }
}
```

### Observation Pattern
```jsonnet
{
  fullUrl: 'urn:uuid:obs-' + obsType + '-' + tei.trackedEntity,
  resource: {
    resourceType: 'Observation',
    status: 'final',
    code: { coding: [{ system: 'http://loinc.org', code: '45404-1' }] },
    subject: { reference: 'urn:uuid:' + tei.trackedEntity },
    valueString: value
  },
  request: { method: 'POST', url: 'Observation' }
}
```

### AllergyIntolerance Pattern
```jsonnet
[{
  fullUrl: 'urn:uuid:allergy-' + tei.trackedEntity + '-' + std.md5(allergy),
  resource: {
    resourceType: 'AllergyIntolerance',
    clinicalStatus: { coding: [{ code: 'active', ... }] },
    patient: { reference: 'urn:uuid:' + tei.trackedEntity },
    reaction: [{ manifestation: [{ text: allergy }] }]
  },
  request: { method: 'POST', url: 'AllergyIntolerance' }
} for allergy in allergiesList if std.length(std.stripChars(allergy, ' ')) > 0]
```

### Bundle with Flatten
```jsonnet
{
  resourceType: 'Bundle',
  type: 'transaction',
  entry: std.prune(std.flattenArrays([
    [patient.patient_entry(body)],
    [practitioner.practitioner_entry(body)],
    [observations.civil_status_observation(body)],
    observations.allergies_resources(body)  // Returns array
  ]))
}
```

## Common Issues & Fixes

| Issue | Fix |
|-------|-----|
| "Cannot find symbol" | `mvn clean compile test-compile` |
| Transformation returns null | Check attribute ID, verify `helpers.getAttrValue()` |
| Nested arrays in bundle | Use `std.flattenArrays()` |
| Reference not found | Ensure `fullUrl` matches reference exactly |
| Validation fails | Add `meta.profile`, check required fields |

## Code Systems

For `Observation` FHIR resources, you often need to refer to certain terminologies and code systems. Here is a list of the most common ones, and the ones used in this exercise. Some UID definitions are also included:

```javascript
  LOINC: 'http://loinc.org',
  OBS_CATEGORY: 'http://terminology.hl7.org/CodeSystem/observation-category',
  ALLERGY_CLINICAL: 'http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical',
  ALLERGY_VERIFICATION: 'http://terminology.hl7.org/CodeSystem/allergyintolerance-verification',
  DHIS2_USER: 'urn:dhis2:user:uid',
  DHIS2_PATIENT: 'urn:dhis2:rmncah:patient-id'
```

## LOINC Codes

- `45404-1` - Marital status (for civil status)
- Category: `social-history`

## Jsonnet Utilities

```jsonnet
std.length(arr)                    // Array/string length
std.split(str, sep)                // Split string
std.stripChars(str, chars)         // Remove characters
std.prune(obj)                     // Remove nulls/empty
std.flattenArrays(arr)             // Flatten nested arrays
[expr for x in arr if condition]  // List comprehension
```