// TODO: Observation and AllergyIntolerance Resources
// Your task: Map DHIS2 attributes (civil status, allergies) that don't fit IPS Patient profile

// If you get stuck, check solutions/observationResources.solution.libsonnet

// CIVIL STATUS CHECKLIST:
// [ ] Extract civil status with helpers.getAttrValue
// [ ] Return null if no data
// [ ] Set resourceType='Observation'
// [ ] Add category with social-history code
// [ ] Add LOINC code 45404-1 (Marital status)
// [ ] Add subject reference to Patient
// [ ] Add valueString with civil status value
// [ ] Add PUT request

// ALLERGIES CHECKLIST:
// [ ] Extract allergies value
// [ ] Split by comma into array
// [ ] Use list comprehension to create multiple resources
// [ ] Set resourceType='AllergyIntolerance'
// [ ] Add clinicalStatus='active'
// [ ] Add verificationStatus='unconfirmed'
// [ ] Add patient reference
// [ ] Add reaction manifestation with allergy text
// [ ] Filter empty values
// [ ] Add PUT request for each

// INTEGRATION CHECKLIST:
// [ ] Update fhirBundle.ds to import this file
// [ ] Add civil_status_observation to bundle entry array
// [ ] Add allergies_resources to bundle entry array
// [ ] Use std.flattenArrays since allergies_resources returns array
// [ ] Run: mvn test -Dtest=IpsPatientMappingTestCase#testCivilStatusObservation
// [ ] Run: mvn test -Dtest=IpsPatientMappingTestCase#testAllergiesResource

local helpers = import 'helperFunctions.libsonnet';

{
  // TODO 1: Define attribute ID constants
  // Hint: Find these IDs in trackedEntity.json
  
  // local ATTR_CIVIL_STATUS = ...,
  // local ATTR_ALLERGIES = ..., 

  /**
   * Creates an Observation resource for civil status
   * 
   * Why Observation? Civil status doesn't fit in IPS Patient profile,
   * but we don't want to lose this data (lossless mapping).
   * 
   * @param tei - DHIS2 Tracked Entity Instance
   * @return Bundle entry with Observation or null if no data
   */
  civil_status_observation(tei)::
    // TODO 2: Extract civil status value using helpers.getAttrValue
    // local civilStatus = helpers. ....
    
    // TODO 3: Return null if no data (will be removed by std.prune)
    // if civilStatus == null then null else
    
    // TODO 4: Create unique identifier (observationId) using attributeId + teiId
    // Hint: local observationId = ATTR_CIVIL_STATUS + '-' + tei.trackedEntity
    // local observationId = ...
    
    {
      // TODO 5: Create unique fullUrl using observationId
      // fullUrl: ...
      
      resource: std.prune({
        // TODO 6: Set resourceType to 'Observation'
        // resourceType: ...
        
        // TODO 7: Set status to 'final'
        // status: ...
        
        // TODO 8: Add identifier for conditional update
        // Hint: system='urn:dhis2:observation:attribute', value=observationId
        // identifier: [...]
        
        // TODO 9: Add category array with social-history coding
        // Hint: system='http://terminology.hl7.org/CodeSystem/observation-category'
        //       code='social-history', display='Social History'
        // category: [...]
        
        // TODO 10: Add code with LOINC coding
        // Hint: system='http://loinc.org', code='45404-1', display='Marital status'
        //       text='Civil Status'
        // code: { ... }
        
        // TODO 11: Add subject reference to Patient
        // Hint: reference='urn:uuid:' + tei.trackedEntity, type='Patient'
        // subject: { ... }
        
        // TODO 12: Add valueString with the civilStatus value
        // valueString: ...
      }),
      
      // TODO 13: Add PUT request with conditional update using identifier
      // Hint: method='PUT', url='Observation?identifier=urn:dhis2:observation:attribute|' + observationId
      // request: { ... }
    },

  /**
   * ADVANCED 
   * Creates AllergyIntolerance resources for allergies
   * 
   * DHIS2 stores allergies as MULTI_TEXT (comma-separated values).
   * We create one AllergyIntolerance resource per allergy.
   * 
   * @param tei - DHIS2 Tracked Entity Instance
   * @return Array of bundle entries (one per allergy)
   */
  allergies_resources(tei)::
    // TODO 14: Extract allergies value
    // local allergiesValue = ...
    
    // TODO 15: Split multi-text value by comma
    // Hint: Use std.split(allergiesValue, ',') or return empty array if null
    // local allergiesList = ...
    
    // TODO 16: Create array of AllergyIntolerance resources using list comprehension
    // Hint: [{ resource } for allergy in allergiesList if condition]
    [
      // TODO 17: Strip whitespace and create unique identifier
      // Hint: local allergyText = std.stripChars(allergy, ' ');
      //       local allergyId = ATTR_ALLERGIES + '-' + tei.trackedEntity + '-' + allergyText;
      // local allergyText = ...
      // local allergyId = ...
      
      {
        // TODO 18: Create unique fullUrl using allergyId
        // fullUrl: ...
        
        resource: std.prune({
          // TODO 19: Set resourceType to 'AllergyIntolerance'
          // resourceType: ...
          
          // TODO 20: Add identifier for conditional update
          // Hint: system='urn:dhis2:allergyintolerance:attribute', value=allergyId
          // identifier: [...]
          
          // TODO 21: Add clinicalStatus with 'active' code
          // Hint: system='http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical'
          // clinicalStatus: { ... }
          
          // TODO 22: Add verificationStatus with 'unconfirmed' code
          // Hint: system='http://terminology.hl7.org/CodeSystem/allergyintolerance-verification'
          // verificationStatus: { ... }
          
          // TODO 23: Add patient reference
          // Hint: Same pattern as Observation subject
          // patient: { ... }
          
          // TODO 24: Add reaction array with manifestation
          // Hint: Use allergyText variable (already stripped)
          // reaction: [...]
        }),
        
        // TODO 25: Add PUT request with conditional update using identifier
        // Hint: method='PUT', url='AllergyIntolerance?identifier=urn:dhis2:allergyintolerance:attribute|' + allergyId
        // request: { ... }
      }
      // TODO 26: Add list comprehension filter
      // Hint: for allergy in allergiesList if std.length(std.stripChars(allergy, ' ')) > 0
      // for allergy in ... if ...
    ],
}
