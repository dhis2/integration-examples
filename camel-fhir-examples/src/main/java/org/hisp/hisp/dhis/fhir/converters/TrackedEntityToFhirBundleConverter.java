package org.hisp.hisp.dhis.fhir.converters;

import lombok.RequiredArgsConstructor;
import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConverters;
import org.apache.camel.component.fhir.internal.FhirConstants;
import org.hisp.hisp.dhis.fhir.config.properties.DhisProperties;
import org.hisp.hisp.dhis.fhir.domain.TrackedEntities;
import org.hisp.hisp.dhis.fhir.domain.TrackedEntity;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackedEntityToFhirBundleConverter implements TypeConverters
{
    private final DhisProperties dhisProperties;

    @Converter
    public Bundle teToBundle( TrackedEntities trackedEntities, Exchange exchange )
    {
        Bundle bundle = new Bundle().setType( Bundle.BundleType.BATCH );

        for ( TrackedEntity trackedEntity : trackedEntities.getTrackedEntities() )
        {
            Patient patient = createPatient( trackedEntity );

            bundle.addEntry()
                .setResource( patient ).getRequest().setUrl( "Patient?identifier=" + patient.getId() )
                .setMethod( Bundle.HTTPVerb.PUT );
        }

        exchange.getIn().setHeader( FhirConstants.PROPERTY_PREFIX + "bundle", bundle );

        return bundle;
    }

    private Patient createPatient( TrackedEntity trackedEntity )
    {
        String namespace = dhisProperties.getBaseUrl() + "/api/trackedEntityInstances";

        Patient patient = new Patient();
        patient.setId( trackedEntity.getId() );

        String gender = trackedEntity.getAttributeValue( "cejWyOfXge6" );
        String firstName = trackedEntity.getAttributeValue( "w75KJ2mc4zz" );
        String lastName = trackedEntity.getAttributeValue( "zDhUuAYrxNC" );

        patient.getIdentifier().add(
            new Identifier().setSystem( namespace ).setValue( trackedEntity.getId() )
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
}
