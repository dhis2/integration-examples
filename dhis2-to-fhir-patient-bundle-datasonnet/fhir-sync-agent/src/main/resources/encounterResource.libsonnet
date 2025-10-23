{
    encounter_entry(ds, phnValue, event) ::
    local PROGRAM_ANC_ID = "eozjj9UivfS";
    local PROGRAM_ANC_NAME = "ANC Program";

    local REGISTRATION_ID = "LWJcStrI6kM";
    local VISIT_ID = "GX0z9IXFaso";
    local REFFERAL_ID = "aQYZkIhWzeJ";

    local STAGE_NAME = {
      [REGISTRATION_ID]: "Registration",
      [VISIT_ID]: "ANC Visits",
      [REFFERAL_ID]: "Referrals",
    };

    local dhis2User = (event.updatedBy) default null;
    local dhis2UserId = (dhis2User.uid) default null;

    local isCompleted = (event.status default null) == "COMPLETED";
    local eventId = event.event default null;
    local eventUid = event.event default null;
    local occurredAt = event.occurredAt default null;
    local scheduledAt = event.scheduledAt default null;
    local completedAt = event.completedAt default null;
    local program = event.program default null;
    local programStage = event.programStage default null; 

    local subjectReference = "Patient?identifier=http://fhir.health.gov.lk/ips/identifier/phn|" + phnValue;

    local encounterIdentifier = {
        system: "urn:dhis2:eventId", value: eventId
    };

    local periodDataType = std.prune({
        start: (scheduledAt default occurredAt),
        end: (completedAt default occurredAt),
    });

    local participants = std.prune([
        if dhis2UserId != null then {
            type: [
                {
                    text: "DHIS2 user who performed latest update to patient."
                }
            ],
            individual: {
                reference: "Practitioner?identifier=urn:dhis2:user:uid|" + dhis2UserId
            }
        }
    ]);

    local programStageText = STAGE_NAME[programStage] default "ANC Stage";
    local typeList = std.prune([
        if programStage != null then {
            coding: [
                {
                    system: "urn:dhis2:programStage",
                    code: programStage,
                    display: programStageText
                }
            ],
            text: programStageText
        }
    ]);

    local reasonCodes = [
        {
            coding: std.prune([
                {
                    system: "http://snomed.info/sct",
                    code: "77386006",
                    display: "Pregnant"
                },
                {
                    system: "urn:dhis2:program",
                    code: (program default PROGRAM_ANC_ID),
                    display: PROGRAM_ANC_NAME
                }
            ]),
            text: "ANC Care"
        }
    ];

    if !isCompleted then {}
    else {
        fullUrl: "urn:uuid:" + eventId,
        resource: std.prune({
            resourceType: "Encounter",
            identifier: [ encounterIdentifier ],
            status: "finished",
            class: {
                system: "http://terminology.hl7.org/CodeSystem/v3-ActCode", 
                code: "AMB",
                display: "ambulatory"
            },
            subject: { reference: subjectReference },
            period: periodDataType,
            type: typeList,
            reasonCode: reasonCodes,
            participant: participants
        }),
        request: {
            method: "PUT",
            url: "Encounter?identifier=urn:dhis2:eventId|" + eventId
        }
    }
}