// SOLUTION: Observation and AllergyIntolerance Resources
// Maps DHIS2 attributes that don't fit IPS Patient profile

local helpers = import '../helperFunctions.libsonnet';

{
  // DHIS2 Attribute ID Constants
  local ATTR_CIVIL_STATUS = 'ciq2USN94oJ',  // Civil status (e.g., "Single or widow")
  local ATTR_ALLERGIES = 'gu1fqsmoU8r',     // Allergies (MULTI_TEXT type)

  /**
   * Creates an Observation resource for civil status
   * 
   * Civil status doesn't map to IPS Patient profile, so we use Observation
   * to map with proper LOINC coding.
   * 
   * @param tei - DHIS2 Tracked Entity Instance
   * @return Bundle entry with Observation or null if no data
   */
  civil_status_observation(tei)::
    local civilStatus = helpers.getAttrValue(tei, ATTR_CIVIL_STATUS);
    
    // Only create observation if data exists
    if civilStatus == null then null else
    
    // Unique identifier: attributeId + teiId
    local observationId = ATTR_CIVIL_STATUS + '-' + tei.trackedEntity;
    
    {
      fullUrl: 'urn:uuid:' + observationId,
      
      resource: std.prune({
        resourceType: 'Observation',
        status: 'final',
        
        // Identifier for conditional update (attributeId + teiId)
        identifier: [{
          system: 'urn:dhis2:observation:attribute',
          value: observationId
        }],
        
        // Category: social-history (demographic/social information)
        category: [{
          coding: [{
            system: 'http://terminology.hl7.org/CodeSystem/observation-category',
            code: 'social-history',
            display: 'Social History'
          }]
        }],
        
        // Code: LOINC 45404-1 = Marital status
        code: {
          coding: [{
            system: 'http://loinc.org',
            code: '45404-1',
            display: 'Marital status'
          }],
          text: 'Civil Status'
        },
        
        // Subject: reference to Patient
        subject: {
          reference: 'urn:uuid:' + tei.trackedEntity,
          type: 'Patient'
        },
        
        // Value: the actual civil status text
        valueString: civilStatus,
      }),
      
      // PUT with identifier query: updates if exists, creates if not
      request: {
        method: 'PUT',
        url: 'Observation?identifier=urn:dhis2:observation:attribute|' + observationId
      }
    },

  /**
   * Creates AllergyIntolerance resources for allergies
   * 
   * DHIS2 stores allergies as MULTI_TEXT (comma-separated). We create
   * a separate AllergyIntolerance resource for each allergy.
   * 
   * @param tei - DHIS2 Tracked Entity Instance
   * @return Array of bundle entries (one per allergy)
   */
  allergies_resources(tei)::
    local allergiesValue = helpers.getAttrValue(tei, ATTR_ALLERGIES);
    
    // Parse multi-text value (split by comma)
    local allergiesList = if allergiesValue == null then [] 
                          else std.split(allergiesValue, ',');
    
    // Create AllergyIntolerance for each allergy
    [
      // Unique identifier: attributeId + teiId + stripped allergy text
      local allergyText = std.stripChars(allergy, ' ');
      local allergyId = ATTR_ALLERGIES + '-' + tei.trackedEntity + '-' + allergyText;
      
      {
        fullUrl: 'urn:uuid:' + allergyId,
        
        resource: std.prune({
          resourceType: 'AllergyIntolerance',
          
          // Identifier for conditional update (attributeId + teiId + allergyHash)
          identifier: [{
            system: 'urn:dhis2:allergyintolerance:attribute',
            value: allergyId
          }],
          
          // Clinical status: active (currently relevant)
          clinicalStatus: {
            coding: [{
              system: 'http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical',
              code: 'active',
              display: 'Active'
            }]
          },
          
          // Verification status: unconfirmed (not clinically verified)
          verificationStatus: {
            coding: [{
              system: 'http://terminology.hl7.org/CodeSystem/allergyintolerance-verification',
              code: 'unconfirmed',
              display: 'Unconfirmed'
            }]
          },
          
          // Patient reference
          patient: {
            reference: 'urn:uuid:' + tei.trackedEntity,
            type: 'Patient'
          },
          
          // Reaction with manifestation (the allergy substance/effect)
          reaction: [{
            manifestation: [{
              text: allergyText
            }]
          }],
        }),
        
        // PUT with identifier query: updates if exists, creates if not
        request: {
          method: 'PUT',
          url: 'AllergyIntolerance?identifier=urn:dhis2:allergyintolerance:attribute|' + allergyId
        }
      }
      // List comprehension: create one resource per allergy
      for allergy in allergiesList 
      // Filter: skip empty strings
      if std.length(std.stripChars(allergy, ' ')) > 0
    ],
}
