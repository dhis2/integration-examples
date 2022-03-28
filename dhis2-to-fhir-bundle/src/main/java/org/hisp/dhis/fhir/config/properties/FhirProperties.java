package org.hisp.hisp.dhis.fhir.config.properties;

import ca.uhn.fhir.context.FhirVersionEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties( "dhis2-to-fhir.fhir" )
public class FhirProperties
{
    private String serverUrl;

    // we only support R4
    private FhirVersionEnum fhirVersion = FhirVersionEnum.R4;
}
