/*******************************************************************************
 * Copyright (c) 2019 Georgia Tech Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package edu.gatech.chai.omoponfhir.omopv5.stu3.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.exceptions.FHIRException;

import edu.gatech.chai.omoponfhir.omopv5.stu3.mapping.OmopCodeableConceptMapping;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.model.entity.Concept;

public class CodeableConceptUtil {
	public static void addCodingFromOmopConcept(CodeableConcept codeableConcept, Concept concept) throws FHIRException {
		String fhirUri = OmopCodeableConceptMapping.fhirUriforOmopVocabulary(concept.getVocabulary().getId());
		
		Coding coding = new Coding();
		coding.setSystem(fhirUri);
		coding.setCode(concept.getConceptCode());
		coding.setDisplay(concept.getName());
		
		codeableConcept.addCoding(coding);
	}
	
	public static CodeableConcept getCodeableConceptFromOmopConcept(Concept concept) throws FHIRException {
		CodeableConcept codeableConcept = new CodeableConcept();
		addCodingFromOmopConcept (codeableConcept, concept);		
		return codeableConcept;
	}
	
	public static Concept getOmopConceptWithOmopVacabIdAndCode(ConceptService conceptService, String omopVocabularyId, String code) {
		if (omopVocabularyId == null) return null;
		
		ParameterWrapper param = new ParameterWrapper(
				"String",
				Arrays.asList("vocabulary.id", "conceptCode"),
				Arrays.asList("like", "like"),
				Arrays.asList(omopVocabularyId, code),
				"and"
				);
		
		List<ParameterWrapper> params = new ArrayList<ParameterWrapper>();
		params.add(param);

		List<Concept> conceptIds = conceptService.searchWithParams(0, 0, params, null);
		if (conceptIds.isEmpty()) {
			return null;
		}
		
		// We should have only one entry... so... 
		return conceptIds.get(0);
	}
	
	public static Concept getOmopConceptWithFhirConcept(ConceptService conceptService, Coding fhirCoding) throws FHIRException {
		String system = fhirCoding.getSystem();
		String code = fhirCoding.getCode();
		
		String omopVocabularyId = OmopCodeableConceptMapping.omopVocabularyforFhirUri(system);
		return getOmopConceptWithOmopVacabIdAndCode(conceptService, omopVocabularyId, code);
	}
	
	public static Concept searchConcept(ConceptService conceptService, CodeableConcept codeableConcept) throws FHIRException {
		List<Coding> codings = codeableConcept.getCoding();
		for (Coding coding : codings) {
			// get OMOP Vocabulary from mapping.
			Concept ret = getOmopConceptWithFhirConcept(conceptService, coding);
			if (ret != null) return ret;
		}
		return null;
	}

	/**
	 * Creates a {@link CodeableConcept} from a {@link Concept}
	 * @param concept the {@link Concept} to use to generate the {@link CodeableConcept}
	 * @return a {@link CodeableConcept} generated from the passed in {@link Concept}
	 * @throws FHIRException if the {@link Concept} vocabulary cannot be mapped by the {@link OmopCodeableConceptMapping} fhirUriforOmopVocabularyi method.
     */
	public static CodeableConcept createFromConcept(Concept concept) throws FHIRException{
		String conceptVocab = concept.getVocabulary().getId();
		String conceptFhirUri = OmopCodeableConceptMapping.fhirUriforOmopVocabulary(conceptVocab);
		String conceptCode = concept.getConceptCode();
		String conceptName = concept.getName();

		Coding conceptCoding = new Coding();
		conceptCoding.setSystem(conceptFhirUri);
		conceptCoding.setCode(conceptCode);
		conceptCoding.setDisplay(conceptName);

		CodeableConcept codeableConcept = new CodeableConcept();
		codeableConcept.addCoding(conceptCoding);
		return codeableConcept;
	}
	
	/**
	 * 
	 * @param coding1
	 * @param coding2
	 * @return 
	 *   1 if only code matches,
	 *   0 if both system and code match,
	 *   -1 if none matches.
	 */
	public static int compareCodings(Coding coding1, Coding coding2) {
		boolean isSystemMatch = false;
		boolean isCodeMatch = false;
		
		if (coding1.hasSystem() && coding1.hasSystem()) {
			if (coding1.getSystem().equals(coding2.getSystem())) {
				isSystemMatch = true;
			}
		}
		
		if (coding1.hasCode() && coding2.hasCode()) {
			if (coding1.getCode().equals(coding2.getCode())) {
				isCodeMatch = true;
			}
		}
		
		if (isSystemMatch && isCodeMatch) return 0;
		if (isCodeMatch) return 1;
		return -1;
	}

}
