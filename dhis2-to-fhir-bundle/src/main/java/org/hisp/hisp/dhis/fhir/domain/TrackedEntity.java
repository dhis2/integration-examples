package org.hisp.hisp.dhis.fhir.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties( ignoreUnknown = true )
public class TrackedEntity
{
    @JsonAlias( "trackedEntityInstance" )
    private String id;
    private String orgUnit;
    private List<TrackedEntityAttribute> attributes = new ArrayList<>();

    // Inefficient, only for demo purpose.
    public String getAttributeValue( String id )
    {
        for ( TrackedEntityAttribute attribute : attributes )
        {
            if ( attribute.getAttribute().equals( id ) )
            {
                return StringUtils.trimToEmpty( attribute.getValue() );
            }
        }

        return null;
    }
}
