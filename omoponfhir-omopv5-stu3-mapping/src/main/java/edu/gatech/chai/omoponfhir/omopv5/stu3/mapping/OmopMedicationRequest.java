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
import java.util.Date;
import java.util.List;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Dosage;
import org.hl7.fhir.dstu3.model.Duration;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.MedicationRequest.MedicationRequestDispenseRequestComponent;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.CodeableConceptUtil;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.EncounterResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.MedicationRequestResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.MedicationStatementResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.PatientResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.PractitionerResourceProvider;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.DrugExposureService;
import edu.gatech.chai.omopv5.dba.service.FPersonService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.ProviderService;
import edu.gatech.chai.omopv5.dba.service.VisitOccurrenceService;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.DrugExposure;
import edu.gatech.chai.omopv5.model.entity.FPerson;
import edu.gatech.chai.omopv5.model.entity.Provider;
import edu.gatech.chai.omopv5.model.entity.VisitOccurrence;

/**
 * 
 * @author mc142
 *
 * concept id	OHDSI drug type	FHIR
 * 38000179		Physician administered drug (identified as procedure), MedicationAdministration
 * 38000180		Inpatient administration, MedicationAdministration
 * 43542356	Physician administered drug (identified from EHR problem list), MedicationAdministration
 * 43542357	Physician administered drug (identified from referral record), MedicationAdministration
 * 43542358	Physician administered drug (identified from EHR observation), MedicationAdministration
 * 581373	Physician administered drug (identified from EHR order), MedicationAdministration
 * ******
 * 38000175	Prescription dispensed in pharmacy, MedicationDispense
 * 38000176	Prescription dispensed through mail order, MedicationDispense
 * 581452	Dispensed in Outpatient office, MedicationDispense
 * ******
 * 38000177	Prescription written, MedicationRequest
 * ******
 * 44787730	Patient Self-Reported Medication, MedicationStatement
 * 38000178	Medication list entry	 
 * 38000181	Drug era - 0 days persistence window	 
 * 38000182	Drug era - 30 days persistence window	 
 * 44777970	Randomized Drug	 
 */
