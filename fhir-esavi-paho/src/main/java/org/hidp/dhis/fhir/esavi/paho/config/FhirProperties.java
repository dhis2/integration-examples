package org.hidp.dhis.fhir.esavi.paho.config;

import ca.uhn.fhir.context.FhirVersionEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Data
@Component
@Validated
@ConfigurationProperties("dhis2-to-fhir.fhir")
public class FhirProperties {

    @NotNull
    private String serverUrl;

    @NotNull
    private FhirVersionEnum fhirVersion = FhirVersionEnum.R4;
}
