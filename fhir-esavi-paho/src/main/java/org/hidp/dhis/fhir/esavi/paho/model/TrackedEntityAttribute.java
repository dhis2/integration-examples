package org.hidp.dhis.fhir.esavi.paho.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties( ignoreUnknown = true )
public class TrackedEntityAttribute
{
    private String displayName;
    private String attribute;
    private String code;
    private String value;
}
