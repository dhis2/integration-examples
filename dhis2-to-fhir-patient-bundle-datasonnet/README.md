# DHIS2 to FHIR IPS Patient Mapping

This example shows how to transform DHIS2 tracked entities into FHIR IPS (International Patient Summary) Patient profiles. It's a practical reference for integrating DHIS2 maternal health data with FHIR-based health information exchanges.

## What is this?

When DHIS2 tracks patients in programs like WHO RMNCAH (maternal health), you often need to share that data with other systems using FHIR. This example demonstrates a lossless mapping approach where:

- Patient demographics map to FHIR Patient resources
- Additional attributes that don't fit in the IPS Patient Profile (like civil status) become Observation resources
- Clinical data (like allergies) uses specialized resources (AllergyIntolerance)
- Everything is bundled together and pushed to a FHIR server with conditional updates

The mapping uses DataSonnet with Jsonnet libraries, using helper functions when applicable to help streamline the process. 

## Quick Start

**Prerequisites:**
- Java 11+
- Maven 3.6+
- Docker

After installing the prerequisites, you can run the entire stack by simply running the following commands:

```bash
yarn install --frozen-lockfile
yarn build
yarn start
```
This will spin up a DHIS2 instance with the Sierra Leone demo DB on `localhost:8080`, a HAPI FHIR server with the IPS Profiles loaded on `localhost:8081/fhir`, and a middleware solution that listens to Tracked Entity changes in the DHIS2 instance. 

To run the transformation tests, run the following command:

```bash
cd fhir-sync-agent
mvn clean test -Dtest=IpsPatientMappingTestCase
```

## Architecture

When someone enrolls in the WHO RMNCAH program in DHIS2, the sync agent picks up that event, transforms the tracked entity data into FHIR resources that are conformant to the IPS Patient Profile, and pushes them to the HAPI FHIR server.

**Key files:**

- `fhirBundle.ds` - Main transformation entry point
- `helperFunctions.libsonnet` - Utilities for extracting DHIS2 attributes and building FHIR data types
- `patientResource.libsonnet` - Builds the IPS Patient resource
- EXERCISE: `practitionerResource.libsonnet` - Maps the DHIS2 user who created the record
- EXERCISE: `observationResources.libsonnet` - Handles attributes that don't fit in Patient (civil status, allergies)

## How It Works

### Source: DHIS2 Tracked Entity

DHIS2 stores patient data as tracked entities with attributes at both the entity level and enrollment level:

```json
{
  "trackedEntity": "QfFVkOL8ixj",
  "attributes": [
    {"attribute": "w75KJ2mc4zz", "value": "Jane"},
    {"attribute": "zDhUuAYrxNC", "value": "Doe"}
  ],
  "enrollments": [{
    "attributes": [
      {"attribute": "gHGyrwKPzej", "value": "1997-04-18"},
      {"attribute": "ciq2USN94oJ", "value": "Single or widow"},
      {"attribute": "gu1fqsmoU8r", "value": "NSAIDS, Penicillin"}
    ]
  }]
}
```

### Target: FHIR Bundle

The transformation produces a transaction bundle with multiple resources:

```json
{
  "resourceType": "Bundle",
  "type": "transaction",
  "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "identifier": [{"value": "8437107", "system": "urn:dhis2:rmncah:patient-id"}],
        "name": [{"given": ["Jane"], "family": "Doe"}],
        "birthDate": "1997-04-18"
      },
      "request": {
        "method": "PUT",
        "url": "Patient?identifier=urn:dhis2:rmncah:patient-id|8437107"
      }
    },
    {
      "resource": {
        "resourceType": "Observation",
        "code": {"coding": [{"system": "http://loinc.org", "code": "45404-1"}]},
        "valueString": "Single or widow"
      },
      "request": {"method": "PUT", "url": "Observation?identifier=..."}
    },
    {
      "resource": {
        "resourceType": "AllergyIntolerance",
        "reaction": [{"manifestation": [{"text": "NSAIDS"}]}]
      },
      "request": {"method": "PUT", "url": "AllergyIntolerance?identifier=..."}
    }
  ]
}
```

