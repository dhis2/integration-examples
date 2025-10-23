{
    resources_for_registration_event(ds, phnValue, event) ::
    local REGISTRATION_ID = "LWJcStrI6kM";

    if (event.programStage default null) != REGISTRATION_ID then [] else

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
        // Age youngest child (INTEGER)
        if getDeById("BqkEw3MQDNI") != null then {
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
                        code: "age-youngest-child",
                        display: "Age of Youngest Child (yrs)"
                    }],
                    text: "Age of Youngest Child (yrs)"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference },
                effectiveDateTime: effectiveDateTime,
                valueInteger: asInt(getDeById("BqkEw3MQDNI"))
            },
            request: {
                method: "PUT",
                url: "Observation?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("BqkEw3MQDNI")
            }
        }
        else null,

        // Cardiac Diseases (BOOLEAN)
        if getDeById("R4EnBNPKblS") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("R4EnBNPKblS"),
            resource: {
                resourceType: "Condition",
                clinicalStatus: {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/condition-clinical",
                        code: "active",
                        display: "Active"
                    } ]
                },
                verificationStatus: {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/condition-verification",
                        code: "confirmed",
                        display: "Confirmed"
                    } ]
                },
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("R4EnBNPKblS")
                } ],
                code: {
                    coding: [ {
                        system: "http://snomed.info/sct",
                        code: "56265001",
                        display: "Heart disease (disorder)"
                    } ],
                    text: "Heart disease (disorder)"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference }
            },
            request: {
                method: "PUT",
                url: "Condition?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("R4EnBNPKblS")
            }
        }
        else null,

        // Consanguinity (BOOLEAN)
        if getDeById("YuZEbIjLLKZ") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("YuZEbIjLLKZ"),
            resource: {
                resourceType: "Observation",
                status: "final",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("YuZEbIjLLKZ")
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
                    coding: [ {
                        system: "http://snomed.info/sct",
                        code: "842009",
                        display: "Consanguinity"
                    } ],
                    text: "Consanguinity"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference },
                effectiveDateTime: effectiveDateTime,
                valueBoolean: asBoolean(getDeById("YuZEbIjLLKZ"))
            },
            request: {
                method: "PUT",
                url: "Observation?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("YuZEbIjLLKZ")
            }
        }
        else null,

        // Diabetes (BOOLEAN)
        if getDeById("D5khklwbOPt") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("D5khklwbOPt"),
            resource: {
                resourceType: "Condition",
                clinicalStatus: {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/condition-clinical",
                        code: "active",
                        display: "Active"
                    } ]
                },
                verificationStatus: {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/condition-verification",
                        code: "confirmed",
                        display: "Confirmed"
                    } ]
                },
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("D5khklwbOPt")
                } ],
                code: {
                    coding: [ {
                        system: "http://snomed.info/sct",
                        code: "73211009",
                        display: "Diabetes mellitus (disorder)"
                    } ],
                    text: "Diabetes mellitus (disorder)"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference }
            },
            request: {
                method: "PUT",
                url: "Condition?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("D5khklwbOPt")
            }
        }
        else null,

        // EDD by dates (DATE)
        if getDeById("r5TIiovGHdi") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("r5TIiovGHdi"),
            resource: {
                resourceType: "Observation",
                status: "final",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("r5TIiovGHdi")
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
                    coding: [ {
                        system: "http://loinc.org",
                        code: "11779-6",
                        display: "Delivery date Estimated from last menstrual period"
                    } ],
                    text: "EDD by dates"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference },
                effectiveDateTime: effectiveDateTime,
                valueDateTime: getDeById("r5TIiovGHdi")
            },
            request: {
                method: "PUT",
                url: "Observation?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("r5TIiovGHdi")
            }
        }
        else null,

        // Gravida (G) (INTEGER)
        if getDeById("TRkCMVFhrmr") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("TRkCMVFhrmr"),
            resource: {
                resourceType: "Observation",
                status: "final",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("TRkCMVFhrmr")
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
                    coding: [ {
                        system: "http://loinc.org",
                        code: "11996-6",
                        display: "[#] Pregnancies"
                    } ],
                    text: "Gravida (G)"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference },
                effectiveDateTime: effectiveDateTime,
                valueInteger: asInt(getDeById("TRkCMVFhrmr"))
            },
            request: {
                method: "PUT",
                url: "Observation?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("TRkCMVFhrmr")
            }
        }
        else null,

        // Hepatic Diseases (BOOLEAN)
        if getDeById("H8VPisy6Ket") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("H8VPisy6Ket"),
            resource: {
                resourceType: "Condition",
                clinicalStatus: {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/condition-clinical",
                        code: "active",
                        display: "Active"
                    } ]
                },
                verificationStatus: {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/condition-verification",
                        code: "confirmed",
                        display: "Confirmed"
                    } ]
                },
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("H8VPisy6Ket")
                } ],
                code: {
                    coding: [ {
                        system: "http://snomed.info/sct",
                        code: "235856003",
                        display: "Disease of liver (disorder)"
                    } ],
                    text: "Disease of liver (disorder)"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference }
            },
            request: {
                method: "PUT",
                url: "Condition?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("H8VPisy6Ket")
            }
        }
        else null,

        // History of subfertility (BOOLEAN)
        if getDeById("n2rSTrjRQ8O") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("n2rSTrjRQ8O"),
            resource: {
                resourceType: "Observation",
                status: "final",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("n2rSTrjRQ8O")
                } ],
                category: [
                    {
                        coding: [
                            {
                                system: "http://terminology.hl7.org/CodeSystem/observation-category",
                                code: "social-history",
                                display: "Social History"
                            }
                        ]
                    }
                ],
                code: {
                    coding: [ {
                        system: "http://snomed.info/sct",
                        code: "17276009",
                        display: "Decreased fertility"
                    } ],
                    text: "History of subfertility"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference },
                effectiveDateTime: effectiveDateTime,
                valueBoolean: asBoolean(getDeById("n2rSTrjRQ8O"))
            },
            request: {
                method: "PUT",
                url: "Observation?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("n2rSTrjRQ8O")
            }
        }
        else null,

        // Hypertension (BOOLEAN)
        if getDeById("plTkCmySMzT") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("plTkCmySMzT"),
            resource: {
                resourceType: "Condition",
                clinicalStatus: {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/condition-clinical",
                        code: "active",
                        display: "Active"
                    } ]
                },
                verificationStatus: {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/condition-verification",
                        code: "confirmed",
                        display: "Confirmed"
                    } ]
                },
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("plTkCmySMzT")
                } ],
                code: {
                    coding: [ {
                        system: "http://snomed.info/sct",
                        code: "38341003",
                        display: "Hypertensive disorder (disorder)"
                    } ],
                    text: "Hypertensive disorder (disorder)"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference }
            },
            request: {
                method: "PUT",
                url: "Condition?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("plTkCmySMzT")
            }
        }
        else null,

        // Last Menstrual Period (LMP) (DATE)
        if getDeById("vuDExM32SZ6") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("vuDExM32SZ6"),
            resource: {
                resourceType: "Observation",
                status: "final",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("vuDExM32SZ6")
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
                    coding: [ {
                        system: "http://loinc.org",
                        code: "8665-2",
                        display: "Last menstrual period start date"
                    } ],
                    text: "Last Menstrual Period (LMP)"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference },
                effectiveDateTime: effectiveDateTime,
                valueDateTime: getDeById("vuDExM32SZ6")
            },
            request: {
                method: "PUT",
                url: "Observation?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("vuDExM32SZ6")
            }
        }
        else null,

        // Number of Living Children (INTEGER)
        if getDeById("OMe6LMsusv4") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("OMe6LMsusv4"),
            resource: {
                resourceType: "Observation",
                status: "final",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("OMe6LMsusv4")
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
                    coding: [ {
                        system: "http://loinc.org",
                        code: "11638-4",
                        display: "[#] Births.still living"
                    } ],
                    text: "Number of Living Children"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference },
                effectiveDateTime: effectiveDateTime,
                valueInteger: asInt(getDeById("OMe6LMsusv4"))
            },
            request: {
                method: "PUT",
                url: "Observation?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("OMe6LMsusv4")
            }
        }
        else null,

        // On folic acid (BOOLEAN)
        if getDeById("J3eLUAZkM6M") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("J3eLUAZkM6M"),
            resource: {
                resourceType: "MedicationStatement",
                status: "active",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("J3eLUAZkM6M")
                } ],
                medicationCodeableConcept: {
                    coding: [ {
                        system: "http://snomed.info/sct",
                        code: "63718003",
                        display: "Folic acid (substance)"
                    } ],
                    text: "Folic acid (substance)"
                },
                context: { reference: encounterReference },
                subject: { reference: subjectReference },
                effectiveDateTime: effectiveDateTime
            },
            request: {
                method: "PUT",
                url: "MedicationStatement?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("J3eLUAZkM6M")
            }
        }
        else null,

        // Parity (P) (INTEGER)
        if getDeById("NPnDmeGBLsl") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("NPnDmeGBLsl"),
            resource: {
                resourceType: "Observation",
                status: "final",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("NPnDmeGBLsl")
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
                    coding: [ {
                        system: "http://loinc.org",
                        code: "11640-0",
                        display: "[#] Births total"
                    } ],
                    text: "Parity (P)"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference },
                effectiveDateTime: effectiveDateTime,
                valueInteger: asInt(getDeById("NPnDmeGBLsl"))
            },
            request: {
                method: "PUT",
                url: "Observation?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("NPnDmeGBLsl")
            }
        }
        else null,

        // POA at Registration (weeks) (INTEGER)
        if getDeById("No3jJWIR7bn") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("No3jJWIR7bn"),
            resource: {
                resourceType: "Observation",
                status: "final",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("No3jJWIR7bn")
                } ],
                category: [
                    {
                        coding: [
                            {
                                system: "http://terminology.hl7.org/CodeSystem/observation-category",
                                code: "exam",
                                display: "Exam"
                            }
                        ]
                    }
                ],
                code: {
                    coding: [ {
                        system: "http://loinc.org",
                        code: "11885-1",
                        display: "Gestational age Estimated from last menstrual period"
                    } ],
                    text: "POA at Registration (weeks)"
                },
                subject:   { reference: subjectReference },
                encounter: { reference: encounterReference },
                effectiveDateTime: effectiveDateTime,
                valueQuantity: {
                    value: asFloat(getDeById("No3jJWIR7bn")),
                    unit: "wk",
                    system: "http://unitsofmeasure.org",
                    code: "wk"
                }
            },
            request: {
                method: "PUT",
                url: "Observation?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("No3jJWIR7bn")
            }
        }
        else null,

        // Psychiatric Illnesses (BOOLEAN)
        if getDeById("OGGnIEQSH1O") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("OGGnIEQSH1O"),
            resource: {
                resourceType: "Condition",
                clinicalStatus: {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/condition-clinical",
                        code: "active",
                        display: "Active"
                    } ]
                },
                verificationStatus: {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/condition-verification",
                        code: "confirmed",
                        display: "Confirmed"
                    } ]
                },
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("OGGnIEQSH1O")
                } ],
                code: {
                    coding: [ {
                        system: "http://snomed.info/sct",
                        code: "74732009",
                        display: "Mental disorder (disorder)"
                    } ],
                    text: "Psychiatric Illnesses"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference }
            },
            request: {
                method: "PUT",
                url: "Condition?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("OGGnIEQSH1O")
            }
        }
        else null,

        // Renal Diseases (BOOLEAN)
        if getDeById("MQ2Hi22F8Uj") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("MQ2Hi22F8Uj"),
            resource: {
                resourceType: "Condition",
                clinicalStatus: {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/condition-clinical",
                        code: "active",
                        display: "Active"
                    } ]
                },
                verificationStatus: {
                    coding: [ {
                        system: "http://terminology.hl7.org/CodeSystem/condition-verification",
                        code: "confirmed",
                        display: "Confirmed"
                    } ]
                },
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("MQ2Hi22F8Uj")
                } ],
                code: {
                    coding: [ {
                        system: "http://snomed.info/sct",
                        code: "90708001",
                        display: "Kidney disease (disorder)"
                    } ],
                    text: "Kidney disease (disorder)"
                },
                subject: { reference: subjectReference },
                encounter: { reference: encounterReference }
            },
            request: {
                method: "PUT",
                url: "Condition?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("MQ2Hi22F8Uj")
            }
        }
        else null,

        // Rubella Immunization (BOOLEAN)
        if getDeById("RpRCMceQhks") != null then {
            fullUrl: "urn:uuid:" + uniqueResourceId("RpRCMceQhks"),
            resource: {
                resourceType: "Immunization",
                status: "completed",
                identifier: [ {
                    system: "urn:dhis2:dataelement:uid",
                    value: uniqueResourceId("RpRCMceQhks")
                } ],
                vaccineCode: {
                    coding: [ {
                        system: "http://snomed.info/sct",
                        code: "871732000",
                        display: "Rubella virus antigen only vaccine product"
                    } ],
                    text: "Rubella virus antigen only vaccine product"
                },
                patient: { reference: subjectReference },
                occurrenceDateTime: effectiveDateTime
            },
            request: {
                method: "PUT",
                url: "Immunization?identifier=urn:dhis2:dataelement:uid|" + uniqueResourceId("RpRCMceQhks")
            }
        }
        else null

    ], function(x) x != null)
}