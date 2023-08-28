package org.hisp.dhis.fhir.converters;

import lombok.RequiredArgsConstructor;
import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConverters;
import org.hisp.dhis.api.model.v2_39_1.Option;
import org.hisp.dhis.api.model.v2_39_1.OptionSet;
import org.hisp.dhis.fhir.config.properties.DhisProperties;
import org.hisp.dhis.fhir.domain.CodeSystemAndValueSet;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.stereotype.Component;

import static org.springframework.util.StringUtils.hasText;

@Component
@RequiredArgsConstructor
public class OptionSetToCodeSystemAndValueSetConverter implements TypeConverters
{
    private final DhisProperties dhisProperties;

    @Converter
    public CodeSystemAndValueSet optionSetToCodeSystemAndValueSet( OptionSet optionSet, Exchange exchange )
    {
        CodeSystem codeSystem = createCodeSystem( optionSet );
        ValueSet valueSet = createValueSet( optionSet );

        return new CodeSystemAndValueSet( codeSystem, valueSet );
    }

    private CodeSystem createCodeSystem( OptionSet optionSet )
    {
        String namespace = dhisProperties.getBaseUrl() + "/api/optionSets";

        CodeSystem codeSystem = new CodeSystem();
        codeSystem.setId( optionSet.getId().get() );
        codeSystem.setUrl( namespace + "/" + optionSet.getId() + "/codeSystem" );
        codeSystem.setValueSet( namespace + "/" + optionSet.getId() + "/valueSet" );
        codeSystem.setName( optionSet.getName().get() );
        codeSystem.setTitle( optionSet.getName().get() );
        codeSystem.setDescription( (String) optionSet.getAdditionalProperties().get( "description" ) );
        codeSystem.setPublisher( dhisProperties.getBaseUrl() );
        codeSystem.setStatus( Enumerations.PublicationStatus.ACTIVE );
        codeSystem.setContent( CodeSystem.CodeSystemContentMode.COMPLETE );
        codeSystem.setVersion( String.valueOf( optionSet.getVersion() ) );
        codeSystem.setExperimental( false );
        codeSystem.setCaseSensitive( true );

        codeSystem.getIdentifier().add(
            new Identifier().setSystem( namespace ).setValue( optionSet.getId().get() )
        );

        if ( hasText( optionSet.getCode().orElse( null ) ) )
        {
            codeSystem.getIdentifier().add(
                new Identifier().setSystem( namespace ).setValue( optionSet.getCode().get() )
            );
        }

        for ( Option option : optionSet.getOptions().get() )
        {
            codeSystem.addConcept()
                .setDisplay( option.getName().get() )
                .setDefinition( option.getName().get() )
                .setCode( option.getCode().orElse( null ) );
        }

        return codeSystem;
    }

    private ValueSet createValueSet( OptionSet optionSet )
    {
        String namespace = dhisProperties.getBaseUrl() + "/api/optionSets";

        ValueSet valueSet = new ValueSet();
        valueSet.setId( optionSet.getId().get() );
        valueSet.setUrl( namespace + "/" + optionSet.getId() + "/valueSet" );
        valueSet.setName( optionSet.getName().get() );
        valueSet.setTitle( optionSet.getName().get() );
        valueSet.setDescription( (String) optionSet.getAdditionalProperties().get( "description" ) );
        valueSet.setStatus( Enumerations.PublicationStatus.ACTIVE );
        valueSet.setVersion( String.valueOf( optionSet.getVersion() ) );
        valueSet.setExperimental( false );
        valueSet.setImmutable( true );

        valueSet.getIdentifier().add(
            new Identifier().setSystem( namespace ).setValue( optionSet.getId().get() )
        );

        if ( hasText( optionSet.getCode().orElse( null ) ) )
        {
            valueSet.getIdentifier().add(
                new Identifier().setSystem( namespace ).setValue( optionSet.getCode().get() )
            );
        }

        valueSet.setCompose( new ValueSet.ValueSetComposeComponent()
            .addInclude(
                new ValueSet.ConceptSetComponent().setSystem( namespace + "/" + optionSet.getId() + "/codeSystem" ) ) );

        return valueSet;
    }
}
