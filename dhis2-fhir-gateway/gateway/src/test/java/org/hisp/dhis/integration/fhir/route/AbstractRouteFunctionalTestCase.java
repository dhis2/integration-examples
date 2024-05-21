package org.hisp.dhis.integration.fhir.route;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@CamelSpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AbstractRouteFunctionalTestCase {

  @Container public static PostgreSQLContainer<?> POSTGRESQL_CONTAINER;

  @Container public static GenericContainer<?> DHIS2_CONTAINER;

  @Autowired protected CamelContext camelContext;

  @Autowired protected ProducerTemplate producerTemplate;

  private static PostgreSQLContainer<?> newPostgreSqlContainer() {
    return new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:12-3.3-alpine")
                .asCompatibleSubstituteFor("postgres"))
        .withFileSystemBind(".db-dump", "/docker-entrypoint-initdb.d/", BindMode.READ_WRITE)
        .withDatabaseName("dhis2")
        .withNetworkAliases("db")
        .withUsername("dhis")
        .withStartupTimeout(Duration.of(10, ChronoUnit.MINUTES))
        .withPassword("dhis")
        .withNetwork(Network.newNetwork());
  }

  private static GenericContainer<?> newDhis2Container(PostgreSQLContainer<?> postgreSqlContainer) {
    return new GenericContainer<>(DockerImageName.parse("dhis2/core:2.40.3"))
        .dependsOn(postgreSqlContainer)
        .withClasspathResourceMapping("dhis.conf", "/opt/dhis2/dhis.conf", BindMode.READ_WRITE)
        .withNetwork(postgreSqlContainer.getNetwork())
        .withExposedPorts(8080)
        .waitingFor(
            new HttpWaitStrategy().forStatusCode(200).withStartupTimeout(Duration.ofSeconds(120)))
        .withEnv("WAIT_FOR_DB_CONTAINER", "db" + ":" + 5432 + " -t 0");
  }

  @BeforeAll
  public static void beforeAll() throws Exception {
    if (POSTGRESQL_CONTAINER == null) {
      File file = new File(".db-dump/dhis2-db-sierra-leone.sql.gz");
      if (!file.exists()) {
        System.out.println("Downloading database dump...");
        FileUtils.copyURLToFile(
            new URL("https://databases.dhis2.org/sierra-leone/2.40.3/dhis2-db-sierra-leone.sql.gz"),
            file);
      }
      POSTGRESQL_CONTAINER = newPostgreSqlContainer();
      POSTGRESQL_CONTAINER.start();
      DHIS2_CONTAINER = newDhis2Container(POSTGRESQL_CONTAINER);
      DHIS2_CONTAINER.start();

      System.setProperty(
          "dhis2.apiUrl",
          String.format("http://localhost:%s/api", DHIS2_CONTAINER.getFirstMappedPort()));
    }
  }
}
