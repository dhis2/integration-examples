package org.hisp.dhis.fhir.converters;

import lombok.RequiredArgsConstructor;
import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConverters;
import org.hisp.dhis.api.model.v2_39_1.OrganisationUnit;
import org.hisp.dhis.fhir.config.properties.DhisProperties;
import org.hisp.dhis.fhir.domain.OrganizationAndLocation;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Component;

import static org.springframework.util.StringUtils.hasText;

@Component
@RequiredArgsConstructor
public class OrgUnitToOrganizationAndLocationConverter implements TypeConverters
{
    private final DhisProperties dhisProperties;

    @Converter
    public OrganizationAndLocation orgUnitToOrganizationAndLocation( OrganisationUnit organisationUnit,
        Exchange exchange )
    {
        Organization organization = createOrganization( organisationUnit );
        Location location = createLocation( organisationUnit );

        return new OrganizationAndLocation( organization, location );
    }

    private Location createLocation( OrganisationUnit organisationUnit )
    {
        // https://www.hl7.org/fhir/location.html
        Location location = new Location();
        location.setId( organisationUnit.getId().get() );
        location.setName( organisationUnit.getName().get() );
        location.setStatus( Location.LocationStatus.ACTIVE );
        location.setMode( Location.LocationMode.INSTANCE );

        String namespace = dhisProperties.getBaseUrl() + "/api/organisationUnits";

        location.getIdentifier().add(
            new Identifier().setSystem( namespace ).setValue( organisationUnit.getId().get() )
        );

        if ( hasText( organisationUnit.getCode().orElse( null ) ) )
        {
            location.getIdentifier().add(
                new Identifier().setSystem( namespace ).setValue( organisationUnit.getCode().get() )
            );
        }

        location.addType(
            new CodeableConcept().setText( "OF" ) );

        // https://www.hl7.org/fhir/valueset-location-physical-type.html
        location.getPhysicalType().addCoding()
            .setSystem( "http://terminology.hl7.org/CodeSystem/location-physical-type" )
            .setCode( "si" );

        location.setManagingOrganization( new Reference( "Organization/" + organisationUnit.getId().get() ) );

        if ( organisationUnit.getParent().isPresent() )
        {
            location.setPartOf( new Reference( "Location/" + organisationUnit.getParent().get().getId().get() ) );
        }

        // TODO add geometry using extension http://hl7.org/fhir/StructureDefinition/location-boundary-geojson

        return location;
    }

    private Organization createOrganization( OrganisationUnit organisationUnit )
    {
        // https://www.hl7.org/fhir/organization.html
        Organization organization = new Organization();
        organization.setId( organisationUnit.getId().get() );
        organization.setName( organisationUnit.getName().get() );

        String namespace = dhisProperties.getBaseUrl() + "/api/organisationUnits";

        organization.getIdentifier().add(
            new Identifier().setSystem( namespace ).setValue( organisationUnit.getId().get() )
        );

        if ( hasText( organisationUnit.getCode().orElse( null ) ) )
        {
            organization.getIdentifier().add(
                new Identifier().setSystem( namespace ).setValue( organisationUnit.getCode().get() )
            );
        }

        // https://www.hl7.org/fhir/valueset-organization-type.html
        organization.addType(
            new CodeableConcept(
                new Coding( "http://terminology.hl7.org/CodeSystem/organization-type", "prov", "Facility" ) ) );

        return organization;
    }
}
