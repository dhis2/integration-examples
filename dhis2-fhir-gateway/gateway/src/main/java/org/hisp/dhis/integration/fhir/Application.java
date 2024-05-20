package org.hisp.dhis.integration.fhir;

import org.hisp.dhis.integration.sdk.Dhis2ClientBuilder;
import org.hisp.dhis.integration.sdk.api.Dhis2Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {
  protected static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

  @Value("${dhis2.apiUrl:}")
  private String dhis2ApiUrl;

  @Value("${dhis2.username:#{null}}")
  private String dhis2Username;

  @Value("${dhis2.password:#{null}}")
  private String dhis2Password;

  @Value("${dhis2.pat:#{null}}")
  private String dhis2Pat;

  @Autowired private ConfigurableApplicationContext applicationContext;

  public static void main(String[] args) {
    SpringApplication springApplication = new SpringApplication(Application.class);
    springApplication.run(args);
  }

  @Bean
  public Dhis2Client dhis2Client() {
    if (!StringUtils.hasText(dhis2ApiUrl)) {
      terminate("Missing DHIS2 API URL. Are you sure that you set `dhis2.api.url`?");
    }

    if (dhis2Pat != null && (dhis2Username != null || dhis2Password != null)) {
      terminate(
          "Bad DHIS2 authentication configuration: PAT authentication and basic authentication are mutually exclusive. Either set `dhis2.pat` or both `dhis2.username` and `dhis2.password`");
    }

    Dhis2Client dhis2Client = null;
    if (StringUtils.hasText(dhis2Pat)) {
      dhis2Client = Dhis2ClientBuilder.newClient(dhis2ApiUrl, dhis2Pat).build();
    } else if (StringUtils.hasText(dhis2Username) && StringUtils.hasText(dhis2Password)) {
      dhis2Client = Dhis2ClientBuilder.newClient(dhis2ApiUrl, dhis2Username, dhis2Password).build();
    } else {
      terminate(
          "Missing DHIS2 authentication details. Are you sure that you set `dhis2.pat` or both `dhis2.username` and `dhis2.password`?");
    }

    return dhis2Client;
  }

  protected void terminate(String shutdownMessage) {
    LOGGER.error("TERMINATING!!! " + shutdownMessage);
    applicationContext.close();
    System.exit(1);
  }
}
