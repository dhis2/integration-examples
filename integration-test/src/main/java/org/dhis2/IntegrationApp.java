package org.dhis2;

import kong.unirest.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

public final class IntegrationApp
{
    private IntegrationApp()
    {

    }

    public static void main( String[] args )
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

        // pull data value set from source DHIS2 instance
        HttpResponse<JsonNode> dataValueSet = Unirest.get(
                sourceDhis2ApiUrl + "/dataValueSets?dataSet={dataSetId}&period={period}&orgUnit={orgUnitId}" )
            .routeParam( "dataSetId", dataSetId )
            .routeParam( "period", period )
            .routeParam( "orgUnitId", sourceOrgUnitId )
            .basicAuth( sourceDhis2ApiUsername, sourceDhis2ApiPassword ).asJson();

        // replace source org unit IDs with target org unit IDs
        dataValueSet.getBody().getObject().put( "orgUnit", targetOrgUnitId );
        for ( Object dataValue : dataValueSet.getBody().getObject().getJSONArray( "dataValues" ) )
        {
            ((JSONObject) dataValue).put( "orgUnit", targetOrgUnitId );
        }

        // push data value set to destination DHIS2 instance
        Unirest.post( targetDhis2ApiUrl + "/dataValueSets" )
            .contentType( ContentType.APPLICATION_JSON.toString() )
            .body( dataValueSet.getBody() )
            .basicAuth( targetDhis2ApiUsername, targetDhis2ApiPassword ).asString();
    }
}
