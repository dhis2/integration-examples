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
package org.hidp.dhis.fhir.esavi.paho.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;

import org.yaml.snakeyaml.Yaml;

public class MappingFileGenerator
{

    private static final String DS_MAPPING_FILE = "fhirqs.mapping.ds";

    private static final String TEI_ATT_MAPPING_FILE = "teiatts.mapping.yml";

    private static final String DATA_ELEMENT_MAPPING_FILE = "dataelements.mapping.yml";

    private static final String TEI_ATT_PLACEHOLDER = "%teiatt_mappings%";

    private static final String DATA_ELEMENT_PLACEHOLDER = "%de_mappings%";

    private final static Yaml YAML = new Yaml();

    private final Path tempFile;

    public MappingFileGenerator( Path tempFile )
    {
        this.tempFile = tempFile;
    }

    private static String generateMappings( InputStream inputStream )
    {
        Map<String, String> teiAttributes = YAML.load( inputStream );

        StringBuilder teiAttBuilder = new StringBuilder();
        teiAttributes.forEach( ( k, v ) -> {
            teiAttBuilder.append( "\"" );
            teiAttBuilder.append( k );
            teiAttBuilder.append( "\" : \"" );
            teiAttBuilder.append( v );
            teiAttBuilder.append( "\"," );
        } );
        teiAttBuilder.deleteCharAt( teiAttBuilder.length() - 1 );

        return teiAttBuilder.toString();
    }

    public void generate()
        throws IOException,
        URISyntaxException
    {
        String dsMapping = Files.readString( Paths.get( Objects.requireNonNull(
            this.getClass().getClassLoader().getResource( DS_MAPPING_FILE ) ).toURI() ) );

        // tracked entity attributes
        InputStream teiAttsStream = this.getClass().getClassLoader().getResourceAsStream( TEI_ATT_MAPPING_FILE );
        dsMapping = dsMapping.replace( TEI_ATT_PLACEHOLDER, generateMappings( teiAttsStream ) );

        // data elements
        InputStream dataElementsStream = this.getClass().getClassLoader()
            .getResourceAsStream( DATA_ELEMENT_MAPPING_FILE );
        dsMapping = dsMapping.replace( DATA_ELEMENT_PLACEHOLDER, generateMappings( dataElementsStream ) );

        Files.writeString( tempFile, dsMapping, StandardOpenOption.WRITE );
    }
}
