package org.hisp.dhis.fhir.aggregate;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePropertyKey;
import org.apache.camel.component.fhir.internal.FhirConstants;
import org.hl7.fhir.r4.model.Bundle;

import java.util.List;

public abstract class AbstractBundleAggregationStrategy implements AggregationStrategy
{
    public boolean isStoreAsBodyOnCompletion()
    {
        return true;
    }

    @Override
    public void onCompletion( Exchange exchange )
    {
        if ( exchange != null && isStoreAsBodyOnCompletion() )
        {
            Bundle bundle = (Bundle) exchange.removeProperty( ExchangePropertyKey.GROUPED_EXCHANGE );
            if ( bundle != null )
            {
                exchange.getIn().setBody( bundle );
                exchange.getIn().setHeader( FhirConstants.PROPERTY_PREFIX + "bundle", bundle );
            }
        }
    }

    @Override
    public Exchange aggregate( Exchange oldExchange, Exchange newExchange )
    {
        Bundle bundle;

        if ( oldExchange == null )
        {
            bundle = getBundle( newExchange );
        }
        else
        {
            bundle = getBundle( oldExchange );
        }

        if ( newExchange != null )
        {
            List<Bundle.BundleEntryComponent> entries = getEntries( newExchange );
            if ( entries != null )
            {
                for ( Bundle.BundleEntryComponent entry : entries )
                {
                    bundle.addEntry( entry );
                }
            }
        }

        return oldExchange != null ? oldExchange : newExchange;
    }

    protected abstract List<Bundle.BundleEntryComponent> getEntries( Exchange exchange );

    protected abstract Bundle createBundle();

    private Bundle getBundle( Exchange exchange )
    {
        Bundle bundle = exchange.getProperty( ExchangePropertyKey.GROUPED_EXCHANGE, Bundle.class );
        if ( bundle == null )
        {
            bundle = createBundle();
            exchange.setProperty( ExchangePropertyKey.GROUPED_EXCHANGE, bundle );
        }
        return bundle;
    }
}
