// Patient Resource Builder for DHIS2 to FHIR IPS Mapping
// Transforms DHIS2 WHO RMNCAH Tracked Entity to FHIR IPS Patient profile
//
// FHIR Profile: http://hl7.org/fhir/uv/ips/StructureDefinition/Patient-uv-ips
// IPS Constraint: ips-pat-1 - Patient.name.given, Patient.name.family or Patient.name.text SHALL be present

local helpers = import 'helperFunctions.libsonnet';

{
  /**
   * Create a FHIR Bundle entry for a Patient resource conforming to IPS profile
   * 
   * @param tei: DHIS2 Tracked Entity Instance with enrollments and attributes
   * @returns: FHIR Bundle entry with Patient resource and conditional update request
   * 
   * Bundle Entry Structure:
   * - fullUrl: Temporary UUID reference for use within the bundle
   * - resource: The actual Patient resource
   * - request: Conditional PUT to upsert based on identifier
   */
  patient_entry(tei)::
    // ========================================================================
    // STEP 1: Define DHIS2 Attribute IDs
    // ========================================================================
    // These UIDs correspond to WHO RMNCAH metadata structure
    // Mapping based on trackedEntity.json example
    
    local ATTR_UNIQUE_ID = 'lZGmxYbs97q';    // MMD_PER_ID - Unique ID
    local ATTR_FIRST_NAME = 'w75KJ2mc4zz';   // MMD_PER_NAM - First name
    local ATTR_LAST_NAME = 'zDhUuAYrxNC';    // Last name
    local ATTR_DOB = 'gHGyrwKPzej';          // MMD_PER_DOB - Birth date
    local ATTR_ADDRESS = 'VqEFza8wbwA';      // MMD_PER_ADR1 - Address
    local ATTR_CITY = 'FO4sWYJ64LQ';         // City
    local ATTR_POSTAL = 'ZcBPrXKahq2';       // Postal code
    local ATTR_MOBILE = 'Agywv2JGwuq';       // MMD_PER_MOB - Mobile number
    local ATTR_EMAIL = 'KmEUg2hHEtx';        // Email address
    local ATTR_CIVIL_STATUS = 'ciq2USN94oJ'; // MMD_PER_STA - Civil status
    
    // ========================================================================
    // STEP 2: Extract Attribute Values
    // ========================================================================
    
    local uniqueId = helpers.getAttrValue(tei, ATTR_UNIQUE_ID);
    local firstName = helpers.getAttrValue(tei, ATTR_FIRST_NAME);
    local lastName = helpers.getAttrValue(tei, ATTR_LAST_NAME);
    local birthDate = helpers.getAttrValue(tei, ATTR_DOB);
    local addressLine = helpers.getAttrValue(tei, ATTR_ADDRESS);
    local city = helpers.getAttrValue(tei, ATTR_CITY);
    local postalCode = helpers.getAttrValue(tei, ATTR_POSTAL);
    local mobile = helpers.getAttrValue(tei, ATTR_MOBILE);
    local email = helpers.getAttrValue(tei, ATTR_EMAIL);
    
    // ========================================================================
    // STEP 3: Transform Complex Data Types
    // ========================================================================
    
    // 3.1 Parse name into FHIR HumanName structure
    // IPS requires at least one of: given, family, or text
    local patientName = helpers.parseName(firstName, lastName);
    
    // 3.2 Build address from separate components
    // Returns null if all components are empty (will be pruned)
    local patientAddress = helpers.parseAddress(
      line=addressLine,
      city=city,
      postalCode=postalCode,
      country=null  // Not provided in this example
    );
    
    // 3.3 Build telecom (contact points) array
    // Prunes null entries automatically
    local telecomItems = std.prune([
      helpers.buildTelecom('phone', mobile, 'mobile'),
      helpers.buildTelecom('email', email, null)
    ]);
    
    // 3.4 Format birth date to FHIR format (YYYY-MM-DD)
    local formattedBirthDate = helpers.formatDate(birthDate);
    
    // ========================================================================
    // STEP 4: Build Identifier Array
    // ========================================================================
    // IPS recommends including patient identifiers for matching
    // Using conditional array construction to include only when value exists
    
    local identifiers = std.prune([
      // Primary identifier - official use
      if uniqueId != null then helpers.buildIdentifier(
        system='urn:dhis2:rmncah:patient-id',
        value=uniqueId,
        use='official'
      )
    ]);
    
    // ========================================================================
    // STEP 5: Construct FHIR Patient Resource
    // ========================================================================
    // Using std.prune() to remove null/empty fields for clean FHIR output
    
    {
      // Bundle entry metadata
      fullUrl: 'urn:uuid:' + tei.trackedEntity,
      
      // The actual Patient resource
      resource: std.prune({
        resourceType: 'Patient',
        
        // IPS Profile declaration - REQUIRED for IPS conformance
        meta: {
          profile: ['http://hl7.org/fhir/uv/ips/StructureDefinition/Patient-uv-ips']
        },
        
        // Identifiers - recommended for patient matching
        identifier: identifiers,
        
        // Name - REQUIRED by IPS (min cardinality 1)
        // ips-pat-1 constraint: Must have given, family, or text
        name: if patientName != null then [patientName] else [],
        
        // Telecom - contact information
        // Include if any contact methods are available
        [if std.length(telecomItems) > 0 then 'telecom']: telecomItems,
        
        // Birth date - important demographic information
        [if formattedBirthDate != null then 'birthDate']: formattedBirthDate,
        
        // Address - physical address information
        [if patientAddress != null then 'address']: [patientAddress],
      }),
      
      // ========================================================================
      // STEP 6: Define Bundle Request (Conditional Update)
      // ========================================================================
      // Using conditional PUT for idempotent upsert behavior:
      // - If patient with this identifier exists: UPDATE
      // - If patient doesn't exist: CREATE
      // This prevents duplicate patient records
      
      request: {
        method: 'PUT',
        url: 'Patient?identifier=urn:dhis2:rmncah:patient-id|' + uniqueId,
      },
    },
  
  /**
   * Create a minimal Patient entry with only required fields
   * Useful for testing minimum IPS compliance
   * 
   * @param tei: DHIS2 Tracked Entity Instance
   * @returns: FHIR Bundle entry with minimal Patient resource
   */
  minimal_patient_entry(tei)::
    local ATTR_UNIQUE_ID = 'lZGmxYbs97q';
    local ATTR_FIRST_NAME = 'w75KJ2mc4zz';
    local ATTR_LAST_NAME = 'zDhUuAYrxNC';
    local ATTR_DOB = 'gHGyrwKPzej';
    
    local uniqueId = helpers.getAttrValue(tei, ATTR_UNIQUE_ID);
    local firstName = helpers.getAttrValue(tei, ATTR_FIRST_NAME);
    local lastName = helpers.getAttrValue(tei, ATTR_LAST_NAME);
    local dob = helpers.getAttrValue(tei, ATTR_DOB);

    local patientName = helpers.parseName(firstName, lastName);
    
    {
      fullUrl: 'urn:uuid:' + tei.trackedEntity,
      resource: {
        resourceType: 'Patient',
        meta: {
          profile: ['http://hl7.org/fhir/uv/ips/StructureDefinition/Patient-uv-ips']
        },
        
        name: if patientName != null then [patientName] else [{ text: 'Unknown' }],
        dateOfBirth: if dob != null then helpers.formatDate(dob) else null,
      },
      request: {
        method: 'POST',
        url: 'Patient',
      },
    },
}
