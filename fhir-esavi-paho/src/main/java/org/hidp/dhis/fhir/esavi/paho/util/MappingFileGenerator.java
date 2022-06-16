package org.hidp.dhis.fhir.esavi.paho.util;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;

public class MappingFileGenerator {

    private static final String DS_MAPPING_FILE = "fhirqs.mapping.ds";
    private static final String TEI_ATT_MAPPING_FILE = "teiatts.mapping.yml";
    private static final String DATA_ELEMENT_MAPPING_FILE = "dataelements.mapping.yml";

    private static final String TEI_ATT_PLACEHOLDER = "%teiatt_mappings%";
    private static final String DATA_ELEMENT_PLACEHOLDER = "%de_mappings%";

    private final static Yaml YAML = new Yaml();
    private final Path tempFile;

    public MappingFileGenerator(Path tempFile) {
        this.tempFile = tempFile;
    }

    private static String generateMappings(InputStream inputStream) {
        Map<String, String> teiAttributes = YAML.load(inputStream);

        StringBuilder teiAttBuilder = new StringBuilder();
        teiAttributes.forEach((k, v) -> {
            teiAttBuilder.append("\"");
            teiAttBuilder.append(k);
            teiAttBuilder.append("\" : \"");
            teiAttBuilder.append(v);
            teiAttBuilder.append("\",");
        });
        teiAttBuilder.deleteCharAt(teiAttBuilder.length() - 1);

        return teiAttBuilder.toString();
    }

    public void generate() throws IOException, URISyntaxException {
        String dsMapping = Files.readString(Paths.get(Objects.requireNonNull(
                this.getClass().getClassLoader().getResource(DS_MAPPING_FILE)).toURI()));

        // tracked entity attributes
        InputStream teiAttsStream = this.getClass().getClassLoader().getResourceAsStream(TEI_ATT_MAPPING_FILE);
        dsMapping = dsMapping.replace(TEI_ATT_PLACEHOLDER, generateMappings(teiAttsStream));

        // data elements
        InputStream dataElementsStream = this.getClass().getClassLoader().getResourceAsStream(DATA_ELEMENT_MAPPING_FILE);
        dsMapping = dsMapping.replace(DATA_ELEMENT_PLACEHOLDER, generateMappings(dataElementsStream));

        Files.writeString(tempFile, dsMapping, StandardOpenOption.WRITE);
    }
}
