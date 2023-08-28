package org.hisp.dhis.fhir.aggregate;

import org.apache.camel.Exchange;
import org.hisp.dhis.fhir.domain.OrganizationAndLocation;
import org.hl7.fhir.r4.model.Bundle;

import java.util.List;

public class OrganizationAndLocationBundleAggregationStrategy extends AbstractBundleAggregationStrategy
{
    @Override
    protected List<Bundle.BundleEntryComponent> getEntries( Exchange exchange )
    {
        OrganizationAndLocation organizationAndLocation = exchange.getIn().getBody( OrganizationAndLocation.class );

        Bundle.BundleEntryComponent organizationEntry = new Bundle.BundleEntryComponent();
        Bundle.BundleEntryComponent locationEntry = new Bundle.BundleEntryComponent();

        organizationEntry.setResource( organizationAndLocation.getOrganization() )
            .getRequest().setUrl( "Organization?identifier=" + organizationAndLocation.getOrganization().getId() )
            .setMethod( Bundle.HTTPVerb.POST )
            .setIfNoneExist( "identifier=" + organizationAndLocation.getOrganization().getId() );

        locationEntry.setResource( organizationAndLocation.getLocation() )
            .getRequest().setUrl( "Location?identifier=" + organizationAndLocation.getLocation().getId() )
            .setMethod( Bundle.HTTPVerb.POST )
            .setIfNoneExist( "identifier=" + organizationAndLocation.getLocation().getId() );

        return List.of( organizationEntry, locationEntry );
    }

    @Override
    protected Bundle createBundle()
    {
        return new Bundle().setType( Bundle.BundleType.TRANSACTION );
    }

}
