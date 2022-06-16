package org.hidp.dhis.fhir.esavi.paho.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataValue {
    private String dataElement;
    private String value;
}
