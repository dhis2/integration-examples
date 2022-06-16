package org.hidp.dhis.fhir.esavi.paho;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.RequiredArgsConstructor;
import org.hidp.dhis.fhir.esavi.paho.config.FhirProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@RequiredArgsConstructor
@EnableConfigurationProperties
public class MainApplication {
    private final FhirProperties fhirProperties;

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

    @Bean
    public FhirContext fhirContext() {
        return fhirProperties.getFhirVersion().newContext();
    }

    @Bean
    public IGenericClient fhirClient(FhirContext fhirContext) {
        return fhirContext.newRestfulGenericClient(fhirProperties.getServerUrl());
    }
}
