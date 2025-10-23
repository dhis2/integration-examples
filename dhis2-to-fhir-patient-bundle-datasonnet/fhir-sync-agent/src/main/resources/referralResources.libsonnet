{
    resources_for_referral_event(ds, phnValue, event) ::
    local REFERRAL_ID = "aQYZkIhWzeJ";

    if (event.programStage default null) != REFERRAL_ID then [] else

    local subjectReference = "Patient?identifier=http://fhir.health.gov.lk/ips/identifier/phn|" + (phnValue default "");
    local eventId = event.event default null;

    local encounterReference = "Encounter?identifier=urn:dhis2:eventId|" + eventId;
    local effectiveDateTime = (event.completedAt default event.occurredAt);
    local dataValues = event.dataValues default [];
    local getDeById(id) = ds.filter(dataValues, function(v, i) v.dataElement == id)[0].value default null;
    local uniqueResourceId(dataElementId) =  eventId + "-" + dataElementId;

    local stringToBool(s) =
        if s == "true" then true
        else if s == "false" then false
        else error "invalid boolean: " + std.manifestJson(s);

    local asInt(value) = if value == null then null else std.parseInt(value);
    local asFloat(value) = if value == null then null else std.parseJson(value);
    local asBoolean(value) = if value == null then null else stringToBool(value);

    ds.filter([
        // Reason for Referral - TEXT
        if getDeById("kLAB3XIylS8") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("BqkEw3MQDNI"),
            resource: {
                resourceType: "Observation",
                status: "final",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("BqkEw3MQDNI")
                } ],
                category: [
                    {
                      coding: [
                        { 
                            system: "http://terminology.hl7.org/CodeSystem/observation-category", 
                            code: "survey", 
                            display: "Survey" 
                        }
                    ] 
                    }
                ],
                code: {
                    coding: [{
                        system: "urn:dhis2:dataElement",
                        code: "716101000000104",
                        display: "Reason for referral (record artifact)"
                    }],
                    text: "Reason for Referral"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference },
                effectiveDateTime: effectiveDateTime,
                valueString: getDeById("BqkEw3MQDNI")
            },
            request: {
                method: "PUT",
                url: "Observation?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("BqkEw3MQDNI")
            }
        }
        else null,

        // Compaints - LONG_TEXT
        if getDeById("ARt4vJRNFmX") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("ARt4vJRNFmX"),
            resource: {
                resourceType: "Observation",
                status: "final",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("ARt4vJRNFmX")
                } ],
                category: [
                    {
                      coding: [
                        { 
                            system: "http://terminology.hl7.org/CodeSystem/observation-category", 
                            code: "survey", 
                            display: "Survey" 
                        }
                    ] 
                    }
                ],
                code: {
                    coding: [{
                        system: "http://snomed.info/sct",
                        code: "886891000000102",
                        display: "Presenting complaints or issues (record artifact)"
                    }],
                    text: "Complaints"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference },
                effectiveDateTime: effectiveDateTime,
                valueString: getDeById("ARt4vJRNFmX")
            },
            request: {
                method: "PUT",
                url: "Observation?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("ARt4vJRNFmX")
            }
        }
        else null,

        // Systolic Blood Pressure (mmHg) - INTEGER_POSITIVE
        if getDeById("Y3xF4qecAVw") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("Y3xF4qecAVw"),
            resource: {
                resourceType: "Observation",
                status: "final",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("Y3xF4qecAVw")
                } ],
                category: [ {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/observation-category",
                        code: "vital-signs",
                        display: "Vital Signs"
                    } ]
                } ],
                code: {
                    coding: [ {
                        system: "http://snomed.info/sct",
                        code: "271649006",
                        display: "Systolic blood pressure (observable entity)"
                    } ],
                    text: "Systolic Blood Pressure (mmHg)"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference },
                effectiveDateTime: effectiveDateTime,
                valueInteger: asInt(getDeById("Y3xF4qecAVw"))
            },
            request: {
                method: "PUT",
                url: "Observation?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("Y3xF4qecAVw")
            }
        } else null,

        // Diastolic Blood Pressure (mmHg) - INTEGER_POSITIVE
        if getDeById("QMCuOLLRHHD") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("QMCuOLLRHHD"),
            resource: {
                resourceType: "Observation",
                status: "final",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("QMCuOLLRHHD")
                } ],
                category: [ {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/observation-category",
                        code: "vital-signs",
                        display: "Vital Signs"
                    } ]
                } ],
                code: {
                    coding: [ {
                        system: "http://snomed.info/sct",
                        code: "271650006",
                        display: "Diastolic blood pressure (observable entity)"
                    } ],
                    text: "Diastolic Blood Pressure (mmHg)"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference },
                effectiveDateTime: effectiveDateTime,
                valueInteger: asInt(getDeById("QMCuOLLRHHD"))
            },
            request: {
                method: "PUT",
                url: "Observation?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("QMCuOLLRHHD")
            }
        } else null,

        // Foetal Heart Sounds (FHS) - TRUE_ONLY
        if getDeById("xTsKEMPT2so") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("xTsKEMPT2so"),
            resource: {
                resourceType: "Observation",
                status: "final",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("xTsKEMPT2so")
                } ],
                category: [ {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/observation-category",
                        code: "exam",
                        display: "Exam"
                    } ]
                } ],
                code: {
                    coding: [ {
                        system: "http://snomed.info/sct",
                        code: "249045009",
                        display: "Fetal heart sounds present (finding)"
                    } ],
                    text: "Foetal Heart Sounds (FHS)"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference },
                effectiveDateTime: effectiveDateTime,
                valueBoolean: true
            },
            request: {
                method: "PUT",
                url: "Observation?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("xTsKEMPT2so")
            }
        } else null,

        // Other examniation findings - LONG_TEXT
        if getDeById("aq7pHlVvpsO") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("aq7pHlVvpsO"),
            resource: {
                resourceType: "Observation",
                status: "final",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("aq7pHlVvpsO")
                } ],
                category: [ {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/observation-category",
                        code: "exam",
                        display: "Exam"
                    } ]
                } ],
                code: {
                    coding: [ {
                        system: "http://snomed.info/sct",
                        code: "715851000000102",
                        display: "Examination findings (record artifact)"
                    } ],
                    text: "Other examniation findings"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference },
                effectiveDateTime: effectiveDateTime,
                valueString: getDeById("aq7pHlVvpsO")
            },
            request: {
                method: "PUT",
                url: "Observation?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("aq7pHlVvpsO")
            }
        } else null,

        // Investigation results - LONG_TEXT
        if getDeById("JU6L76XajgD") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("JU6L76XajgD"),
            resource: {
                resourceType: "Observation",
                status: "final",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("JU6L76XajgD")
                } ],
                category: [ {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/observation-category",
                        code: "exam",
                        display: "Exam"
                    } ]
                } ],
                code: {
                    coding: [ {
                        system: "http://snomed.info/sct",
                        code: "1082101000000102",
                        display: "Investigation results (record artifact)"
                    } ],
                    text: "Investigation results"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference },
                effectiveDateTime: effectiveDateTime,
                valueString: getDeById("JU6L76XajgD")
            },
            request: {
                method: "PUT",
                url: "Observation?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("JU6L76XajgD")
            }
        } else null,

        // Management & Recommendations - LONG_TEXT
        if getDeById("B6mWJ74OY4e") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("B6mWJ74OY4e"),
            resource: {
                resourceType: "Observation",
                status: "final",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("B6mWJ74OY4e")
                } ],
                category: [ {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/observation-category",
                        code: "exam",
                        display: "Exam"
                    } ]
                } ],
                code: {
                    coding: [ {
                        system: "http://snomed.info/sct",
                        code: "1382601000000107",
                        display: "Recommended Summary Plan for Emergency Care and Treatment form (record artifact)"
                    } ],
                    text: "Management & Recommendations"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference },
                effectiveDateTime: effectiveDateTime,
                valueString: getDeById("B6mWJ74OY4e")
            },
            request: {
                method: "PUT",
                url: "Observation?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("B6mWJ74OY4e")
            }
        } else null,

        // Plan of management - LONG_TEXT
        if getDeById("XwlSz3I5QMA") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("XwlSz3I5QMA"),
            resource: {
                resourceType: "Observation",
                status: "final",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("XwlSz3I5QMA")
                } ],
                category: [ {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/observation-category",
                        code: "exam",
                        display: "Exam"
                    } ]
                } ],
                code: {
                    coding: [ {
                        system: "http://snomed.info/sct",
                        code: "737427001",
                        display: "Clinical management plan (record artifact)"
                    } ],
                    text: "Plan of management"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference },
                effectiveDateTime: effectiveDateTime,
                valueString: getDeById("XwlSz3I5QMA")
            },
            request: {
                method: "PUT",
                url: "Observation?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("XwlSz3I5QMA")
            }
        } else null
    ], function(x) x != null)
}