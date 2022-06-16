package org.hidp.dhis.fhir.esavi.paho.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TrackedEntities
{
    @JsonAlias( "trackedEntityInstances" )
    private List<TrackedEntity> trackedEntities = new ArrayList<>();
}
