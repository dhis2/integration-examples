package org.hisp.dhis.integration.fhir.route;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.hisp.dhis.integration.fhir.FhirMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FhirGatewayRouteBuilder extends RouteBuilder {
    
  @Autowired
  protected FhirMapper fhirMapper;

  @Override
  public void configure() throws Exception {
      
    from("direct:readQuestionnaireResponse")
        .setHeader("CamelDhis2.queryParams", () -> Map.of("program", "IpHINAT79UW", "ouMode", "ACCESSIBLE"))
        .toD("dhis2://get/resource?path=tracker/trackedEntities/${header.rid}&fields=*&client=#dhis2Client")
        .transform(fhirMapper);
    
  }
}