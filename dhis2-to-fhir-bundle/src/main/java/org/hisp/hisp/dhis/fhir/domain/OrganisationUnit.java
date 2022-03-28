package org.hisp.hisp.dhis.fhir.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties( ignoreUnknown = true )
public class OrganisationUnit
{
    private String id;
    private String code;
    private String name;
    private String description;
    private OrganisationUnit parent;
}
