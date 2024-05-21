package org.hisp.dhis.integration.fhir;

import static io.restassured.RestAssured.given;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class HawtioWebConsoleFunctionalTestCase {
  @LocalServerPort private int serverPort;

  private RequestSpecification hawtioRequestSpec;

  @BeforeEach
  public void beforeEach() {
    hawtioRequestSpec =
        new RequestSpecBuilder()
            .setBaseUri(String.format("http://localhost:%s/management/hawtio", serverPort))
            .setRelaxedHTTPSValidation()
            .build();
  }

  @Test
  public void testAnonymousHttpGet() throws InterruptedException {
    given(hawtioRequestSpec).get().then().statusCode(401);
  }

  @Test
  public void testAuthorisedHttpGet() {
    given(hawtioRequestSpec).auth().basic("test", "test").get().then().statusCode(200);
  }
}
