{
  patient_entry(ds, tei)::
    local ATTR_FULLNAME = "VQl0wK3eqiw";
    local ATTR_PHONE    = "gGAQeOr1Pgu";
    local ATTR_DOB      = "Yie7mOY913J";
    local ATTR_GENDER   = "p7zizFkC6Lv";
    local ATTR_REGNO    = "CSZevH4P5yV";
    local ATTR_NIC      = "M6NNPC3hNrb";
    local ATTR_ADDRESS  = "EOMGwaUTMrU";
    local ATTR_PHN      = "IrUmPkFMDU5";
    local ATTR_SUBJECT_ID = "CJLJmmp8r9g";

    local getAttrById(attr) = ds.filter(tei.enrollments[0].attributes, function(v, i) v.attribute == attr)[0].value default null;

    local parseFullName() =
      local fullNameAttr = getAttrById(ATTR_FULLNAME);
      if fullNameAttr == null then []
      else
        local parts = std.split(fullNameAttr, " ");
        local names = [p for p in parts if p != ""];
        local n = std.length(names);
        if n == 0 then []
        else [
          {
            text: fullNameAttr,
            given: if n > 1 then [ names[i] for i in std.range(0, n - 2) ] else [],
            family: names[n - 1],
          }
        ];

    local parseAddress(address) =
      local addressAttr = std.map(ds.trim, std.split(address, ",")) default null;
      local postalCity  = std.map(ds.trim, std.split(addressAttr[1], " ")) default null;
      {
        [if addressAttr != null then "line"]: [ addressAttr[0] ],
        [if postalCity != null then "postalCode"]: postalCity[0],
        [if postalCity != null && std.length(postalCity) > 1 then "city"]: postalCity[1],
        [if addressAttr != null && std.length(addressAttr) > 2 then "district"]: addressAttr[2],
        [if addressAttr != null && std.length(addressAttr) > 3 then "state"]: addressAttr[3],
        [if addressAttr != null then "country"]: if std.length(addressAttr) > 4 then addressAttr[4] else "LK",
      };

    {
      fullUrl: "urn:uuid:" + (tei.trackedEntity),
      resource: std.prune({
        resourceType: "Patient",
        meta: [
            {
                profile: "http://fhir.health.gov.lk/ips/StructureDefinition/ips-patient",
            }
        ],
        extension: [
          {
            url: "http://fhir.health.gov.lk/ips/StructureDefinition/patient-registration-system",
            valueReference: {
              reference: "Device?identifier=http://fhir.health.gov.lk/ips/identifier/system-id|5b21b377-f424-48c1-8c24-1980b4d00059",
            },
          }
        ],
        identifier: std.prune([
          {
            use: "official",
            type: {
              coding: [
                {
                  system: "http://fhir.health.gov.lk/ips/CodeSystem/cs-identifier-types",
                  code: "PHN",
                  display: "Personal Health Number",
                }
              ],
              text: "Personal Health Number",
            },
            system: "http://fhir.health.gov.lk/ips/identifier/phn",
            value: getAttrById(ATTR_PHN),
          },
          if getAttrById(ATTR_SUBJECT_ID) != null then {
            use: "secondary",
            system: "urn:esignet:sub",
            value: getAttrById(ATTR_SUBJECT_ID),
          },
          if getAttrById(ATTR_NIC) != null then {
            use: "secondary",
            type: {
              coding: [
                {
                  system: "http://fhir.health.gov.lk/ips/CodeSystem/cs-identifier-types",
                  code: "NIC",
                  display: "National Identity Card",
                }
              ],
              text: "National identity number",
            },
            system: "http://fhir.health.gov.lk/ips/identifier/nic",
            value: getAttrById(ATTR_NIC),
          },
          if getAttrById(ATTR_REGNO) != null then {
            use: "secondary",
            system: "urn:dhis2:anc:regno",
            value: getAttrById(ATTR_REGNO),
          }
        ]),
        name: parseFullName(),
        telecom: std.prune([
          if getAttrById(ATTR_PHONE) != null then {
            system: "phone",
            value: getAttrById(ATTR_PHONE),
          }
        ]),
        gender: if getAttrById(ATTR_GENDER) != null then ds.lower(getAttrById(ATTR_GENDER)) else null,
        birthDate: getAttrById(ATTR_DOB),
        address: std.prune([
          if getAttrById(ATTR_ADDRESS) != null then parseAddress(getAttrById(ATTR_ADDRESS))
        ]),
      }),
      request: {
        method: "PUT",
        url: "Patient?identifier=http://fhir.health.gov.lk/ips/identifier/phn|" + (getAttrById(ATTR_PHN)),
      },
    },
}
