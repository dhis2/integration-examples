package org.hisp.hisp.dhis.fhir.converters;

import lombok.RequiredArgsConstructor;
import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConverters;
import org.apache.camel.component.fhir.internal.FhirConstants;
import org.hisp.hisp.dhis.fhir.config.properties.DhisProperties;
import org.hisp.hisp.dhis.fhir.domain.Option;
import org.hisp.hisp.dhis.fhir.domain.OptionSet;
import org.hisp.hisp.dhis.fhir.domain.OptionSets;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.stereotype.Component;

import static org.springframework.util.StringUtils.hasText;

@Component
@RequiredArgsConstructor
public class OptionSetToFhirBundleConverter implements TypeConverters
{
    private final DhisProperties dhisProperties;

    @Converter
    public Bundle osToFhirBundle( OptionSets optionSets, Exchange exchange )
    {
        Bundle bundle = new Bundle().setType( Bundle.BundleType.BATCH );

        for ( OptionSet optionSet : optionSets.getOptionSets() )
        {
            CodeSystem codeSystem = createCodeSystem( optionSet );
            ValueSet valueSet = createValueSet( optionSet );

            bundle.addEntry().setResource( codeSystem )
                .getRequest().setUrl( "CodeSystem?identifier=" + codeSystem.getId() ).setMethod( Bundle.HTTPVerb.PUT );

            bundle.addEntry().setResource( valueSet )
                .getRequest().setUrl( "ValueSet?identifier=" + valueSet.getId() ).setMethod( Bundle.HTTPVerb.PUT );
        }

        exchange.getIn().setHeader( FhirConstants.PROPERTY_PREFIX + "bundle", bundle );

        return bundle;
    }

    private CodeSystem createCodeSystem( OptionSet optionSet )
    {
        String namespace = dhisProperties.getBaseUrl() + "/api/optionSets";

        CodeSystem codeSystem = new CodeSystem();
        codeSystem.setId( optionSet.getId() );
        codeSystem.setUrl( namespace + "/" + optionSet.getId() + "/codeSystem" );
        codeSystem.setValueSet( namespace + "/" + optionSet.getId() + "/valueSet" );
        codeSystem.setName( optionSet.getName() );
        codeSystem.setTitle( optionSet.getName() );
        codeSystem.setDescription( optionSet.getDescription() );
        codeSystem.setPublisher( dhisProperties.getBaseUrl() );
        codeSystem.setStatus( Enumerations.PublicationStatus.ACTIVE );
        codeSystem.setContent( CodeSystem.CodeSystemContentMode.COMPLETE );
        codeSystem.setVersion( String.valueOf( optionSet.getVersion() ) );
        codeSystem.setExperimental( false );
        codeSystem.setCaseSensitive( true );

        codeSystem.getIdentifier().add(
            new Identifier().setSystem( namespace ).setValue( optionSet.getId() )
        );

        if ( hasText( optionSet.getCode() ) )
        {
            codeSystem.getIdentifier().add(
                new Identifier().setSystem( namespace ).setValue( optionSet.getCode() )
            );
        }

        for ( Option option : optionSet.getOptions() )
        {
            codeSystem.addConcept()
                .setDisplay( option.getName() )
                .setDefinition( option.getName() )
                .setCode( option.getCode() );
        }

        return codeSystem;
    }

    private ValueSet createValueSet( OptionSet optionSet )
    {
        String namespace = dhisProperties.getBaseUrl() + "/api/optionSets";

        ValueSet valueSet = new ValueSet();
        valueSet.setId( optionSet.getId() );
        valueSet.setUrl( namespace + "/" + optionSet.getId() + "/valueSet" );
        valueSet.setName( optionSet.getName() );
        valueSet.setTitle( optionSet.getName() );
        valueSet.setDescription( optionSet.getDescription() );
        valueSet.setStatus( Enumerations.PublicationStatus.ACTIVE );
        valueSet.setVersion( String.valueOf( optionSet.getVersion() ) );
        valueSet.setExperimental( false );
        valueSet.setImmutable( true );

        valueSet.getIdentifier().add(
            new Identifier().setSystem( namespace ).setValue( optionSet.getId() )
        );

        if ( hasText( optionSet.getCode() ) )
        {
            valueSet.getIdentifier().add(
                new Identifier().setSystem( namespace ).setValue( optionSet.getCode() )
            );
        }

        valueSet.setCompose( new ValueSet.ValueSetComposeComponent()
            .addInclude( new ValueSet.ConceptSetComponent().setSystem( namespace + "/" + optionSet.getId() + "/codeSystem" ) ) );

        return valueSet;
    }
}
