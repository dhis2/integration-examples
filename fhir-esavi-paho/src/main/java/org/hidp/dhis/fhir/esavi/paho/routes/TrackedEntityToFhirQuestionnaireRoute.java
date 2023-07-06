/*
 * Copyright (c) 2004-2023, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hidp.dhis.fhir.esavi.paho.routes;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.RequiredArgsConstructor;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.hidp.dhis.fhir.esavi.paho.config.DhisProperties;
import org.hidp.dhis.fhir.esavi.paho.config.FhirProperties;
import org.hidp.dhis.fhir.esavi.paho.model.DataHolder;
import org.hidp.dhis.fhir.esavi.paho.model.TrackedEntity;
import org.hidp.dhis.fhir.esavi.paho.util.MappingFileGenerator;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackedEntityToFhirQuestionnaireRoute extends RouteBuilder
{
    private final DhisProperties dhisProperties;;

    private final FhirProperties fhirProperties;;

    @Override
    public void configure()
        throws Exception
    {
        String sourceUrl = dhisProperties.getBaseUrl()
            + "/api/trackedEntityInstances/r79wSdGII3v.json?program=aFGRl00bzio&fields=*";
        String basicAuth = HttpHeaders.encodeBasicAuth( dhisProperties.getUsername(), dhisProperties.getPassword(),
            StandardCharsets.UTF_8 );

        // generating the mapping file
        Path tempFile = Files.createTempFile( "mapping", ".ds" );
        MappingFileGenerator mappingFileGenerator = new MappingFileGenerator( tempFile );
        mappingFileGenerator.generate();

        // doing the mapping
        RouteDefinition routeDefinition = from( "timer://foo?repeatCount=1" )
            .routeId( "dhis2-te-to-fhir-ques" )
            .setHeader( "Authorization", constant( String.format( "Basic %s", basicAuth ) ) )
            .to( sourceUrl )
            .unmarshal().json( JsonLibrary.Jackson, TrackedEntity.class )
            .process( exchange -> {
                DataHolder dataHolder = new DataHolder( exchange.getMessage().getBody( TrackedEntity.class ) );
                exchange.getMessage().setBody( dataHolder, DataHolder.class );
            } )
            .marshal().json( JsonLibrary.Jackson )
            .convertBodyTo( String.class )
            .transform( datasonnet( "resource:file:" + tempFile.toAbsolutePath(), String.class ) )
            .log( "Inserting questionnaire response : ${body}" );

        if ( !fhirProperties.isDisabled() )
        {
            routeDefinition.to( "fhir://create/resource?inBody=resourceAsString&client=#fhirClient" )
                // The body we are getting is a MethodOutcome
                .log( "Questionnaire response created successfully : ${body.getId().getValue()}" );
        }

        routeDefinition.log( "Done." );
    }
}
