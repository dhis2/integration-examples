package org.hisp.hisp.dhis.fhir.converters;

import lombok.RequiredArgsConstructor;
import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConverters;
import org.apache.camel.component.fhir.internal.FhirConstants;
import org.hisp.hisp.dhis.fhir.config.properties.DhisProperties;
import org.hisp.hisp.dhis.fhir.domain.OrganisationUnit;
import org.hisp.hisp.dhis.fhir.domain.OrganisationUnits;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.springframework.util.StringUtils.hasText;

@Component
@RequiredArgsConstructor
public class OrgUnitToFhirBundleConverter implements TypeConverters
{
    private final DhisProperties dhisProperties;

    @Converter
    public Bundle ouToBundle( OrganisationUnits organisationUnits, Exchange exchange ) throws IOException
    {
        Bundle bundle = new Bundle().setType( Bundle.BundleType.TRANSACTION );

        for ( OrganisationUnit organisationUnit : organisationUnits.getOrganisationUnits() )
        {
            Organization organization = createOrganization( organisationUnit );
            Location location = createLocation( organisationUnit );

            bundle.addEntry().setResource( organization )
                .getRequest().setUrl( "Organization?identifier=" + organization.getId() )
                .setMethod( Bundle.HTTPVerb.POST )
                .setIfNoneExist( "identifier=" + organization.getId() );

            bundle.addEntry().setResource( location )
                .getRequest().setUrl( "Location?identifier=" + location.getId() )
                .setMethod( Bundle.HTTPVerb.POST )
                .setIfNoneExist( "identifier=" + location.getId() );
        }

        exchange.getIn().setHeader( FhirConstants.PROPERTY_PREFIX + "bundle", bundle );

        return bundle;
    }

    private Location createLocation( OrganisationUnit organisationUnit )
    {
        // https://www.hl7.org/fhir/location.html
        Location location = new Location();
        location.setId( organisationUnit.getId() );
        location.setName( organisationUnit.getName() );
        location.setStatus( Location.LocationStatus.ACTIVE );
        location.setMode( Location.LocationMode.INSTANCE );

        String namespace = dhisProperties.getBaseUrl() + "/api/organisationUnits";

        location.getIdentifier().add(
            new Identifier().setSystem( namespace ).setValue( organisationUnit.getId() )
        );

        if ( hasText( organisationUnit.getCode() ) )
        {
            location.getIdentifier().add(
                new Identifier().setSystem( namespace ).setValue( organisationUnit.getCode() )
            );
        }

        location.addType(
            new CodeableConcept().setText( "OF" ) );

        // https://www.hl7.org/fhir/valueset-location-physical-type.html
        location.getPhysicalType().addCoding()
            .setSystem( "http://terminology.hl7.org/CodeSystem/location-physical-type" )
            .setCode( "si" );

        location.setManagingOrganization( new Reference( "Organization/" + organisationUnit.getId() ) );

        if ( organisationUnit.getParent() != null )
        {
            location.setPartOf( new Reference( "Location/" + organisationUnit.getParent().getId() ) );
        }

        // TODO add geometry using extension http://hl7.org/fhir/StructureDefinition/location-boundary-geojson

        return location;
    }

    private Organization createOrganization( OrganisationUnit organisationUnit )
    {
        // https://www.hl7.org/fhir/organization.html
        Organization organization = new Organization();
        organization.setId( organisationUnit.getId() );
        organization.setName( organisationUnit.getName() );

        String namespace = dhisProperties.getBaseUrl() + "/api/organisationUnits";

        organization.getIdentifier().add(
            new Identifier().setSystem( namespace ).setValue( organisationUnit.getId() )
        );

        if ( hasText( organisationUnit.getCode() ) )
        {
            organization.getIdentifier().add(
                new Identifier().setSystem( namespace ).setValue( organisationUnit.getCode() )
            );
        }

        // https://www.hl7.org/fhir/valueset-organization-type.html
        organization.addType(
            new CodeableConcept(
                new Coding( "http://terminology.hl7.org/CodeSystem/organization-type", "prov", "Facility" ) ) );

        return organization;
    }
}
