package org.hisp.dhis.fhir.converters;

import lombok.RequiredArgsConstructor;
import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConverters;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.api.model.v2_39_1.Attribute__1;
import org.hisp.dhis.api.model.v2_39_1.TrackedEntityInstance;
import org.hisp.dhis.fhir.config.properties.DhisProperties;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TrackedEntityToPatientConverter implements TypeConverters
{

    private final DhisProperties dhisProperties;

    @Converter
    public Patient trackedEntityToPatient( TrackedEntityInstance trackedEntity, Exchange exchange )
    {
        return createPatient( trackedEntity );
    }

    private Patient createPatient( TrackedEntityInstance trackedEntity )
    {
        String namespace = dhisProperties.getBaseUrl() + "/api/trackedEntityInstances";

        Patient patient = new Patient();
        patient.setId( trackedEntity.getTrackedEntityInstance() );

        String gender = getAttributeValue( trackedEntity, "cejWyOfXge6" );
        String firstName = getAttributeValue( trackedEntity, "w75KJ2mc4zz" );
        String lastName = getAttributeValue( trackedEntity, "zDhUuAYrxNC" );

        patient.getIdentifier().add(
            new Identifier().setSystem( namespace ).setValue( trackedEntity.getTrackedEntityInstance() )
        );

        patient.setManagingOrganization( new Reference( "Organization?identifier=" + trackedEntity.getOrgUnit() ) );

        patient.setGender( getGender( gender ) );
        patient.getName().add( new HumanName().addGiven( firstName ).setFamily( lastName ) );

        return patient;
    }

    private Enumerations.AdministrativeGender getGender( String gender )
    {
        if ( gender == null )
        {
            return Enumerations.AdministrativeGender.UNKNOWN;
        }

        switch ( gender )
        {
        case "Female":
            return Enumerations.AdministrativeGender.FEMALE;
        case "Male":
            return Enumerations.AdministrativeGender.MALE;
        default:
            return Enumerations.AdministrativeGender.UNKNOWN;
        }
    }

    // Inefficient, only for demo purpose.
    public String getAttributeValue( TrackedEntityInstance trackedEntity, String id )
    {
        List<Attribute__1> attributes = trackedEntity.getAttributes().get();
        for ( Attribute__1 attribute : attributes )
        {
            if ( attribute.getAttribute().get().equals( id ) )
            {
                return StringUtils.trimToEmpty( attribute.getValue().get() );
            }
        }

        return null;
    }
}
