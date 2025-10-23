/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.integration.camel.util;

import org.hl7.fhir.r4.model.OperationOutcome;

public class FhirValidatorUtil {
    public static String extractValidationErrors(OperationOutcome outcome) {
        StringBuilder errorDetails = new StringBuilder();
        for (OperationOutcome.OperationOutcomeIssueComponent issue : outcome.getIssue()) {
            if (issue.getSeverity() == OperationOutcome.IssueSeverity.ERROR ||
                issue.getSeverity() == OperationOutcome.IssueSeverity.FATAL) {
                if (errorDetails.isEmpty()) {
                    errorDetails.append("Patient resource is not conformant to NEHR IPS profile:\nFailures:\n");
                }
                errorDetails.append(issue.getSeverity().toCode().toUpperCase())
                    .append(": ")
                    .append(issue.getCode().toCode())
                    .append(" - ")
                    .append(issue.getDiagnostics() != null ? issue.getDiagnostics() : "")
                    .append("\n");
            }
        }
        return errorDetails.toString();
    }
}
