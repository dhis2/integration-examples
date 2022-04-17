package org.hidp.dhis.fhir.esavi.paho.routes;

import java.nio.charset.StandardCharsets;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.hidp.dhis.fhir.esavi.paho.config.DhisProperties;
import org.hidp.dhis.fhir.esavi.paho.model.TrackedEntities;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TrackedEntityToFhirQuestionnaireRoute extends RouteBuilder {
    private final DhisProperties dhisProperties;

    @Override
    public void configure() {
        String sourceUrl = dhisProperties.getBaseUrl() + "/api/trackedEntityInstances/r79wSdGII3v.json?program=aFGRl00bzio&fields=*";
        String basicAuth = HttpHeaders.encodeBasicAuth(dhisProperties.getUsername(), dhisProperties.getPassword(), StandardCharsets.UTF_8);

        JacksonDataFormat jacksonDataFormat = new JacksonDataFormat(TrackedEntities.class);
        jacksonDataFormat.setPrettyPrint(true);

        from("timer://foo?repeatCount=1").routeId("dhis2-te-to-fhir-ques")
            .setHeader("Authorization", constant(String.format("Basic %s", basicAuth)))
            .to(sourceUrl)
            .convertBodyTo(String.class)
            .transform(datasonnet("resource:classpath:fhirqs.ds", String.class))
            .log("${body}")
            .log("Done.");
    }
}
