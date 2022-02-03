package org.hisp.hisp.dhis.fhir.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties( ignoreUnknown = true )
public
class OrganisationUnits
{
    private List<OrganisationUnit> organisationUnits = new ArrayList<>();
}
