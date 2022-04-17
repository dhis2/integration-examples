package org.hidp.dhis.fhir.esavi.paho;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@RequiredArgsConstructor
@EnableConfigurationProperties
public class MainApplication
{
    public static void main( String[] args )
    {
        SpringApplication.run( MainApplication.class, args );
    }
}
