package org.hisp.hisp.dhis.fhir.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OptionSet
{
    private String id;
    private String code;
    private String name;
    private String description;
    private int version;
    private List<Option> options = new ArrayList<>();
}
