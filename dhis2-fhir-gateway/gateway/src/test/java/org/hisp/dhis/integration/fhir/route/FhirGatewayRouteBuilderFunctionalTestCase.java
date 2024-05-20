package org.hisp.dhis.integration.fhir.route;

import org.apache.camel.Exchange;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.*;

public class FhirGatewayRouteBuilderFunctionalTestCase extends AbstractRouteFunctionalTestCase {
  @LocalServerPort 
  private int serverPort;

  @Test
  public void testConfigure() throws InterruptedException {
    Exchange exchange =
        producerTemplate.request(
            String.format("http://localhost:%s/api/QuestionnaireResponse/SBjuNw0Xtkn", serverPort), e -> {});

    assertEquals(200, exchange.getMessage().getHeader("CamelHttpResponseCode"));
    assertNotNull(exchange.getMessage().getBody(String.class));
  }
}