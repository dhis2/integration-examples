package org.hidp.dhis.fhir.esavi.paho.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackedEntity {
    private String trackedEntityInstance;
    private String orgUnit;
    private List<TrackedEntityAttribute> attributes = new ArrayList<>();
    private List<Enrollment> enrollments = new ArrayList<>();
}