### Why Bundles?

FHIR transaction bundles let you:
- Push multiple resources in a single HTTP request
- Reference resources internally using `fullUrl`
- Use conditional updates (upsert) so repeated pushes don't create duplicates
- Ensure atomic operations (all succeed or all fail)

## Core Concepts

### Helper Functions (`helperFunctions.libsonnet`)

These utilities make the transformation code cleaner:

**`getAttrValue(tei, attributeId)`** - Extracts attribute values with smart fallback. Checks enrollment attributes first (program-specific), then falls back to TEI attributes (general demographic).

**`parseName(firstName, lastName)`** - Combines name parts into a FHIR HumanName structure that satisfies IPS constraints (must have `given`, `family`, or `text`).

**`parseAddress(...)`**, **`buildTelecom(...)`** - Transform DHIS2 address and contact data into FHIR structures.

All helpers handle null values gracefully and use conditional field syntax to avoid empty objects.

### Lossless Mapping

Not everything in DHIS2 fits into a FHIR Patient resource. The IPS Patient profile has specific fields for demographics, but what about:

- Civil status? → Use an Observation with LOINC code 45404-1
- Allergies? → Use AllergyIntolerance resources (one per allergy)
- Who created the record? → Create a Practitioner and link via `generalPractitioner`

This way nothing is lost in the transformation.

### Conditional Updates

The bundle uses `PUT` with identifier queries:

```
PUT Patient?identifier=urn:dhis2:rmncah:patient-id|8437107
```

This means:
- If a patient with that identifier exists it gets update
- If not - create it

This means that if you create a new enrollment in DHIS2 and need to make changes to the attributes, the resulting FHIR patient is just updated instead of duplicated. This is critical for keeping DHIS2 and FHIR in sync.

## Hands-On Exercise

Want to learn by doing? Check out `docs/HANDS_ON_GUIDE.md` for a step-by-step exercise where you'll:

1. Add a Practitioner resource (map DHIS2 user to FHIR Practitioner)
2. Add Observations for civil status
3. Handle multi-value attributes (splitting comma-separated allergies into separate AllergyIntolerance resources)

The exercise uses guided TODO files with hints, and you can check your work against the solution files in `/fhir-sync-agent/src/main/resources/solutions`.

## Key Files Reference

- **`fhir-sync-agent/src/main/resources/fhirBundle.ds`** - Entry point that orchestrates the transformation
- **`fhir-sync-agent/src/main/resources/helperFunctions.libsonnet`** - Reusable utilities (documented inline)
- **`fhir-sync-agent/src/main/resources/patientResource.libsonnet`** - Patient resource builder (see comments for attribute IDs)
- **`fhir-sync-agent/src/main/resources/observationResources.libsonnet`** - Observation and AllergyIntolerance builders
- **`fhir-sync-agent/src/test/java/.../IpsPatientMappingTestCase.java`** - Tests with HAPI FHIR validation

## Adapting This Example

To use this with your own DHIS2 instance:

1. **Identify your attribute IDs and/or data element IDs** - Look up the IDs in your DHIS2 metadata
2. **Update the constants** - Change `ATTR_FIRST_NAME`, `ATTR_DOB`, etc. in the resource libraries
3. **Adjust the identifier system** - Replace `urn:dhis2:rmncah:patient-id` with your system URI
4. **Add/remove resources** - Map additional attributes as needed
5. **Test thoroughly** - Run the validator against your FHIR server's profiles

## Resources

- [FHIR IPS Implementation Guide](http://hl7.org/fhir/uv/ips/)
- [DHIS2 Tracker API](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/tracker.html)
- [DataSonnet Documentation](https://datasonnet.com/)
- [Jsonnet Tutorial](https://jsonnet.org/learning/tutorial.html)

## Support

Questions, issues or difficulties with the exercise? Open an issue on GitHub or reach out on the [DHIS2 Community of Practice](https://community.dhis2.org/).