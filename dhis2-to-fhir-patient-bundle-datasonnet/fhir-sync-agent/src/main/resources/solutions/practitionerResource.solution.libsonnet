// SOLUTION: Practitioner Resource Mapping
// This file contains the complete implementation for mapping DHIS2 createdBy user to FHIR Practitioner

local helpers = import '../helperFunctions.libsonnet';

{
  /**
   * Creates a FHIR Practitioner resource from DHIS2 TEI createdBy information
   * 
   * @param tei - DHIS2 Tracked Entity Instance object
   * @return Bundle entry with Practitioner resource and conditional PUT request
   */
  practitioner_entry(tei)::
    // Extract practitioner information from createdBy
    local practitionerId = tei.createdBy.uid;
    local firstName = tei.createdBy.firstName;
    local lastName = tei.createdBy.surname;
    local username = tei.createdBy.username;

    // Transform name using helper function (same as Patient)
    local practitionerName = helpers.parseName(firstName, lastName);

    // Build the Practitioner resource
    {
      // Use unique fullUrl for internal references
      fullUrl: 'urn:uuid:practitioner-' + practitionerId,
      
      resource: std.prune({
        resourceType: 'Practitioner',
        
        // Official identifier from DHIS2 user UID
        identifier: [{
          use: 'official',
          system: 'urn:dhis2:user:uid',
          value: practitionerId
        }],
        
        // Name from user profile
        name: [practitionerName],
        
      }),
      
      // Conditional PUT: update if exists, create if not (upsert pattern)
      request: {
        method: 'PUT',
        url: 'Practitioner?identifier=urn:dhis2:user:uid|' + practitionerId
      }
    },
}
