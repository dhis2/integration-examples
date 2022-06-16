package org.hidp.dhis.fhir.esavi.paho.model;


import lombok.Data;
import org.hidp.dhis.fhir.esavi.paho.exceptions.UnsupportedPayloadException;

import java.util.HashMap;
import java.util.Map;

@Data
public class DataHolder {
    private TrackedEntity trackedEntity;
    private Map<String, String> teiAttributes = new HashMap<>();
    private Map<String, String> dataValues = new HashMap<>();

    public DataHolder(TrackedEntity trackedEntity) {
        this.trackedEntity = trackedEntity;

        // extract attributes
        trackedEntity.getAttributes().forEach(att -> teiAttributes.put(att.getAttribute(), att.getValue()));

        // extract data values
        if (trackedEntity.getEnrollments().size() > 1) {
            throw new UnsupportedPayloadException("This implementation supports only one enrollment");
        }

        for (Enrollment enrollment : trackedEntity.getEnrollments()) {
            for (Event event : enrollment.getEvents()) {
                event.getDataValues().forEach(dv -> dataValues.put(dv.getDataElement(), dv.getValue()));
            }
        }
    }
}