public class OmopMedicationRequest extends BaseOmopResource<MedicationRequest, DrugExposure, DrugExposureService>
		implements IResourceMapping<MedicationRequest, DrugExposure> {

	public static Long MEDICATIONREQUEST_CONCEPT_TYPE_ID = 38000177L;
	private static OmopMedicationRequest omopMedicationRequest = new OmopMedicationRequest();
	private VisitOccurrenceService visitOccurrenceService;
	private ConceptService conceptService;
	private ProviderService providerService;
	private FPersonService fPersonService;

	public OmopMedicationRequest(WebApplicationContext context) {
		super(context, DrugExposure.class, DrugExposureService.class, MedicationRequestResourceProvider.getType());
		initialize(context);
	}
	
	public OmopMedicationRequest() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), DrugExposure.class, DrugExposureService.class, MedicationStatementResourceProvider.getType());
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}

	private void initialize(WebApplicationContext context) {
		visitOccurrenceService = context.getBean(VisitOccurrenceService.class);
		conceptService = context.getBean(ConceptService.class);
		providerService = context.getBean(ProviderService.class);
		fPersonService = context.getBean(FPersonService.class);
	}

	public static OmopMedicationRequest getInstance() {
		return OmopMedicationRequest.omopMedicationRequest;
	}
	
	@Override
	public Long toDbase(MedicationRequest fhirResource, IdType fhirId) throws FHIRException {
		Long omopId = null;
		DrugExposure drugExposure = null;
		if (fhirId != null) {
			omopId = IdMapping.getOMOPfromFHIR(fhirId.getIdPartAsLong(), MedicationRequestResourceProvider.getType());
		}

		drugExposure = constructOmop(omopId, fhirResource);

		Long retOmopId = null;
		if (omopId == null) {
			retOmopId = getMyOmopService().create(drugExposure).getId();
		} else {
			retOmopId = getMyOmopService().update(drugExposure).getId();
		}
		
		return IdMapping.getFHIRfromOMOP(retOmopId, MedicationStatementResourceProvider.getType());
	}

	@Override
	public MedicationRequest constructFHIR(Long fhirId, DrugExposure entity) {
		MedicationRequest medicationRequest = new MedicationRequest();
		
		medicationRequest.setId(new IdType(fhirId));
		
		// Subject from FPerson
		Reference patientRef = new Reference(new IdType(PatientResourceProvider.getType(), entity.getFPerson().getId()));
		patientRef.setDisplay(entity.getFPerson().getNameAsSingleString());
		medicationRequest.setSubject(patientRef);		
		
		// AuthoredOn. TODO: endDate is lost if we map to AuthoredOn.
		Date startDate = entity.getDrugExposureStartDate();
//		Date endDate = entity.getDrugExposureEndDate();
		if (startDate != null)
			medicationRequest.setAuthoredOn(startDate);
		
		// Get codeableconcept for medication
//		Concept omopDrugConcept = entity.getDrugConcept();
		CodeableConcept medicationCodeableConcept;
		try {
			medicationCodeableConcept = CodeableConceptUtil.getCodeableConceptFromOmopConcept(entity.getDrugConcept());
		} catch (FHIRException e1) {
			e1.printStackTrace();
			return null;
		}
		
		medicationRequest.setMedication(medicationCodeableConcept);

		// Get ingredient codeable concept for medication
//		Concept ingredient = conceptService.getIngredient(omopDrugConcept);
//
//		// If ingredient is available, then use contained. Otherwise
//		// use medicationCodeableConcept.
//		if (ingredient != null) {
//			CodeableConcept ingredientCodeableConcept;
//			try {
//				ingredientCodeableConcept = CodeableConceptUtil.getCodeableConceptFromOmopConcept(ingredient);
//				if (!ingredientCodeableConcept.isEmpty()) {
//					// We have ingredient information. Add this to MedicationStatement.
//					// To do this, we need to add Medication resource to contained section.
//					Medication medicationResource = new Medication();
//					medicationResource.setCode(medicationCodeableConcept);
//					MedicationIngredientComponent medIngredientComponent = new MedicationIngredientComponent();
//					medIngredientComponent.setItem(ingredientCodeableConcept);
//					medicationResource.addIngredient(medIngredientComponent);
//					medicationResource.setId("med1");
//					medicationRequest.addContained(medicationResource);
//					medicationRequest.setMedication(new Reference("#med1"));
//				}
//			} catch (FHIRException e) {
//				e.printStackTrace();
//				return null;
//			}
//		} else {
//			medicationRequest.setMedication(medicationCodeableConcept);
//		}

		// Dosage mapping
		Double dose = entity.getEffectiveDrugDose();
		SimpleQuantity doseQuantity = new SimpleQuantity();
		if (dose != null) {
			doseQuantity.setValue(dose);
		}
		
		Concept unitConcept = entity.getDoseUnitConcept();
		String unitUnit = null;
		String unitCode = null;
		String unitSystem = null;
		if (unitConcept != null) {
			String omopUnitVocab = unitConcept.getVocabulary().getId();
			String omopUnitCode = unitConcept.getConceptCode();
			String omopUnitName = unitConcept.getName();
			
			String fhirUnitUri;
			try {
				fhirUnitUri = OmopCodeableConceptMapping.fhirUriforOmopVocabulary(omopUnitVocab);
				if ("None".equals(fhirUnitUri)) {
					fhirUnitUri = unitConcept.getVocabulary().getVocabularyReference();
				}

				unitUnit = omopUnitName;
				unitCode = omopUnitCode;
				unitSystem = fhirUnitUri; 
			} catch (FHIRException e) {
				e.printStackTrace();
			}
		}
		
		if (!doseQuantity.isEmpty()) {
			doseQuantity.setUnit(unitUnit);
			doseQuantity.setCode(unitCode);
			doseQuantity.setSystem(unitSystem);
			
			Dosage dosage = new Dosage();
			dosage.setDose(doseQuantity);
			medicationRequest.addDosageInstruction(dosage);
		}

		// dispense request mapping.
		Integer refills = entity.getRefills();
		MedicationRequestDispenseRequestComponent dispenseRequest = new MedicationRequestDispenseRequestComponent();
		if (refills != null) {
			dispenseRequest.setNumberOfRepeatsAllowed(refills);
		}

		Double quantity = entity.getQuantity();
		if (quantity != null) {
			SimpleQuantity simpleQty = new SimpleQuantity();
			simpleQty.setValue(quantity);
			simpleQty.setUnit(unitUnit);
			simpleQty.setCode(unitCode);
			simpleQty.setSystem(unitSystem);
			dispenseRequest.setQuantity(simpleQty);
		}
		
		Integer daysSupply = entity.getDaysSupply();
		if (daysSupply != null) {
			Duration qty = new Duration();
			qty.setValue(daysSupply);
			// Set the UCUM unit to day.
			String fhirUri = OmopCodeableConceptMapping.UCUM.getFhirUri();
			qty.setSystem(fhirUri);
			qty.setCode("d");
			qty.setUnit("day");
			dispenseRequest.setExpectedSupplyDuration(qty);
		}
		
		if (!dispenseRequest.isEmpty()) {
			medicationRequest.setDispenseRequest(dispenseRequest);
		}
		
		// Recorder mapping
		Provider provider = entity.getProvider();
		if (provider != null) {
			Reference recorderReference = 
					new Reference(new IdType(PractitionerResourceProvider.getType(), provider.getId()));
			recorderReference.setDisplay(provider.getProviderName());
			medicationRequest.setRecorder(recorderReference);
		}
		
		// Context mapping
		VisitOccurrence visitOccurrence = entity.getVisitOccurrence();
		if (visitOccurrence != null) {
			Reference contextReference = 
					new Reference(new IdType(EncounterResourceProvider.getType(), visitOccurrence.getId()));
			medicationRequest.setContext(contextReference);
		}
		
		return medicationRequest;
	}
	
	@Override
	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
        if (or) paramWrapper.setUpperRelationship("or");
        else paramWrapper.setUpperRelationship("and");

		switch (parameter) {
		case MedicationRequest.SP_RES_ID:
			String medicationRequestId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(medicationRequestId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case MedicationRequest.SP_CODE:
			String system = ((TokenParam) value).getSystem();
			String code = ((TokenParam) value).getValue();
			
			if ((system == null || system.isEmpty()) && (code == null || code.isEmpty()))
				break;
			
			String omopVocabulary = "None";
			if (system != null && !system.isEmpty()) {
				try {
//					omopVocabulary = OmopCodeableConceptMapping.omopVocabularyforFhirUri(system);
					omopVocabulary = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(system);
				} catch (FHIRException e) {
					e.printStackTrace();
					break;
				}
			}

			paramWrapper.setParameterType("String");
			if ("None".equals(omopVocabulary) && code != null && !code.isEmpty()) {
				paramWrapper.setParameters(Arrays.asList("drugConcept.conceptCode"));
				paramWrapper.setOperators(Arrays.asList("like"));
				paramWrapper.setValues(Arrays.asList(code));
				paramWrapper.setRelationship("or");
			} else if (!"None".equals(omopVocabulary) && (code == null || code.isEmpty())) {
				paramWrapper.setParameters(Arrays.asList("drugConcept.vocabulary.id"));
				paramWrapper.setOperators(Arrays.asList("like"));
				paramWrapper.setValues(Arrays.asList(omopVocabulary));
				paramWrapper.setRelationship("or");
			} else {
				paramWrapper.setParameters(Arrays.asList("drugConcept.vocabulary.id", "drugConcept.conceptCode"));
				paramWrapper.setOperators(Arrays.asList("like","like"));
				paramWrapper.setValues(Arrays.asList(omopVocabulary, code));
				paramWrapper.setRelationship("and");
			}
			mapList.add(paramWrapper);
			break;
		case MedicationRequest.SP_CONTEXT:
			Long fhirEncounterId = ((ReferenceParam) value).getIdPartAsLong();
			Long omopVisitOccurrenceId = IdMapping.getOMOPfromFHIR(fhirEncounterId, EncounterResourceProvider.getType());
//			String resourceName = ((ReferenceParam) value).getResourceType();
			
			// We support Encounter so the resource type should be Encounter.
			if (omopVisitOccurrenceId != null) {
				paramWrapper.setParameterType("Long");
				paramWrapper.setParameters(Arrays.asList("visitOccurrence.id"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(String.valueOf(omopVisitOccurrenceId)));
				paramWrapper.setRelationship("or");
				mapList.add(paramWrapper);
			}
			break;
		case MedicationRequest.SP_AUTHOREDON:
			DateParam authoredOnDataParam = ((DateParam) value);
			ParamPrefixEnum apiOperator = authoredOnDataParam.getPrefix();
			String sqlOperator = null;
			if (apiOperator.equals(ParamPrefixEnum.GREATERTHAN)) {
				sqlOperator = ">";
			} else if (apiOperator.equals(ParamPrefixEnum.GREATERTHAN_OR_EQUALS)) {
				sqlOperator = ">=";
			} else if (apiOperator.equals(ParamPrefixEnum.LESSTHAN)) {
				sqlOperator = "<";
			} else if (apiOperator.equals(ParamPrefixEnum.LESSTHAN_OR_EQUALS)) {
				sqlOperator = "<=";
			} else if (apiOperator.equals(ParamPrefixEnum.NOT_EQUAL)) {
				sqlOperator = "!=";
			} else {
				sqlOperator = "=";
			}
			Date authoredOnDate = authoredOnDataParam.getValue();
			
			paramWrapper.setParameterType("Date");
			paramWrapper.setParameters(Arrays.asList("drugExposureStartDate"));
			paramWrapper.setOperators(Arrays.asList(sqlOperator));
			paramWrapper.setValues(Arrays.asList(String.valueOf(authoredOnDate.getTime())));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case MedicationRequest.SP_PATIENT:
		case MedicationRequest.SP_SUBJECT:
			ReferenceParam patientReference = ((ReferenceParam) value);
			Long fhirPatientId = patientReference.getIdPartAsLong();
			Long omopPersonId = IdMapping.getOMOPfromFHIR(fhirPatientId, PatientResourceProvider.getType());

			String omopPersonIdString = String.valueOf(omopPersonId);
			
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("fPerson.id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(omopPersonIdString));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		default:
			mapList = null;
		}
		
		return mapList;
	}
	
	final ParameterWrapper filterParam = new ParameterWrapper(
			"Long",
			Arrays.asList("drugTypeConcept.id"),
			Arrays.asList("="),
			Arrays.asList(String.valueOf(OmopMedicationRequest.MEDICATIONREQUEST_CONCEPT_TYPE_ID)),
			"or"
			);

	@Override
	public Long getSize() {
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper> ();
		// call getSize with empty parameter list. The getSize will add filter parameter.
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
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper> ();
		searchWithParams (fromIndex, toIndex, paramList, listResources, includes, sort);
	}

	@Override
	public void searchWithParams(int fromIndex, int toIndex, List<ParameterWrapper> mapList,
			List<IBaseResource> listResources, List<String> includes, String sort) {
		mapList.add(filterParam);

		List<DrugExposure> entities = getMyOmopService().searchWithParams(fromIndex, toIndex, mapList, sort);

		for (DrugExposure entity : entities) {
			Long omopId = entity.getIdAsLong();
			Long fhirId = IdMapping.getFHIRfromOMOP(omopId, getMyFhirResourceType());
			MedicationRequest fhirResource = constructResource(fhirId, entity, includes);
			if (fhirResource != null) {
				listResources.add(fhirResource);			
				// Do the rev_include and add the resource to the list.
				addRevIncludes(omopId, includes, listResources);
			}

		}
	}

	@Override
	public DrugExposure constructOmop(Long omopId, MedicationRequest fhirResource) {
		DrugExposure drugExposure = null;
		if (omopId != null) {
			// Update
			drugExposure = getMyOmopService().findById(omopId);
			if (drugExposure == null) { 
				try {
					throw new FHIRException(fhirResource.getId()+" does not exist");
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}
		} else {
			// Create
			List<Identifier> identifiers = fhirResource.getIdentifier();
			for (Identifier identifier: identifiers) {
				if (identifier.isEmpty()) continue;
				String identifierValue = identifier.getValue();
				List<DrugExposure> results = getMyOmopService().searchByColumnString("drugSourceValue", identifierValue);
				if (results.size() > 0) {
					drugExposure = results.get(0);
					omopId = drugExposure.getId();
					break;
				}
			}
			
			if (drugExposure == null) {
				drugExposure = new DrugExposure();
				// Add the source column.
				Identifier identifier = fhirResource.getIdentifierFirstRep();
				if (!identifier.isEmpty()) {
					drugExposure.setDrugSourceValue(identifier.getValue());
				}
			}
		}
				
		// Set patient.
		 Reference patientReference = fhirResource.getSubject();
		if (patientReference == null)
			try {
				throw new FHIRException("Patient must exist.");
			} catch (FHIRException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		
		Long patientFhirId = patientReference.getReferenceElement().getIdPartAsLong();
		Long omopFPersonId = IdMapping.getOMOPfromFHIR(patientFhirId, PatientResourceProvider.getType());

		FPerson fPerson = fPersonService.findById(omopFPersonId);
		if (fPerson == null)
			try {
				throw new FHIRException("Patient/"+patientFhirId+" is not valid");
			} catch (FHIRException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		drugExposure.setFPerson(fPerson);

		// Get medication[x]
		Type medicationType = fhirResource.getMedication();
		Concept omopConcept = null;
		CodeableConcept medicationCodeableConcept = null;
		if (medicationType instanceof Reference) {
			// We may have reference.
			Reference medicationReference;
			try {
				medicationReference = fhirResource.getMedicationReference();
				if (medicationReference.isEmpty()) {
					// This is an error. We require this.
					throw new FHIRException("Medication[CodeableConcept or Reference] is missing");
				} else {
					String medicationReferenceId = medicationReference.getReferenceElement().getIdPart();
					if (medicationReference.getReferenceElement().isLocal()) {
						List<Resource> contains = fhirResource.getContained();
						for (Resource resource: contains) {
							if (!resource.isEmpty() &&
								resource.getIdElement().getIdPart().equals(medicationReferenceId)) {

								// This must medication resource. 
								Medication medicationResource = (Medication) resource;
								medicationCodeableConcept = medicationResource.getCode();
								break;
							}
						}
					} else {
						throw new FHIRException("Medication Reference must have the medication in the contained");
					}
				}			} catch (FHIRException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			try {
				medicationCodeableConcept = fhirResource.getMedicationCodeableConcept();
			} catch (FHIRException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (medicationCodeableConcept == null || medicationCodeableConcept.isEmpty()) { 		
			try {
				throw new FHIRException("Medication[CodeableConcept or Reference] could not be mapped");
			} catch (FHIRException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			omopConcept = CodeableConceptUtil.searchConcept(conceptService, medicationCodeableConcept);
			if (omopConcept == null) {
				throw new FHIRException("Medication[CodeableConcept or Reference] could not be found");
			} else {
				drugExposure.setDrugConcept(omopConcept);
			}
		} catch (FHIRException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Set drug exposure type
		Concept drugExposureType = new Concept();
		drugExposureType.setId(MEDICATIONREQUEST_CONCEPT_TYPE_ID);
		drugExposure.setDrugTypeConcept(drugExposureType);
				
		// Set start date from authored on date 
		Date authoredDate = fhirResource.getAuthoredOn();
		drugExposure.setDrugExposureStartDate(authoredDate);

		// Set VisitOccurrence 
		Reference encounterReference = fhirResource.getContext();
		if (encounterReference != null && !encounterReference.isEmpty()) {
			if (EncounterResourceProvider.getType()
					.equals(encounterReference.getReferenceElement().getResourceType())) {
				// Get fhirIDLong.
				Long fhirEncounterIdLong = encounterReference.getReferenceElement().getIdPartAsLong();
				Long omopEncounterId = IdMapping.getOMOPfromFHIR(fhirEncounterIdLong, EncounterResourceProvider.getType());
				if (omopEncounterId != null) {
					VisitOccurrence visitOccurrence = visitOccurrenceService.findById(omopEncounterId);
					if (visitOccurrence != null)
						drugExposure.setVisitOccurrence(visitOccurrence);
				} else {
					try {
						throw new FHIRException("Encounter/"+fhirEncounterIdLong+" is not valid.");
					} catch (FHIRException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}		

		// dosageInstruction
		List<Dosage> dosageInstructions = fhirResource.getDosageInstruction();
		for (Dosage dosageInstruction: dosageInstructions) {
			SimpleQuantity doseQty;
			try {
				doseQty = dosageInstruction.getDoseSimpleQuantity();
				if (doseQty.isEmpty()) continue;
				drugExposure.setEffectiveDrugDose(doseQty.getValue().doubleValue());
				String doseCode = doseQty.getCode();
				String doseSystem = doseQty.getSystem();
				String vocabId = OmopCodeableConceptMapping.omopVocabularyforFhirUri(doseSystem);
				Concept unitConcept = 
						CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, vocabId, doseCode);
				drugExposure.setDoseUnitConcept(unitConcept);
				break;
			} catch (FHIRException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// dispense request 
		MedicationRequestDispenseRequestComponent dispenseRequest = fhirResource.getDispenseRequest();
		if (dispenseRequest != null && !dispenseRequest.isEmpty()) {
			Integer refills = dispenseRequest.getNumberOfRepeatsAllowed();
			if (refills != null) {
				drugExposure.setRefills(refills);
			}

			SimpleQuantity qty = dispenseRequest.getQuantity();
			if (qty != null) {
				drugExposure.setQuantity(qty.getValue().doubleValue());
				String doseCode = qty.getCode();
				String doseSystem = qty.getSystem();
				String vocabId;
				try {
					vocabId = OmopCodeableConceptMapping.omopVocabularyforFhirUri(doseSystem);
					Concept unitConcept = 
							CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, vocabId, doseCode);
					drugExposure.setDoseUnitConcept(unitConcept);
				} catch (FHIRException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		Reference practitionerRef = fhirResource.getRecorder();
		if (practitionerRef != null && !practitionerRef.isEmpty()) {
			Long fhirPractitionerIdLong = 
					practitionerRef.getReferenceElement().getIdPartAsLong();
			Long omopProviderId = IdMapping.getOMOPfromFHIR(fhirPractitionerIdLong, PractitionerResourceProvider.getType());
			Provider provider = providerService.findById(omopProviderId);
			if (provider != null) {
				drugExposure.setProvider(provider);
			}			
		}
		
		return drugExposure;
	}
}
