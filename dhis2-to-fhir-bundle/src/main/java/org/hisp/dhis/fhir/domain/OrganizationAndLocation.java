package org.hisp.dhis.fhir.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Organization;

@RequiredArgsConstructor
@Getter
public class OrganizationAndLocation
{
    private final Organization organization;
    private final Location location;
}
