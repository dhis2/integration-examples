/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.integration.camel;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.hisp.dhis.integration.sdk.Dhis2ClientBuilder;
import org.hisp.dhis.integration.sdk.api.Dhis2Client;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.TestSocketUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@CamelSpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class AbstractFunctionalTestCase {

  @Container public static GenericContainer<?> HAPI_FHIR_CONTAINER;

  @Container public static GenericContainer<?> DHIS2_CONTAINER;

  @Container public static GenericContainer<?> DHIS2_DB_CONTAINER;

  @Autowired protected CamelContext camelContext;

  protected static IGenericClient fhirClient;

  protected static String authorisationServerUrl;

  protected static Dhis2Client dhis2Client;

  protected CamelContext authorisationServerCamelContext;

  private static GenericContainer<?> newHapiFhirContainer() {
    return new GenericContainer<>(DockerImageName.parse("hapiproject/hapi:v8.2.0-2-tomcat"))
        .withEnv("SPRING_CONFIG_LOCATION", "file:///data/hapi/application.yaml")
        .withFileSystemBind(
            "../config/ehr/nehr-ips-package.tgz",
            "/package.tgz",
            BindMode.READ_ONLY)
        .withFileSystemBind(
            "../config/ehr/hapi.application.yaml",
            "/data/hapi/application.yaml",
            BindMode.READ_ONLY)
        .withExposedPorts(8080)
        .waitingFor(
            new HttpWaitStrategy().forStatusCode(200).withStartupTimeout(Duration.ofSeconds(300)));
  }

  private static GenericContainer<?> newDhis2Container() {
    Network.NetworkImpl dhis2Network = Network.builder().build();

    DHIS2_DB_CONTAINER = newPostgreSQLContainer("dhis2", "dhis", "dhis", dhis2Network);
    DHIS2_DB_CONTAINER.start();
    System.setProperty("dhis2DatabasePort", DHIS2_DB_CONTAINER.getFirstMappedPort().toString());

    return new GenericContainer<>("dhis2/core:42.1.0")
        .withClasspathResourceMapping("dhis.conf", "/opt/dhis2/dhis.conf", BindMode.READ_WRITE)
        .withNetwork(dhis2Network)
        .withExposedPorts(8080)
        .dependsOn(DHIS2_DB_CONTAINER)
        .waitingFor(
            new HttpWaitStrategy().forStatusCode(200).withStartupTimeout(Duration.ofSeconds(120)))
        .withEnv("WAIT_FOR_DB_CONTAINER", "db" + ":" + 5432 + " -t 0");
  }

  private static PostgreSQLContainer<?> newPostgreSQLContainer(
      String databaseName, String username, String password, Network network) {
    return new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:12-3.2-alpine")
                .asCompatibleSubstituteFor("postgres"))
        .withFileSystemBind("../db-dump", "/docker-entrypoint-initdb.d/", BindMode.READ_ONLY)
        .withFileSystemBind(
            "../config/dhis2/postgresql.conf", "/etc/postgresql.conf", BindMode.READ_ONLY)
        .withCommand("postgres -c config_file=/etc/postgresql.conf")
        .withExposedPorts(5432)
        .withDatabaseName(databaseName)
        .withNetworkAliases("db")
        .withUsername(username)
        .withPassword(password)
        .withNetwork(network);
  }

  @BeforeAll
  public static void beforeAll() throws IOException {
    if (HAPI_FHIR_CONTAINER == null) {
      Files.deleteIfExists(Path.of("target/offset.dat"));

      DHIS2_CONTAINER = newDhis2Container();
      DHIS2_CONTAINER.start();
      String dhis2ApiUrl =
          String.format(
              "http://%s:%s/api", DHIS2_CONTAINER.getHost(), DHIS2_CONTAINER.getFirstMappedPort());
      System.setProperty("dhis2ApiUrl", dhis2ApiUrl);
      dhis2Client = Dhis2ClientBuilder.newClient(dhis2ApiUrl, "admin", "district").build();

      HAPI_FHIR_CONTAINER = newHapiFhirContainer();
      HAPI_FHIR_CONTAINER.start();
      String fhirServerUrl =
          String.format("http://localhost:%s/fhir", HAPI_FHIR_CONTAINER.getFirstMappedPort());
      System.setProperty("ehr-url", fhirServerUrl);
      authorisationServerUrl =
          String.format(
              "http://localhost:%s/realms/ehr/protocol/openid-connect/token",
              TestSocketUtils.findAvailableTcpPort());
      fhirClient = FhirVersionEnum.R4.newContext().newRestfulGenericClient(fhirServerUrl);

      System.setProperty("oauth2.tokenEndpoint", authorisationServerUrl);
    }
  }

  protected void startMockAuthorisationServer() throws Exception {
    if (authorisationServerCamelContext == null) {
        authorisationServerCamelContext = new org.apache.camel.impl.DefaultCamelContext();
        authorisationServerCamelContext.addRoutes(new org.apache.camel.builder.RouteBuilder() {
            @Override
          public void configure() {
            from("jetty:" + authorisationServerUrl)
                .process(
                    exchange -> {
                      assertEquals(
                          "Basic ZWhyLWNsaWVudDpwYXNzdzByZA==",
                          exchange.getMessage().getHeader("Authorization"));
                      assertEquals(
                          "grant_type=client_credentials",
                          exchange.getMessage().getBody(String.class));
                    })
                .setBody(
                    (Function<Exchange, Object>)
                        exchange ->
                            Map.of(
                                "access_token",
                                "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIzOGZSMXNmQzlQb0IxTlcyTTRHUnN4d1UzdUZfYmNjUGNseWt2WVU5c2pRIn0.eyJleHAiOjE3MjczNDg0MTMsImlhdCI6MTcyNzM0ODExMywianRpIjoiN2E4OWQxYWQtOWY4OS00ZDVhLWI4MWItNDU1NjRkZDNjMTNjIiwiaXNzIjoiaHR0cDovL2tleWNsb2FrOjgwODAvcmVhbG1zL2NpdmlsLXJlZ2lzdHJ5IiwiYXVkIjpbImFjY291bnQiLCJjaXZpbC1yZWdpc3RyeS1jbGllbnQiXSwic3ViIjoiY2Y4NmFkY2QtMzIyNi00MWZmLThjY2EtMWJiM2FjNzUyOGM5IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiY2l2aWwtcmVnaXN0cnktY2xpZW50IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIvKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1jaXZpbC1yZWdpc3RyeSIsIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6ImVtYWlsIHByb2ZpbGUiLCJjbGllbnRIb3N0IjoiMTkyLjE2OC45Ni4xIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJzZXJ2aWNlLWFjY291bnQtY2l2aWwtcmVnaXN0cnktY2xpZW50IiwiY2xpZW50QWRkcmVzcyI6IjE5Mi4xNjguOTYuMSIsImNsaWVudF9pZCI6ImNpdmlsLXJlZ2lzdHJ5LWNsaWVudCJ9.MYcQDPNz7Z4URYcLOH3v60bNxkqJlyWvYPWIBWp_VYKKZrmTXH2nvG3hPkF8aTHT2P-Kom5iQSwrZz519WB16X-qVYCdvqnCQY1poRITnAXOsjF3I1Ymli29vWdKJvkn7aXmEYn54c00VvfyjCfKbjOweKa-UdIXjfcO8hATP7neo-UiNQ6a7-Sj2TEwGDBFc989Sj40JjIVh6G6rH2h5zte8mxZy1RZUhXDp3DppHZB0ddfrk5rkECLITfsAg6pzyHmzaPYOq8kSRis59yzKgWCXurkq4WOw9-Rz7oNIc1CfPan_8YvYtsnYUG35Rh44UU6cWJnyv1sDIgyUHPIZw",
                                "expires_in",
                                300,
                                "refresh_expires_in",
                                0,
                                "token_type",
                                "Bearer",
                                "not-before-policy",
                                0,
                                "scope",
                                "email profile"))
                .marshal()
                .json();
          }
        });
    }
    if (!authorisationServerCamelContext.isStarted()) {
        authorisationServerCamelContext.start();
        }
    }
    protected void stopMockAuthorisationServer() throws Exception {
        if (authorisationServerCamelContext != null && authorisationServerCamelContext.isStarted()) {
            authorisationServerCamelContext.stop();
        }
    }
}
