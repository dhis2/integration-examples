// Helper Functions Library for DHIS2 to FHIR IPS Mapping
// This library provides reusable utilities for extracting and transforming
// DHIS2 Tracked Entity data into FHIR-compliant structures.

{
  // ============================================================================
  // ATTRIBUTE LOOKUP
  // ============================================================================
  
  /**
   * Get attribute value from TEI
   * 
   * @param tei: The tracked entity instance object
   * @param attrId: The DHIS2 attribute UID to look for
   * @returns: The attribute value or null if not found
   * 
   * Rationale: Enrollment attributes are program-specific and take precedence
   * over TEI-level attributes. This ensures we get the most contextual data.
   */
  getAttrValue(tei, attrId)::
    local attributes = if std.length(tei.enrollments default []) > 0 
      then tei.enrollments[0].attributes 
      else [];
    
    local attributeMatch = std.filter(
      function(attr) attr.attribute == attrId,
      attributes
    );
    if std.length(attributeMatch) > 0 then
      attributeMatch[0].value
    else
        null,
  
  /**
   * Get multiple attribute values at once
   * 
   * @param tei: The tracked entity instance object
   * @param attrIds: Array of DHIS2 attribute UIDs
   * @returns: Object with attrId as key and value as value
   * 
   * Usage: getAttrValues(tei, ['attr1', 'attr2']) => { attr1: 'value1', attr2: null }
   */
  getAttrValues(tei, attrIds)::
    {
      [attrId]: $.getAttrValue(tei, attrId)
      for attrId in attrIds
    },
  
  // ============================================================================
  // NAME PARSING AND TRANSFORMATION
  // ============================================================================
  
  /**
   * Parse first and last name into FHIR HumanName structure
   * 
   * @param firstName: Given name (can be null)
   * @param lastName: Family name (can be null)
   * @returns: FHIR HumanName object or null if both are empty
   * 
   * IPS Constraint: ips-pat-1 requires at least one of given, family, or text
   * This function ensures compliance by including 'text' field.
   */
  parseName(firstName, lastName)::
    local hasFirst = firstName != null && std.length(std.toString(firstName)) > 0;
    local hasLast = lastName != null && std.length(std.toString(lastName)) > 0;
    
    if !hasFirst && !hasLast then
      // No name data available
      null
    else
      {
        // Always include text to satisfy ips-pat-1 constraint
        [if hasFirst || hasLast then 'text']: 
          std.join(' ', std.prune([firstName, lastName])),
        // Include structured parts when available
        [if hasFirst then 'given']: [firstName],
        [if hasLast then 'family']: lastName,
      },
  
  /**
   * Parse full name string into structured components
   * Useful when DHIS2 stores full name in a single field
   * 
   * @param fullName: Complete name as single string "FirstName LastName"
   * @returns: FHIR HumanName object with given and family parsed
   * 
   * Limitation: Assumes Western name order (given names first, family name last)
   */
  parseFullName(fullName)::
    if fullName == null || std.length(std.toString(fullName)) == 0 then
      null
    else
      local parts = std.split(fullName, ' ');
      local names = [p for p in parts if std.length(p) > 0];
      local n = std.length(names);
      
      if n == 0 then
        null
      else if n == 1 then
        {
          text: fullName,
          family: names[0],
        }
      else
        {
          text: fullName,
          given: [names[i] for i in std.range(0, n - 2)],
          family: names[n - 1],
        },
  
  // ============================================================================
  // ADDRESS PARSING AND TRANSFORMATION
  // ============================================================================
  
  /**
   * Build FHIR Address from separate components
   * 
   * @param line: Street address (single line or null)
   * @param city: City name
   * @param postalCode: Postal/ZIP code
   * @param country: Country code (ISO 3166-1 alpha-2 recommended)
   * @param state: State/province (optional)
   * @param district: District/county (optional)
   * @returns: FHIR Address object or null if all components are empty
   * 
   * Note: Returns null instead of empty object to allow std.prune() to remove it
   */
  parseAddress(line, city, postalCode, country, state=null, district=null)::
    local hasLine = line != null && std.length(std.toString(line)) > 0;
    local hasCity = city != null && std.length(std.toString(city)) > 0;
    local hasPostal = postalCode != null && std.length(std.toString(postalCode)) > 0;
    local hasCountry = country != null && std.length(std.toString(country)) > 0;
    local hasState = state != null && std.length(std.toString(state)) > 0;
    local hasDistrict = district != null && std.length(std.toString(district)) > 0;
    
    if !hasLine && !hasCity && !hasPostal && !hasCountry && !hasState && !hasDistrict then
      null
    else
      {
        [if hasLine then 'line']: [line],
        [if hasCity then 'city']: city,
        [if hasDistrict then 'district']: district,
        [if hasState then 'state']: state,
        [if hasPostal then 'postalCode']: postalCode,
        [if hasCountry then 'country']: country,
      },
  
  /**
   * Parse comma-separated address string into structured components
   * Expected format: "Street, PostalCode City, District, State, Country"
   * 
   * @param address: Full address as comma-separated string
   * @returns: FHIR Address object
   * 
   * Example: "123 Main St, 10001 New York, Manhattan, NY, USA"
   */
  parseAddressString(address)::
    if address == null || std.length(std.toString(address)) == 0 then
      null
    else
      local parts = std.map(function(s) std.stripChars(s, ' \t'), std.split(address, ','));
      {
        [if std.length(parts) > 0 then 'line']: [parts[0]],
        [if std.length(parts) > 1 then 'postalCode']: std.split(parts[1], ' ')[0],
        [if std.length(parts) > 1 && std.length(std.split(parts[1], ' ')) > 1 then 'city']: 
          std.join(' ', std.split(parts[1], ' ')[1:]),
        [if std.length(parts) > 2 then 'district']: parts[2],
        [if std.length(parts) > 3 then 'state']: parts[3],
        [if std.length(parts) > 4 then 'country']: parts[4],
      },
  
  // ============================================================================
  // TELECOM (CONTACT POINT) FUNCTIONS
  // ============================================================================
  
  /**
   * Build FHIR ContactPoint (telecom entry)
   * 
   * @param system: phone | fax | email | pager | url | sms | other
   * @param value: The actual phone number, email address, etc.
   * @param use: home | work | temp | old | mobile (optional)
   * @returns: FHIR ContactPoint object or null if value is empty
   * 
   * Validation: Returns null for empty values to prevent invalid FHIR resources
   */
  buildTelecom(system, value, use=null)::
    if value == null || std.length(std.toString(value)) == 0 then
      null
    else
      {
        system: system,
        value: value,
        [if use != null then 'use']: use,
      },
  
  /**
   * Build array of telecom entries from phone and email
   * Convenience function for common case
   * 
   * @param phone: Phone number
   * @param email: Email address
   * @returns: Array of ContactPoint objects (pruned of nulls)
   */
  buildTelecomArray(phone=null, email=null)::
    std.prune([
      $.buildTelecom('phone', phone, 'mobile'),
      $.buildTelecom('email', email, null),
    ]),
  
  // ============================================================================
  // IDENTIFIER FUNCTIONS
  // ============================================================================
  
  /**
   * Build FHIR Identifier with proper structure
   * 
   * @param system: URI identifying the identifier system
   * @param value: The actual identifier value
   * @param use: official | usual | temp | secondary | old (optional)
   * @param type: CodeableConcept describing identifier type (optional)
   * @returns: FHIR Identifier object or null if value is empty
   * 
   * Best Practice: Always provide system URI for proper identifier matching
   */
  buildIdentifier(system, value, use='official', type=null)::
    if value == null || std.length(std.toString(value)) == 0 then
      null
    else
      {
        use: use,
        [if type != null then 'type']: type,
        system: system,
        value: value,
      },
  
  // ============================================================================
  // DATA TYPE CONVERSION FUNCTIONS
  // ============================================================================
  
  /**
   * Convert DHIS2 gender to FHIR administrative gender
   * 
   * @param dhis2Gender: Gender value from DHIS2 (various formats)
   * @returns: FHIR gender code: male | female | other | unknown
   * 
   * Handles common variations and returns 'unknown' for unmappable values
   */
  convertGender(dhis2Gender)::
    if dhis2Gender == null then
      null
    else
      local normalized = std.asciiLower(std.toString(dhis2Gender));
      if std.startsWith(normalized, 'male') then
        'male'
      else if std.startsWith(normalized, 'female') then
        'female'
      else if normalized == 'm' then
        'male'
      else if normalized == 'f' then
        'female'
      else if std.startsWith(normalized, 'other') then
        'other'
      else
        'unknown',
  
  /**
   * Validate and format date string to FHIR date format (YYYY-MM-DD)
   * 
   * @param dateStr: Date string in various formats
   * @returns: ISO 8601 date string or null if invalid
   * 
   * Note: Currently passes through assuming DHIS2 provides ISO format
   * Could be extended to handle format conversion
   */
  formatDate(dateStr)::
    if dateStr == null || std.length(std.toString(dateStr)) == 0 then
      null
    else
      // DHIS2 typically provides ISO 8601 dates
      // Extract just the date part (YYYY-MM-DD)
      std.substr(dateStr, 0, 10),
  
  // ============================================================================
  // UTILITY FUNCTIONS
  // ============================================================================
  
  /**
   * Check if a value is effectively empty (null, empty string, empty array, empty object)
   * 
   * @param value: Any value to check
   * @returns: true if value is empty, false otherwise
   */
  isEmpty(value)::
    value == null 
    || (std.isString(value) && std.length(value) == 0)
    || (std.isArray(value) && std.length(value) == 0)
    || (std.isObject(value) && std.length(std.objectFields(value)) == 0),
  
  /**
   * Safe string conversion that handles null
   * 
   * @param value: Value to convert to string
   * @returns: String representation or empty string if null
   */
  safeString(value)::
    if value == null then '' else std.toString(value),
  
  /**
   * Get first non-null value from array of options
   * Useful for fallback chains
   * 
   * @param options: Array of potential values
   * @returns: First non-null value or null if all are null
   * 
   * Usage: coalesce([null, '', 'value', 'other']) => ''
   */
  coalesce(options)::
    local nonNull = std.filter(function(v) v != null, options);
    if std.length(nonNull) > 0 then nonNull[0] else null,
}
