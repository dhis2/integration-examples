// TODO: Practitioner Resource Mapping
// Your task: Complete this implementation to map DHIS2 user (createdBy) to FHIR Practitioner
// 
// If you get stuck, check solutions/practitionerResource.solution.libsonnet

// CHECKLIST:
// [ ] Extract all fields from tei.createdBy
// [ ] Use helpers.parseName for name transformation
// [ ] Set correct resourceType
// [ ] Add identifier with urn:dhis2:user:uid system
// [ ] Add name array
// [ ] Create conditional PUT request
// [ ] Update patientResource.libsonnet to add generalPractitioner reference
// [ ] Update fhirBundle.ds to import and include this resource
// [ ] Run: mvn test -Dtest=IpsPatientMappingTestCase#testPatientReferencesPractitioner

local helpers = import 'helperFunctions.libsonnet';

{
  /**
   * Creates a FHIR Practitioner resource from DHIS2 TEI createdBy information
   * 
   * @param tei - DHIS2 Tracked Entity Instance object
   * @return Bundle entry with Practitioner resource and conditional PUT request
   */
  practitioner_entry(tei)::
    // TODO 1: Extract practitioner information from tei.createdBy
    // Hint: You need uid, firstName, surname, and username
    // local practitionerId = ...
    // local firstName = ...
    // local lastName = ...
    // local username = ...

    // TODO 2: Use helpers.parseName to create practitionerName
    // Hint: This is the same function used in patientResource.libsonnet
    // local practitionerName = ...

    // TODO 3: Build the bundle entry
    {
      // TODO 3a: Create unique fullUrl
      // Hint: Use pattern 'urn:uuid:practitioner-' + practitionerId
      // fullUrl: ...
      
      resource: std.prune({
        // TODO 3b: Set resourceType
        // resourceType: ...
        
        // TODO 3c: Add identifier array with system and value
        // Hint: system should be 'urn:dhis2:user:uid', you can use helpers.buildIdentifier(...)
        // identifier: [...]
        
        // TODO 3d: Add name array
        // Hint: Wrap practitionerName in array brackets
        // name: ...
        
      }),
      
      // TODO 3e: Add conditional PUT request
      // Hint: method='PUT', url='Practitioner?identifier=urn:dhis2:user:uid|' + practitionerId
      // request: { ... }
    },
}
