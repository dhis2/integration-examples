package org.hidp.dhis.fhir.esavi.paho.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Enrollment {
    private List<Event> events = new ArrayList<>();
}
