package org.hisp.dhis.fhir.aggregate;

import org.apache.camel.Exchange;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;

import java.util.List;

public class PatientBundleAggregationStrategy extends AbstractBundleAggregationStrategy
{
    @Override
    protected List<Bundle.BundleEntryComponent> getEntries( Exchange exchange )
    {
        Patient patient = exchange.getIn().getBody( Patient.class );
        Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
        entry.setResource( patient ).getRequest().setUrl( "Patient?identifier=" + patient.getId() )
            .setMethod( Bundle.HTTPVerb.PUT );

        return List.of( entry );
    }

    @Override
    protected Bundle createBundle()
    {
        return new Bundle().setType( Bundle.BundleType.BATCH );
    }

}
