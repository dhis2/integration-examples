package org.hisp.dhis.fhir.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;

@RequiredArgsConstructor
@Getter
public class CodeSystemAndValueSet
{
    private final CodeSystem codeSystem;
    private final ValueSet valueSet;
}
