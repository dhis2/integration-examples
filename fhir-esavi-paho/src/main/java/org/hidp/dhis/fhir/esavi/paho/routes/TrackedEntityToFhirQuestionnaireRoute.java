package org.hidp.dhis.fhir.esavi.paho.routes;

import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.hidp.dhis.fhir.esavi.paho.config.DhisProperties;
import org.hidp.dhis.fhir.esavi.paho.model.DataHolder;
import org.hidp.dhis.fhir.esavi.paho.model.TrackedEntity;
import org.hidp.dhis.fhir.esavi.paho.util.MappingFileGenerator;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class TrackedEntityToFhirQuestionnaireRoute extends RouteBuilder {
    private final DhisProperties dhisProperties;
    ;

    @Override
    public void configure() throws Exception {
        String sourceUrl = dhisProperties.getBaseUrl() + "/api/trackedEntityInstances/r79wSdGII3v.json?program=aFGRl00bzio&fields=*";
        String basicAuth = HttpHeaders.encodeBasicAuth(dhisProperties.getUsername(), dhisProperties.getPassword(), StandardCharsets.UTF_8);

        // generating the mapping file
        Path tempFile = Files.createTempFile("mapping", ".ds");
        MappingFileGenerator mappingFileGenerator = new MappingFileGenerator(tempFile);
        mappingFileGenerator.generate();

        // doing the mapping
        from("timer://foo?repeatCount=1")
                .routeId("dhis2-te-to-fhir-ques")
                .setHeader("Authorization", constant(String.format("Basic %s", basicAuth)))
                .to(sourceUrl)
                .unmarshal().json(JsonLibrary.Jackson, TrackedEntity.class)
                .process(exchange -> {
                    DataHolder dataHolder = new DataHolder(exchange.getMessage().getBody(TrackedEntity.class));
                    exchange.getMessage().setBody(dataHolder, DataHolder.class);
                })
                .marshal().json(JsonLibrary.Jackson)
                .convertBodyTo(String.class)
                .transform(datasonnet("resource:file:" + tempFile.toAbsolutePath(), String.class))
                .log("Inserting questionnaire response : ${body}")
                .to("fhir://create/resource?inBody=resourceAsString&client=#fhirClient")
                // The body we are getting is a MethodOutcome
                .log("Questionnaire response created successfully : ${body.getId().getValue()}")
                .log("Done.");
    }
}
