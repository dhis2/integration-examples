package org.hisp.dhis.integration.fhir;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.RuntimeCamelException;
import org.hl7.fhir.r5.elementmodel.Element;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.formats.IParser;
import org.hl7.fhir.utilities.ByteProvider;
import org.hl7.fhir.utilities.VersionUtilities;
import org.hl7.fhir.validation.IgLoader;
import org.hl7.fhir.validation.ValidationEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

public class FhirMapper implements Expression {

    private final ValidationEngine validationEngine;

    // this method is called when the Camel application is starting up
    public FhirMapper() throws IOException, URISyntaxException {

        // create transformation/validation engine
        ValidationEngine.ValidationEngineBuilder validationEngineBuilder = new ValidationEngine.ValidationEngineBuilder();
        final String definitions = VersionUtilities.packageForVersion("4.0.1") + "#" + VersionUtilities.getCurrentVersion("4.0.1");
        validationEngine = validationEngineBuilder.fromSource(definitions);

        // create IG loader
        IgLoader igLoader = new IgLoader(validationEngine.getPcm(), validationEngine.getContext(), validationEngine.getVersion(), validationEngine.isDebug());

        // load into validation engine the MinimalGatewayIG from the IG package
        igLoader.loadIg(validationEngine.getIgs(), validationEngine.getBinaries(), Paths.get("package.tgz").toAbsolutePath().toString(), false);
    }

    @Override
    // this method is called after receiving the tracked entity from DHIS2
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        try {
            // get JSON tracked entity from the Camel message
            String body = exchange.getMessage().getBody(String.class);

            // transform tracked entity into a QuestionnaireResponse
            Element qrAsElement = validationEngine.transform(ByteProvider.forBytes(body.getBytes()), Manager.FhirFormat.JSON, "https://dhis2.org/fhir/StructureMap/TrackedEntityToBundle");

            ByteArrayOutputStream qrAsOutputStream = new ByteArrayOutputStream();
            // serialise the in-memory QuestionnaireResponse to JSON and write it to qrAsOutputStream
            new org.hl7.fhir.r5.elementmodel.JsonParser(validationEngine.getContext()).compose(qrAsElement, qrAsOutputStream, IParser.OutputStyle.PRETTY, null);

            // return the QuestionnaireResponse as a string
            return (T) qrAsOutputStream.toString();
        } catch (Exception e) {
            throw new RuntimeCamelException(e);
        }
    }
}