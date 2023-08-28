package org.hisp.dhis.fhir.aggregate;

import org.apache.camel.Exchange;
import org.hisp.dhis.fhir.domain.CodeSystemAndValueSet;
import org.hl7.fhir.r4.model.Bundle;

import java.util.List;

public class CodeSystemAndValueSetBundleAggregationStrategy extends AbstractBundleAggregationStrategy
{
    @Override
    protected List<Bundle.BundleEntryComponent> getEntries( Exchange exchange )
    {
        CodeSystemAndValueSet codeSystemAndValueSet = exchange.getIn().getBody( CodeSystemAndValueSet.class );

        Bundle.BundleEntryComponent codeSystemEntry = new Bundle.BundleEntryComponent();
        Bundle.BundleEntryComponent valueSetEntry = new Bundle.BundleEntryComponent();

        codeSystemEntry.setResource( codeSystemAndValueSet.getCodeSystem() )
            .getRequest().setUrl( "CodeSystem?identifier=" + codeSystemAndValueSet.getCodeSystem().getId() )
            .setMethod( Bundle.HTTPVerb.PUT );

        valueSetEntry.setResource( codeSystemAndValueSet.getValueSet() )
            .getRequest().setUrl( "ValueSet?identifier=" + codeSystemAndValueSet.getValueSet().getId() )
            .setMethod( Bundle.HTTPVerb.PUT );

        return List.of( codeSystemEntry, valueSetEntry );
    }

    @Override
    protected Bundle createBundle()
    {
        return new Bundle().setType( Bundle.BundleType.TRANSACTION );
    }

}
