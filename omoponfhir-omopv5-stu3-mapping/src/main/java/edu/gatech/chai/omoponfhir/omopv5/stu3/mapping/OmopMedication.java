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
package edu.gatech.chai.omoponfhir.omopv5.stu3.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.Medication.MedicationIngredientComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.CodeableConceptUtil;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.MedicationResourceProvider;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.model.entity.Concept;

public class OmopMedication extends BaseOmopResource<Medication, Concept, ConceptService>
		implements IResourceMapping<Medication, Concept> {

	private static OmopMedication omopMedication = new OmopMedication();
	
	public OmopMedication(WebApplicationContext context) {
		super(context, Concept.class, ConceptService.class, MedicationResourceProvider.getType());
		
		initialize(context);
	}

	public OmopMedication() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), Concept.class, ConceptService.class, MedicationResourceProvider.getType());
		
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}
	
	private void initialize(WebApplicationContext context) {
	}

	
	public static OmopMedication getInstance() {
		return omopMedication;
	}
	
	@Override
	public Long toDbase(Medication fhirResource, IdType fhirId) throws FHIRException {
		throw new FHIRException("Medication Resource is Read-Only");
	}

	@Override
	public Medication constructFHIR(Long fhirId, Concept entity) {
		Medication medication = new Medication();
		
		medication.setId(new IdType(fhirId));
		CodeableConcept medicationCodeableConcept;
		try {
			medicationCodeableConcept = CodeableConceptUtil.getCodeableConceptFromOmopConcept(entity);
		} catch (FHIRException e1) {
			e1.printStackTrace();
			return null;
		}		
		
		medication.setCode(medicationCodeableConcept);

		// See if we can add ingredient version of this medication.
		List<Concept> ingredients = getMyOmopService().getIngredient(entity);
		if (ingredients.size() > 0) {
			CodeableConcept ingredientCodeableConcept;
			try {
				for (Concept ingredient: ingredients) {
					ingredientCodeableConcept = CodeableConceptUtil.getCodeableConceptFromOmopConcept(ingredient);
					if (!ingredientCodeableConcept.isEmpty()) {
						MedicationIngredientComponent medIngredientComponent = new MedicationIngredientComponent();
						medIngredientComponent.setItem(ingredientCodeableConcept);
						medication.addIngredient(medIngredientComponent);
					}
				}
			} catch (FHIRException e) {
				e.printStackTrace();
				return null;
			}
		} 
		
		return medication;
	}
	
	@Override
	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
        if (or) paramWrapper.setUpperRelationship("or");
        else paramWrapper.setUpperRelationship("and");

        switch (parameter) {
		case Medication.SP_RES_ID:
			String medicationId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(medicationId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Medication.SP_CODE:
			String system = ((TokenParam) value).getSystem();
			String code = ((TokenParam) value).getValue();
			
			if ((system == null || system.isEmpty()) && (code == null || code.isEmpty()))
				break;
			
			String omopVocabulary = "None";
			if (system != null && !system.isEmpty()) {
				try {
					omopVocabulary = OmopCodeableConceptMapping.omopVocabularyforFhirUri(system);
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			} 

			paramWrapper.setParameterType("String");
			if ("None".equals(omopVocabulary) && code != null && !code.isEmpty()) {
				paramWrapper.setParameters(Arrays.asList("conceptCode"));
				paramWrapper.setOperators(Arrays.asList("like"));
				paramWrapper.setValues(Arrays.asList(code));
			} else if (!"None".equals(omopVocabulary) && (code == null || code.isEmpty())) {
				paramWrapper.setParameters(Arrays.asList("vocabulary.id"));
				paramWrapper.setOperators(Arrays.asList("like"));
				paramWrapper.setValues(Arrays.asList(omopVocabulary));				
			} else {
				paramWrapper.setParameters(Arrays.asList("vocabulary.id", "conceptCode"));
				paramWrapper.setOperators(Arrays.asList("like","like"));
				paramWrapper.setValues(Arrays.asList(omopVocabulary, code));
			}
			paramWrapper.setRelationship("and");
			mapList.add(paramWrapper);
			break;
		default:
			mapList = null;
		}
		
		return mapList;
	}
	
	final ParameterWrapper filterParam = new ParameterWrapper(
			"String",
			Arrays.asList("domain"),
			Arrays.asList("="),
			Arrays.asList("Drug"),
			"or"
			);

	@Override
	public Long getSize() {
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper> ();
		return getSize(paramList);
	}

	@Override
	public Long getSize(List<ParameterWrapper> paramList) {
		paramList.add(filterParam);

		return getMyOmopService().getSize(paramList);
	}

	@Override
	public void searchWithoutParams(int fromIndex, int toIndex, List<IBaseResource> listResources,
			List<String> includes, String sort) {

		// This is read all. But, since we will add an exception conditions to add filter.
		// we will call the search with params method.
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper> ();
		searchWithParams (fromIndex, toIndex, mapList, listResources, includes, sort);
	}

	@Override
	public void searchWithParams(int fromIndex, int toIndex, List<ParameterWrapper> mapList,
			List<IBaseResource> listResources, List<String> includes, String sort) {
		mapList.add(filterParam);

		List<Concept> entities = getMyOmopService().searchWithParams(fromIndex, toIndex, mapList, sort);

		for (Concept entity : entities) {
			Long omopId = entity.getIdAsLong();
			Long fhirId = IdMapping.getFHIRfromOMOP(omopId, getMyFhirResourceType());
			Medication fhirResource = constructResource(fhirId, entity, includes);
			if (fhirResource != null) {
				listResources.add(fhirResource);			
				// Do the rev_include and add the resource to the list.
				addRevIncludes(omopId, includes, listResources);
			}

		}
	}

	@Override
	public Concept constructOmop(Long omopId, Medication fhirResource) {
		// This is READ-ONLY. We are reading medication information from
		// concept table.
		return null;
	}
}
