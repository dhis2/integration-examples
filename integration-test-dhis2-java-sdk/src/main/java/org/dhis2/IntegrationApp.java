package org.dhis2;

import org.hisp.dhis.api.model.v2_36_11.DataValueSet;
import org.hisp.dhis.api.model.v2_36_11.DataValue__1;
import org.hisp.dhis.integration.sdk.Dhis2ClientBuilder;
import org.hisp.dhis.integration.sdk.api.Dhis2Client;

import java.io.IOException;

public final class IntegrationApp
{
    private IntegrationApp()
    {

    }

    public static void main( String[] args )
        throws
        IOException
    {
        String sourceDhis2ApiUrl = args[0];
        String sourceDhis2ApiUsername = args[1];
        String sourceDhis2ApiPassword = args[2];
        String sourceOrgUnitId = args[3];

        String targetDhis2ApiUrl = args[4];
        String targetDhis2ApiUsername = args[5];
        String targetDhis2ApiPassword = args[6];
        String targetOrgUnitId = args[7];

        String dataSetId = args[8];
        String period = args[9];

        Dhis2Client sourceDhis2Client = Dhis2ClientBuilder.newClient( sourceDhis2ApiUrl, sourceDhis2ApiUsername,
            sourceDhis2ApiPassword ).build();
        // pull data value set from source DHIS2 instance
        DataValueSet dataValueSet = sourceDhis2Client.get( "dataValueSets" )
            .withParameter( "dataSet", dataSetId ).withParameter( "period", period )
            .withParameter( "orgUnit", sourceOrgUnitId ).transfer().returnAs( DataValueSet.class );

        // replace source org unit IDs with target org unit IDs
        dataValueSet.setOrgUnit( targetOrgUnitId );
        for ( DataValue__1 dataValue : dataValueSet.getDataValues().get() )
        {
            dataValue.setOrgUnit( targetOrgUnitId );
        }

        Dhis2Client targetDhis2Client = Dhis2ClientBuilder.newClient( targetDhis2ApiUrl, targetDhis2ApiUsername,
            targetDhis2ApiPassword ).build();
        // push data value set to destination DHIS2 instance
        targetDhis2Client.post( "dataValueSets" ).withResource( dataValueSet ).transfer().close();
    }
}
