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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Observation.ObservationComponentComponent;
import org.hl7.fhir.dstu3.model.Observation.ObservationReferenceRangeComponent;
import org.hl7.fhir.dstu3.model.Observation.ObservationStatus;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.CodeableConceptUtil;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.EncounterResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.ObservationResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.PatientResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.PractitionerResourceProvider;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.FObservationViewService;
import edu.gatech.chai.omopv5.dba.service.FactRelationshipService;
import edu.gatech.chai.omopv5.dba.service.MeasurementService;
import edu.gatech.chai.omopv5.dba.service.NoteService;
import edu.gatech.chai.omopv5.dba.service.ObservationService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.VisitOccurrenceService;
import edu.gatech.chai.omopv5.model.entity.BaseEntity;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.FObservationView;
import edu.gatech.chai.omopv5.model.entity.FPerson;
import edu.gatech.chai.omopv5.model.entity.FactRelationship;
import edu.gatech.chai.omopv5.model.entity.Measurement;
import edu.gatech.chai.omopv5.model.entity.Note;
import edu.gatech.chai.omopv5.model.entity.VisitOccurrence;

public class OmopObservation extends BaseOmopResource<Observation, FObservationView, FObservationViewService>
		implements IResourceMapping<Observation, FObservationView> {

	final static Logger logger = LoggerFactory.getLogger(OmopObservation.class);
	private static OmopObservation omopObservation = new OmopObservation();

	public static final long SYSTOLIC_CONCEPT_ID = 3004249L;
	public static final long DIASTOLIC_CONCEPT_ID = 3012888L;
	public static final String SYSTOLIC_LOINC_CODE = "8480-6";
	public static final String DIASTOLIC_LOINC_CODE = "8462-4";
	public static final String BP_SYSTOLIC_DIASTOLIC_CODE = "85354-9";
	public static final String BP_SYSTOLIC_DIASTOLIC_DISPLAY = "Blood pressure systolic & diastolic";

	private ConceptService conceptService;
	private MeasurementService measurementService;
	private ObservationService observationService;
	private VisitOccurrenceService visitOccurrenceService;
	private NoteService noteService;
	private FactRelationshipService factRelationshipService;

	public OmopObservation(WebApplicationContext context) {
		super(context, FObservationView.class, FObservationViewService.class, ObservationResourceProvider.getType());
		initialize(context);
	}

	public OmopObservation() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), FObservationView.class,
				FObservationViewService.class, ObservationResourceProvider.getType());
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}

	private void initialize(WebApplicationContext context) {
		// Get bean for other services that we need for mapping.
		conceptService = context.getBean(ConceptService.class);
		measurementService = context.getBean(MeasurementService.class);
		observationService = context.getBean(ObservationService.class);
		visitOccurrenceService = context.getBean(VisitOccurrenceService.class);
		noteService = context.getBean(NoteService.class);
		factRelationshipService = context.getBean(FactRelationshipService.class);
	}

	public Long getDiastolicConcept() {
		return OmopObservation.DIASTOLIC_CONCEPT_ID;
	}

	public static OmopObservation getInstance() {
		return OmopObservation.omopObservation;
	}

	@Override
	public Observation constructFHIR(Long fhirId, FObservationView fObservationView) {
		Observation observation = new Observation();
		observation.setId(new IdType(fhirId));

		String omopVocabulary = fObservationView.getObservationConcept().getVocabulary().getId();
		String systemUriString = fhirOmopVocabularyMap.getFhirSystemNameFromOmopVocabulary(omopVocabulary);
		if ("None".equals(systemUriString)) {
			// If we can't find FHIR Uri or system name, just use Omop Vocabulary Id.
			systemUriString = omopVocabulary;
		}

		// If we have unit, this should be used across all the value.
		String unitSystemUri = null;
		String unitCode = null;
		String unitUnit = null;
		String unitSource = null;
		Concept unitConcept = fObservationView.getUnitConcept();
		if (unitConcept == null || unitConcept.getId() == 0L) {
			// see if we can get the unit from source column.
			unitSource = fObservationView.getUnitSourceValue();
			if (unitSource != null && !unitSource.isEmpty()) {
				unitConcept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, OmopCodeableConceptMapping.UCUM.getOmopVocabulary(), unitSource);
			}
		}
		
		if (unitConcept != null && unitConcept.getId() != 0L) {
			String omopUnitVocabularyId = unitConcept.getVocabulary().getId();
			unitSystemUri = fhirOmopVocabularyMap.getFhirSystemNameFromOmopVocabulary(omopUnitVocabularyId);
			if ("None".equals(unitSystemUri)) {
				unitSystemUri = omopUnitVocabularyId;
			}

			unitUnit = unitConcept.getName();
			unitCode = unitConcept.getConceptCode();
		} 

		String codeString = fObservationView.getObservationConcept().getConceptCode();
		String displayString;
		if (fObservationView.getObservationConcept().getId() == 0L) {
			displayString = fObservationView.getSourceValue();
		} else {
			displayString = fObservationView.getObservationConcept().getName();
		}

		// OMOP database maintains Systolic and Diastolic Blood Pressures
		// separately.
		// FHIR however keeps them together. Observation DAO filters out
		// Diastolic values.
		// Here, when we are reading systolic, we search for matching diastolic
		// and put them
		// together. The Observation ID will be systolic's OMOP ID.
		// public static final Long SYSTOLIC_CONCEPT_ID = new Long(3004249);
		// public static final Long DIASTOLIC_CONCEPT_ID = new Long(3012888);
		if (OmopObservation.SYSTOLIC_CONCEPT_ID == fObservationView.getObservationConcept().getId()) {
			// Set coding for systolic and diastolic observation
			systemUriString = OmopCodeableConceptMapping.LOINC.getFhirUri();
			codeString = BP_SYSTOLIC_DIASTOLIC_CODE;
			displayString = BP_SYSTOLIC_DIASTOLIC_DISPLAY;

			List<ObservationComponentComponent> components = new ArrayList<ObservationComponentComponent>();
			// First we add systolic component.
			ObservationComponentComponent comp = new ObservationComponentComponent();
			Coding coding = new Coding(systemUriString, fObservationView.getObservationConcept().getConceptCode(),
					fObservationView.getObservationConcept().getName());
			CodeableConcept componentCode = new CodeableConcept();
			componentCode.addCoding(coding);
			comp.setCode(componentCode);

			if (fObservationView.getValueAsNumber() != null) {
				Quantity quantity = new Quantity(fObservationView.getValueAsNumber().doubleValue());

				// Unit is defined as a concept code in omop v4, then unit and
				// code are the same in this case
				if (unitSystemUri != null || unitCode != null || unitUnit != null) {
					quantity.setUnit(unitUnit);
					quantity.setCode(unitCode);
					quantity.setSystem(unitSystemUri);
					comp.setValue(quantity);
				} else {
					if (unitSource != null) {
						quantity.setUnit(unitSource);
					}
				}
			}
			components.add(comp);

			// Now search for diastolic component.
			WebApplicationContext myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
			FObservationViewService myService = myAppCtx.getBean(FObservationViewService.class);
			FObservationView diastolicDb = myService.findDiastolic(DIASTOLIC_CONCEPT_ID,
					fObservationView.getFPerson().getId(), fObservationView.getDate(), fObservationView.getTime());
			if (diastolicDb != null) {
				comp = new ObservationComponentComponent();
				coding = new Coding(systemUriString, diastolicDb.getObservationConcept().getConceptCode(),
						diastolicDb.getObservationConcept().getName());
				componentCode = new CodeableConcept();
				componentCode.addCoding(coding);
				comp.setCode(componentCode);

				if (diastolicDb.getValueAsNumber() != null) {
					Quantity quantity = new Quantity(diastolicDb.getValueAsNumber().doubleValue());
					// Unit is defined as a concept code in omop v4, then unit
					// and code are the same in this case
					if (diastolicDb.getUnitConcept() != null && diastolicDb.getUnitConcept().getId() != 0L) {
						quantity.setUnit(diastolicDb.getUnitConcept().getName());
						quantity.setCode(diastolicDb.getUnitConcept().getConceptCode());
						String unitSystem = fhirOmopVocabularyMap.getFhirSystemNameFromOmopVocabulary(
								diastolicDb.getUnitConcept().getVocabulary().getId());
						if ("None".equals(unitSystem))
							unitSystem = diastolicDb.getUnitConcept().getVocabulary().getId();
						quantity.setSystem(unitSystem);
						comp.setValue(quantity);
					} else {
						String diastolicUnitSource = diastolicDb.getUnitSourceValue();
						if (diastolicUnitSource != null && !diastolicUnitSource.isEmpty()) {
							Concept diastolicUnitConcept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, OmopCodeableConceptMapping.UCUM.getOmopVocabulary(), unitSource);
							if (diastolicUnitConcept != null && diastolicUnitConcept.getId() != 0L) {
								quantity.setUnit(diastolicUnitConcept.getName());
								quantity.setCode(diastolicUnitConcept.getConceptCode());
								String unitSystem = fhirOmopVocabularyMap.getFhirSystemNameFromOmopVocabulary(
										diastolicUnitConcept.getVocabulary().getId());
								if ("None".equals(unitSystem))
									unitSystem = diastolicUnitConcept.getVocabulary().getId();
								quantity.setSystem(unitSystem);
							} else {
								quantity.setUnit(diastolicUnitSource);
							}
							comp.setValue(quantity);								
						}
					}
				}
				components.add(comp);
			}

			if (components.size() > 0) {
				observation.setComponent(components);
			}
		} else {
			if (fObservationView.getValueAsNumber() != null) {
				Quantity quantity = new Quantity(fObservationView.getValueAsNumber().doubleValue());
				if (unitSystemUri != null || unitCode != null || unitUnit != null) {
					quantity.setUnit(unitUnit);
					quantity.setCode(unitCode);
					quantity.setSystem(unitSystemUri);
				} else {
					if (unitSource != null) {
						quantity.setUnit(unitSource);
					}
				}
				
//				if (fObservationView.getUnitConcept() != null) {
//					// Unit is defined as a concept code in omop v4, then unit
//					// and code are the same in this case
//					quantity.setUnit(unitUnit);
//					quantity.setCode(unitCode);
//					quantity.setSystem(unitSystemUri);
//				}
				observation.setValue(quantity);
			} else if (fObservationView.getValueAsString() != null) {
				observation.setValue(new StringType(fObservationView.getValueAsString()));
			} else if (fObservationView.getValueAsConcept() != null
					&& fObservationView.getValueAsConcept().getId() != 0L) {
				// vocabulary is a required attribute for concept, then it's
				// expected to not be null
				String valueSystem = fhirOmopVocabularyMap.getFhirSystemNameFromOmopVocabulary(
						fObservationView.getValueAsConcept().getVocabulary().getId());
				if ("None".equals(valueSystem))
					valueSystem = fObservationView.getValueAsConcept().getVocabulary().getId();
				Coding coding = new Coding(valueSystem, fObservationView.getValueAsConcept().getConceptCode(),
						fObservationView.getValueAsConcept().getName());
				CodeableConcept valueAsConcept = new CodeableConcept();
				valueAsConcept.addCoding(coding);
				observation.setValue(valueAsConcept);
			} else {
				observation.setValue(new StringType(fObservationView.getValueSourceValue()));
			}
		}

		if (fObservationView.getRangeLow() != null) {
			SimpleQuantity low = new SimpleQuantity();
			low.setValue(fObservationView.getRangeLow().doubleValue());
			low.setSystem(unitSystemUri);
			low.setCode(unitCode);
			low.setUnit(unitUnit);
			observation.getReferenceRangeFirstRep().setLow(low);
		}
		if (fObservationView.getRangeHigh() != null) {
			SimpleQuantity high = new SimpleQuantity();
			high.setValue(fObservationView.getRangeHigh().doubleValue());
			high.setSystem(unitSystemUri);
			high.setCode(unitCode);
			high.setUnit(unitUnit);
			observation.getReferenceRangeFirstRep().setHigh(high);
		}

		Coding resourceCoding = new Coding(systemUriString, codeString, displayString);
		CodeableConcept code = new CodeableConcept();
		code.addCoding(resourceCoding);
		observation.setCode(code);

		observation.setStatus(ObservationStatus.FINAL);

		if (fObservationView.getDate() != null) {
			Date myDate = createDateTime(fObservationView);
			if (myDate != null) {
				DateTimeType appliesDate = new DateTimeType(myDate);
				observation.setEffective(appliesDate);
			}
		}
		if (fObservationView.getFPerson() != null) {
			Reference personRef = new Reference(
					new IdType(PatientResourceProvider.getType(), fObservationView.getFPerson().getId()));
			personRef.setDisplay(fObservationView.getFPerson().getNameAsSingleString());
			observation.setSubject(personRef);
		}
		if (fObservationView.getVisitOccurrence() != null)
			observation.getContext().setReferenceElement(
					new IdType(EncounterResourceProvider.getType(), fObservationView.getVisitOccurrence().getId()));

		if (fObservationView.getTypeConcept() != null) {
			if (fObservationView.getTypeConcept().getId() == 44818701L) {
				// This is From physical examination.

				CodeableConcept typeConcept = new CodeableConcept();
				Coding typeCoding = new Coding("http://hl7.org/fhir/observation-category", "exam", "");
				typeConcept.addCoding(typeCoding);
				observation.addCategory(typeConcept);
			} else if (fObservationView.getTypeConcept().getId() == 44818702L
					|| fObservationView.getTypeConcept().getId() == 44791245L) {
				CodeableConcept typeConcept = new CodeableConcept();
				// This is Lab result
				Coding typeCoding = new Coding("http://hl7.org/fhir/observation-category", "laboratory", "");
				typeConcept.addCoding(typeCoding);
				observation.addCategory(typeConcept);
			} else if (fObservationView.getTypeConcept().getId() == 45905771L) {
				CodeableConcept typeConcept = new CodeableConcept();
				// This is Lab result
				Coding typeCoding = new Coding("http://hl7.org/fhir/observation-category", "survey", "");
				typeConcept.addCoding(typeCoding);
				observation.addCategory(typeConcept);
			} else if (fObservationView.getTypeConcept().getId() == 38000277L
					|| fObservationView.getTypeConcept().getId() == 38000278L) {
				CodeableConcept typeConcept = new CodeableConcept();
				// This is Lab result
				Coding typeCoding = new Coding("http://hl7.org/fhir/observation-category", "laboratory", "");
				typeConcept.addCoding(typeCoding);
				observation.addCategory(typeConcept);
			} else if (fObservationView.getTypeConcept().getId() == 38000280L
					|| fObservationView.getTypeConcept().getId() == 38000281L) {
				CodeableConcept typeConcept = new CodeableConcept();
				// This is Lab result
				Coding typeCoding = new Coding("http://hl7.org/fhir/observation-category", "exam", "");
				typeConcept.addCoding(typeCoding);
				observation.addCategory(typeConcept);
			}
		}

		if (fObservationView.getProvider() != null) {
			Reference performerRef = new Reference(
					new IdType(PractitionerResourceProvider.getType(), fObservationView.getProvider().getId()));
			String providerName = fObservationView.getProvider().getProviderName();
			if (providerName != null && !providerName.isEmpty())
				performerRef.setDisplay(providerName);
			observation.addPerformer(performerRef);
		}

		String identifierString = fObservationView.getSourceValue();
		if (identifierString != null && !identifierString.isEmpty()) {
			Identifier identifier = new Identifier();
			identifier.setValue(identifierString);
			observation.addIdentifier(identifier);
		}

		if (fObservationView.getId() > 0) {
			List<BaseEntity> methods = factRelationshipService.searchMeasurementUsingMethod(fObservationView.getId());
			if (methods != null && methods.size() > 0) {
				for (BaseEntity method : methods) {
					if (method instanceof Note) {
						Note note = (Note) method;
						String methodString = noteService.findById(note.getId()).getNoteText();
						CodeableConcept methodCodeable = new CodeableConcept();
						methodCodeable.setText(methodString);
						observation.setMethod(methodCodeable);
					} else if (method instanceof Concept) {
						Concept concept = (Concept) method;
						CodeableConcept methodCodeable = CodeableConceptUtil
								.getCodeableConceptFromOmopConcept(conceptService.findById(concept.getId()));
						observation.setMethod(methodCodeable);
					} else {
						logger.error("Method couldn't be retrieved. Method class type undefined");
					}
				}
			}

			List<Note> notes = factRelationshipService.searchMeasurementContainsComments(fObservationView.getId());
			String comments = "";
			for (Note note : notes) {
				comments = comments.concat(noteService.findById(note.getId()).getNoteText());
			}
			if (!comments.isEmpty()) {
				observation.setComment(comments);
			}
		}

		return observation;
	}

	// @Override
	// public Observation constructResource(Long fhirId, FObservationView
	// entity, List<String> includes) {
	// Observation observation = constructFHIR(fhirId, entity);
	//
	// return observation;
	// }

	private List<Measurement> HandleBloodPressure(Long omopId, Observation fhirResource) {
		List<Measurement> retVal = new ArrayList<Measurement>();

		// This is measurement. And, fhirId is for systolic.
		// And, for update, we need to find diastolic and update that as well.
		Measurement systolicMeasurement = null;
		Measurement diastolicMeasurement = null;

		if (omopId != null) {
			Measurement measurement = measurementService.findById(omopId);
			if (measurement == null) {
				try {
					throw new FHIRException(
							"Couldn't find the matching resource, " + fhirResource.getIdElement().asStringValue());
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}

			if (measurement.getMeasurementConcept().getId() == SYSTOLIC_CONCEPT_ID) {
				systolicMeasurement = measurement;
			}

			if (measurement.getMeasurementConcept().getId() == DIASTOLIC_CONCEPT_ID) {
				diastolicMeasurement = measurement;
			}
		}

		// String identifier_value = null;
		// List<Identifier> identifiers = fhirResource.getIdentifier();
		// for (Identifier identifier : identifiers) {
		// identifier_value = identifier.getValue();
		// List<Measurement> measurements =
		// measurementService.searchByColumnString("sourceValue",
		// identifier_value);
		//
		// for (Measurement measurement : measurements) {
		// if (systolicMeasurement == null &&
		// measurement.getMeasurementConcept().getId() == SYSTOLIC_CONCEPT_ID) {
		// systolicMeasurement = measurement;
		// }
		// if (diastolicMeasurement == null
		// && measurement.getMeasurementConcept().getId() ==
		// DIASTOLIC_CONCEPT_ID) {
		// diastolicMeasurement = measurement;
		// }
		// }
		// if (systolicMeasurement != null && diastolicMeasurement != null)
		// break;
		// }

		Type systolicValue = null;
		Type diastolicValue = null;
		List<ObservationComponentComponent> components = fhirResource.getComponent();
		for (ObservationComponentComponent component : components) {
			List<Coding> codings = component.getCode().getCoding();
			for (Coding coding : codings) {
				String fhirSystem = coding.getSystem();
				String fhirCode = coding.getCode();

				if (OmopCodeableConceptMapping.LOINC.getFhirUri().equals(fhirSystem)
						&& SYSTOLIC_LOINC_CODE.equals(fhirCode)) {
					Type value = component.getValue();
					if (value != null && !value.isEmpty()) {
						systolicValue = value;
					}
				} else if (OmopCodeableConceptMapping.LOINC.getFhirUri().equals(fhirSystem)
						&& DIASTOLIC_LOINC_CODE.equals(fhirCode)) {
					Type value = component.getValue();
					if (value != null && !value.isEmpty()) {
						diastolicValue = value;
					}
				}
			}
		}

		if (systolicValue == null && diastolicValue == null) {
			try {
				throw new FHIRException("Either systolic or diastolic needs to be available in component");
			} catch (FHIRException e) {
				e.printStackTrace();
			}
		}

		Long fhirSubjectId = fhirResource.getSubject().getReferenceElement().getIdPartAsLong();
		Long omopPersonId = IdMapping.getOMOPfromFHIR(fhirSubjectId, PatientResourceProvider.getType());
		FPerson tPerson = new FPerson();
		tPerson.setId(omopPersonId);

		Identifier identifier_value = fhirResource.getIdentifierFirstRep();
		if (omopId == null) {
			// Create.
			if (systolicMeasurement == null && systolicValue != null) {
				systolicMeasurement = new Measurement();

				if (identifier_value != null && !identifier_value.isEmpty()) {
					systolicMeasurement.setSourceValue(identifier_value.getValue());
				}
//				systolicMeasurement.setSourceValue(SYSTOLIC_LOINC_CODE);
				systolicMeasurement.setFPerson(tPerson);

			}
			if (diastolicMeasurement == null && diastolicValue != null) {
				diastolicMeasurement = new Measurement();

				if (identifier_value != null) {
					diastolicMeasurement.setSourceValue(identifier_value.getValue());
				}
//				diastolicMeasurement.setSourceValue(DIASTOLIC_LOINC_CODE);
				diastolicMeasurement.setFPerson(tPerson);
			}

		} else {
			// Update
			// Sanity check. The entry found from identifier should have
			// matching id.
			try {
				if (systolicMeasurement != null) {
					if (systolicMeasurement.getId() != omopId) {
						throw new FHIRException("The systolic measurement has incorrect id or identifier.");
					}
				} else {
					// Now check if we have disastoic measurement.
					if (diastolicMeasurement != null) {
						// OK, originally, we had no systolic. Do the sanity
						// check
						// with diastolic measurement.
						if (diastolicMeasurement.getId() != omopId) {
							throw new FHIRException("The diastolic measurement has incorrect id or identifier.");
						}
					}
				}
			} catch (FHIRException e) {
				e.printStackTrace();
			}

			// Update. We use systolic measurement id as our prime id. However,
			// sometimes, there is a chance that only one is available.
			// If systolic is not available, diastolic will use the id.
			// Thus, we first need to check if
			if (systolicMeasurement == null) {
				if (systolicValue != null) {
					systolicMeasurement = measurementService.findById(omopId);
					systolicMeasurement.setFPerson(tPerson);
				}
			}
			if (diastolicMeasurement == null) {
				if (diastolicValue != null) {
					// We have diastolic value. But, we cannot use omopId here.
					//
					diastolicMeasurement = measurementService.findById(omopId);
					diastolicMeasurement.setFPerson(tPerson);
				}
			}

			if (systolicMeasurement == null && diastolicMeasurement == null) {
				try {
					throw new FHIRException("Failed to get either systolic or diastolic measurement for update.");
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}
		}

		// We look at component coding.
		if (systolicMeasurement != null) {
			Concept codeConcept = new Concept();
			codeConcept.setId(SYSTOLIC_CONCEPT_ID);
			systolicMeasurement.setMeasurementConcept(codeConcept);

			try {
				if (systolicValue instanceof Quantity) {
					systolicMeasurement.setValueAsNumber(((Quantity) systolicValue).getValue().doubleValue());

					// Save the unit in the unit source column to save the
					// source
					// value.
					String unitString = ((Quantity) systolicValue).getUnit();
					systolicMeasurement.setUnitSourceValue(unitString);

					String unitSystem = ((Quantity) systolicValue).getSystem();
					String unitCode = ((Quantity) systolicValue).getCode();
//					String omopVocabularyId = OmopCodeableConceptMapping.omopVocabularyforFhirUri(unitSystem);
					String omopVocabularyId = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(unitSystem);
					if (omopVocabularyId != null) {
						Concept unitConcept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService,
								omopVocabularyId, unitCode);
						systolicMeasurement.setUnitConcept(unitConcept);
					}
					systolicMeasurement.setValueSourceValue(((Quantity) systolicValue).getValue().toString());
				} else if (systolicValue instanceof CodeableConcept) {
					Concept systolicValueConcept = CodeableConceptUtil.searchConcept(conceptService,
							(CodeableConcept) systolicValue);
					systolicMeasurement.setValueAsConcept(systolicValueConcept);
					systolicMeasurement.setValueSourceValue(((CodeableConcept) systolicValue).toString());
				} else
					throw new FHIRException("Systolic measurement should be either Quantity or CodeableConcept");
			} catch (FHIRException e) {
				e.printStackTrace();
			}
		}

		if (diastolicMeasurement != null) {
			Concept codeConcept = new Concept();
			codeConcept.setId(DIASTOLIC_CONCEPT_ID);
			diastolicMeasurement.setMeasurementConcept(codeConcept);

			try {
				if (diastolicValue instanceof Quantity) {
					diastolicMeasurement.setValueAsNumber(((Quantity) diastolicValue).getValue().doubleValue());

					// Save the unit in the unit source column to save the
					// source
					// value.
					String unitString = ((Quantity) diastolicValue).getUnit();
					diastolicMeasurement.setUnitSourceValue(unitString);

					String unitSystem = ((Quantity) diastolicValue).getSystem();
					String unitCode = ((Quantity) diastolicValue).getCode();
//					String omopVocabularyId = OmopCodeableConceptMapping.omopVocabularyforFhirUri(unitSystem);
					String omopVocabularyId = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(unitSystem);
					if (omopVocabularyId != null) {
						Concept unitConcept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService,
								omopVocabularyId, unitCode);
						diastolicMeasurement.setUnitConcept(unitConcept);
					}
					diastolicMeasurement.setValueSourceValue(((Quantity) diastolicValue).getValue().toString());
				} else if (diastolicValue instanceof CodeableConcept) {
					Concept diastolicValueConcept = CodeableConceptUtil.searchConcept(conceptService,
							(CodeableConcept) diastolicValue);
					diastolicMeasurement.setValueAsConcept(diastolicValueConcept);
					diastolicMeasurement.setValueSourceValue(((CodeableConcept) diastolicValue).toString());
				} else
					throw new FHIRException("Diastolic measurement should be either Quantity or CodeableConcept");
			} catch (FHIRException e) {
				e.printStackTrace();
			}
		}

		// Get low and high range if available.
		// Components have two value. From the range list, we should
		// find the matching range. If exists, we can update measurement
		// entity class.
		List<ObservationReferenceRangeComponent> ranges = fhirResource.getReferenceRange();
		List<Coding> codings;

		// For BP, we should walk through these range references and
		// find a right matching one to put our measurement entries.
		for (ObservationReferenceRangeComponent range : ranges) {
			if (range.isEmpty())
				continue;

			// Get high and low values.
			SimpleQuantity highQtyValue = range.getHigh();
			SimpleQuantity lowQtyValue = range.getLow();
			if (highQtyValue.isEmpty() && lowQtyValue.isEmpty()) {
				// We need these values. If these are empty.
				// We have no reason to look at the appliesTo data.
				// Skip to next reference.
				continue;
			}

			// Check the all the included FHIR concept codes.
			List<CodeableConcept> rangeConceptCodes = range.getAppliesTo();
			for (CodeableConcept rangeConceptCode : rangeConceptCodes) {
				codings = rangeConceptCode.getCoding();
				for (Coding coding : codings) {
					try {
						if (OmopCodeableConceptMapping.LOINC.fhirUri.equals(coding.getSystem())) {
							if (SYSTOLIC_LOINC_CODE.equals(coding.getCode())) {
								// This applies to Systolic blood pressure.
								if (systolicMeasurement != null) {
									if (!highQtyValue.isEmpty()) {
										systolicMeasurement.setRangeHigh(highQtyValue.getValue().doubleValue());
									}
									if (!lowQtyValue.isEmpty()) {
										systolicMeasurement.setRangeLow(lowQtyValue.getValue().doubleValue());
									}
									break;
								} else {
									throw new FHIRException(
											"Systolic value is not available. But, range for systolic is provided. BP data inconsistent");
								}
							} else if (DIASTOLIC_LOINC_CODE.equals(coding.getCode())) {
								// This applies to Diastolic blood pressure.
								if (diastolicMeasurement != null) {
									if (!highQtyValue.isEmpty()) {
										diastolicMeasurement.setRangeHigh(highQtyValue.getValue().doubleValue());
									}
									if (!lowQtyValue.isEmpty()) {
										diastolicMeasurement.setRangeLow(lowQtyValue.getValue().doubleValue());
									}
									break;
								} else {
									throw new FHIRException(
											"Diastolic value is not available. But, range for diastolic is provided. BP data inconsistent");
								}
							}
						}
					} catch (FHIRException e) {
						e.printStackTrace();
					}
				}
			}
		}

		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		if (fhirResource.getEffective() instanceof DateTimeType) {
			Date date = ((DateTimeType) fhirResource.getEffective()).getValue();
			if (systolicMeasurement != null) {
				systolicMeasurement.setDate(date);
				systolicMeasurement.setTime(timeFormat.format(date));
			}
			if (diastolicMeasurement != null) {
				diastolicMeasurement.setDate(date);
				diastolicMeasurement.setTime(timeFormat.format(date));
			}
		} else if (fhirResource.getEffective() instanceof Period) {
			Date startDate = ((Period) fhirResource.getEffective()).getStart();
			if (startDate != null) {
				if (systolicMeasurement != null) {
					systolicMeasurement.setDate(startDate);
					systolicMeasurement.setTime(timeFormat.format(startDate));
				}
			}
			if (startDate != null) {
				if (diastolicMeasurement != null) {
					diastolicMeasurement.setDate(startDate);
					diastolicMeasurement.setTime(timeFormat.format(startDate));
				}
			}
		}

		/* Set visit occurrence */
		Reference contextReference = fhirResource.getContext();
		VisitOccurrence visitOccurrence = null;
		if (contextReference != null && !contextReference.isEmpty()) {
			if (contextReference.getReferenceElement().getResourceType().equals(EncounterResourceProvider.getType())) {
				// Encounter context.
				Long fhirEncounterId = contextReference.getReferenceElement().getIdPartAsLong();
				Long omopVisitOccurrenceId = IdMapping.getOMOPfromFHIR(fhirEncounterId,
						EncounterResourceProvider.getType());
				if (omopVisitOccurrenceId != null) {
					visitOccurrence = visitOccurrenceService.findById(omopVisitOccurrenceId);
				}
				if (visitOccurrence == null) {
					try {
						throw new FHIRException(
								"The Encounter (" + contextReference.getReference() + ") context couldn't be found.");
					} catch (FHIRException e) {
						e.printStackTrace();
					}
				} else {
					if (systolicMeasurement != null) {
						systolicMeasurement.setVisitOccurrence(visitOccurrence);
					}
					if (diastolicMeasurement != null) {
						diastolicMeasurement.setVisitOccurrence(visitOccurrence);
					}
				}
			} else {
				// Episode of Care context.
				// TODO: Do we have a mapping for the Episode of Care??
			}
		}

		List<CodeableConcept> categories = fhirResource.getCategory();
		Long typeConceptId = 0L;
		for (CodeableConcept category : categories) {
			codings = category.getCoding();
			for (Coding coding : codings) {
				String fhirSystem = coding.getSystem();
				String fhirCode = coding.getCode();
				if (fhirSystem == null || fhirSystem.isEmpty() || fhirCode == null || fhirCode.isEmpty()) {
					continue;
				}
				try {
					typeConceptId = OmopConceptMapping.omopForObservationCategoryCode(fhirCode);
				} catch (FHIRException e) {
					e.printStackTrace();
				}
				if (typeConceptId > 0L)
					break;
			}
			if (typeConceptId > 0L)
				break;
		}

		Concept typeConcept = new Concept();
		typeConcept.setId(typeConceptId);

		// Long retvalSystolic = null, retvalDiastolic = null;
		if (systolicMeasurement != null) {
			systolicMeasurement.setType(typeConcept);
			retVal.add(systolicMeasurement);

			// if (systolicMeasurement.getId() != null) {
			// retvalSystolic =
			// measurementService.update(systolicMeasurement).getId();
			// } else {
			// retvalSystolic =
			// measurementService.create(systolicMeasurement).getId();
			// }
		}
		if (diastolicMeasurement != null) {
			diastolicMeasurement.setType(typeConcept);
			retVal.add(diastolicMeasurement);

			// if (diastolicMeasurement.getId() != null) {
			// retvalDiastolic =
			// measurementService.update(diastolicMeasurement).getId();
			// } else {
			// retvalDiastolic =
			// measurementService.create(diastolicMeasurement).getId();
			// }
		}

		return retVal;
		// if (retvalSystolic != null)
		// return retvalSystolic;
		// else if (retvalDiastolic != null)
		// return retvalDiastolic;
		// else
		// return null;
	}

	@Override
	public Long removeByFhirId(IdType fhirId) {
		Long id_long_part = fhirId.getIdPartAsLong();
		Long myId = IdMapping.getOMOPfromFHIR(id_long_part, getMyFhirResourceType());
		if (myId < 0) {
			// This is observation table.
			return observationService.removeById(myId);
		} else {
			return measurementService.removeById(myId);
		}
	}

	public List<Measurement> constructOmopMeasurement(Long omopId, Observation fhirResource, String system,
			String codeString) {
		List<Measurement> retVal = new ArrayList<Measurement>();

		// If we have BP information, we handle this separately.
		// OMOP cannot handle multiple entries. So, we do not have
		// this code in our concept table.
		if (system != null && system.equals(OmopCodeableConceptMapping.LOINC.getFhirUri())
				&& codeString.equals(BP_SYSTOLIC_DIASTOLIC_CODE)) {
			// OK, we have BP systolic & diastolic. Handle this separately.
			// If successful, we will end and return.

			return HandleBloodPressure(omopId, fhirResource);
		}

		Measurement measurement = null;
		if (omopId == null) {
			// This is CREATE.
			measurement = new Measurement();
		} else {
			// This is UPDATE.
			measurement = measurementService.findById(omopId);
			if (measurement == null) {
				// We have no observation to update.
				try {
					throw new FHIRException("We have no matching FHIR Observation (Observation) to update.");
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}
		}

		Identifier identifier = fhirResource.getIdentifierFirstRep();
		if (identifier != null && !identifier.isEmpty()) {
			measurement.setSourceValue(identifier.getValue());
		}

		String idString = fhirResource.getSubject().getReferenceElement().getIdPart();

		try {
			Long fhirSubjectId = Long.parseLong(idString);
			Long omopPersonId = IdMapping.getOMOPfromFHIR(fhirSubjectId, PatientResourceProvider.getType());

			FPerson tPerson = new FPerson();
			tPerson.setId(omopPersonId);
			measurement.setFPerson(tPerson);
		} catch (Exception e) {
			// We have non-numeric id for the person. This should be handled later by
			// caller.
			e.printStackTrace();
		}

		// Get code system information.
		CodeableConcept code = fhirResource.getCode();

		// code should NOT be null as this is required field.
		// And, validation should check this.
		List<Coding> codings = code.getCoding();
		Coding codingFound = null;
		Coding codingSecondChoice = null;
		String omopSystem = null;
		String valueSourceString = null;
		for (Coding coding : codings) {
			String fhirSystemUri = coding.getSystem();
			// We prefer LOINC code. So, if we found one, we break out from
			// this loop
			if (code.getText() != null && !code.getText().isEmpty()) {
				valueSourceString = code.getText();
			} else {
				valueSourceString = coding.getSystem() + " " + coding.getCode() + " " + coding.getDisplay();
				valueSourceString = valueSourceString.trim();
			}

			if (fhirSystemUri != null && fhirSystemUri.equals(OmopCodeableConceptMapping.LOINC.getFhirUri())) {
				// Found the code we want.
				codingFound = coding;
				break;
			} else {
				// See if we can handle this coding.
				try {
					if (fhirSystemUri != null && !fhirSystemUri.isEmpty()) {
//						omopSystem = OmopCodeableConceptMapping.omopVocabularyforFhirUri(fhirSystemUri);
						omopSystem = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(fhirSystemUri);

						if ("None".equals(omopSystem) == false) {
							// We can at least handle this. Save it
							// We may find another one we can handle. Let it replace.
							// 2nd choice is just 2nd choice.
							codingSecondChoice = coding;
						}
					}
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}
		}

		// if (codingFound == null && codingSecondChoice == null) {
		// try {
		// throw new FHIRException("We couldn't support the code");
		// } catch (FHIRException e) {
		// e.printStackTrace();
		// }
		// }

		Concept concept = null;
		if (codingFound != null) {
			// Find the concept id for this coding.
			concept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService,
					OmopCodeableConceptMapping.LOINC.getOmopVocabulary(), codingFound.getCode());
//				if (concept == null) {
//					throw new FHIRException("We couldn't map the code - "
//							+ OmopCodeableConceptMapping.LOINC.getFhirUri() + ":" + codingFound.getCode());
//				}
		} else if (codingSecondChoice != null) {
			// This is not our first choice. But, found one that we can
			// map.
			concept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, omopSystem,
					codingSecondChoice.getCode());
//				if (concept == null) {
//					throw new FHIRException("We couldn't map the code - "
//							+ OmopCodeableConceptMapping.fhirUriforOmopVocabulary(omopSystem) + ":"
//							+ codingSecondChoice.getCode());
//				}
		} else {
			concept = null;
		}

		if (concept == null) {
			concept = conceptService.findById(0L);
		}

		measurement.setMeasurementConcept(concept);

		// Set this in the source column
		if (concept == null || concept.getIdAsLong() == 0L) {
			measurement.setValueSourceValue(valueSourceString);
		}

		if (concept != null)
			measurement.setSourceValueConcept(concept);

		/* Set the value of the observation */
		Type valueType = fhirResource.getValue();
		try {
			if (valueType instanceof Quantity) {
				measurement.setValueAsNumber(((Quantity) valueType).getValue().doubleValue());
				measurement.setValueSourceValue(String.valueOf(((Quantity) valueType).getValue()));

				// For unit, OMOP need unit concept
				String unitCode = ((Quantity) valueType).getCode();
				String unitSystem = ((Quantity) valueType).getSystem();

				String omopVocabulary = null;
				concept = null;
				if (unitCode != null && !unitCode.isEmpty()) {
					if (unitSystem == null || unitSystem.isEmpty()) {
						// If system is empty, then we check UCUM for the unit.
						omopVocabulary = OmopCodeableConceptMapping.UCUM.getOmopVocabulary();
					} else {
//						omopVocabulary = OmopCodeableConceptMapping.omopVocabularyforFhirUri(unitSystem);
						omopVocabulary = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(unitSystem);
					}
					concept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, omopVocabulary,
							unitCode);
				}

				// Save the unit in the unit source column to save the source
				// value.
				String unitString = ((Quantity) valueType).getUnit();
				measurement.setUnitSourceValue(unitString);

				if (concept != null) {
					// If we found the concept for unit, use it. Otherwise,
					// leave it empty.
					// We still have this in the unit source column.
					measurement.setUnitConcept(concept);
				}

			} else if (valueType instanceof CodeableConcept) {
				// We have coeable concept value. Get System and Value.
				// FHIR allows one value[x].
				codings = ((CodeableConcept) valueType).getCoding();
				concept = null;
				for (Coding coding : codings) {
					String fhirSystem = coding.getSystem();
					String fhirCode = coding.getCode();

					if (fhirSystem == null || fhirSystem.isEmpty() || fhirCode == null || fhirCode.isEmpty()) {
						continue;
					}

//					String omopVocabulary = OmopCodeableConceptMapping.omopVocabularyforFhirUri(fhirSystem);
					String omopVocabulary = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(fhirSystem);
					concept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, omopVocabulary,
							fhirCode);

					if (concept == null) {
						throw new FHIRException(
								"We couldn't map the codeable concept value - " + fhirSystem + ":" + fhirCode);
					}
					break;
				}
				if (concept == null) {
					throw new FHIRException("We couldn't find a concept to map the codeable concept value.");
				}

				measurement.setValueAsConcept(concept);
			} else if (valueType instanceof StringType) {
				String valueString = ((StringType) valueType).getValue();
				// Measurement table in OMOPv5 does not have a column for string value.
				// If the value is what we can recognize as a concept code, we will use it.
				if ("none detected".equalsIgnoreCase(valueString)) {
					measurement.setValueAsConcept(conceptService.findById(45878003L));
				} else if ("not detected".equalsIgnoreCase(valueString)) {
					measurement.setValueAsConcept(conceptService.findById(45880296L));
				} else if ("detected".equalsIgnoreCase(valueString)) {
					measurement.setValueAsConcept(conceptService.findById(45877985L));
				}

				measurement.setValueSourceValue(valueString);
			}
		} catch (FHIRException e) {
			e.printStackTrace();
		}

		// Get low and high range if available. This is only applicable to
		// measurement.
		if (!fhirResource.getReferenceRangeFirstRep().isEmpty()) {
			SimpleQuantity high = fhirResource.getReferenceRangeFirstRep().getHigh();
			if (!high.isEmpty()) {
				measurement.setRangeHigh(high.getValue().doubleValue());

				if (measurement.getUnitConcept() == null) {
					Concept rangeUnitConcept = null;
					if (high.getCode() != null && !high.getCode().isEmpty()) {
						String omopVocabulary;
						if (high.getSystem() == null || high.getSystem().isEmpty()) {
							// If system is empty, then we check UCUM for the unit.
							omopVocabulary = OmopCodeableConceptMapping.UCUM.getOmopVocabulary();
						} else {
							omopVocabulary = fhirOmopVocabularyMap
									.getOmopVocabularyFromFhirSystemName(high.getSystem());
						}
						rangeUnitConcept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService,
								omopVocabulary, high.getCode());
					}

					if (rangeUnitConcept != null) {
						measurement.setUnitConcept(rangeUnitConcept);
					}
				}
			}
			SimpleQuantity low = fhirResource.getReferenceRangeFirstRep().getLow();
			if (!low.isEmpty()) {
				measurement.setRangeLow(low.getValue().doubleValue());

				if (measurement.getUnitConcept() == null) {
					Concept rangeUnitConcept = null;
					if (low.getCode() != null && !low.getCode().isEmpty()) {
						String omopVocabulary;
						if (low.getSystem() == null || low.getSystem().isEmpty()) {
							// If system is empty, then we check UCUM for the unit.
							omopVocabulary = OmopCodeableConceptMapping.UCUM.getOmopVocabulary();
						} else {
							omopVocabulary = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(low.getSystem());
						}
						rangeUnitConcept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService,
								omopVocabulary, low.getCode());
					}

					if (rangeUnitConcept != null) {
						measurement.setUnitConcept(rangeUnitConcept);
					}
				}
			}
		}

		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		if (fhirResource.getEffective() instanceof DateTimeType) {
			Date date = ((DateTimeType) fhirResource.getEffective()).getValue();
			measurement.setDate(date);
			measurement.setTime(timeFormat.format(date));
		} else if (fhirResource.getEffective() instanceof Period) {
			Date startDate = ((Period) fhirResource.getEffective()).getStart();
			if (startDate != null) {
				measurement.setDate(startDate);
				measurement.setTime(timeFormat.format(startDate));
			}
		}
		/* Set visit occurrence */
		Reference contextReference = fhirResource.getContext();
		VisitOccurrence visitOccurrence = null;
		if (contextReference != null && !contextReference.isEmpty()) {
			if (contextReference.getReferenceElement().getResourceType().equals(EncounterResourceProvider.getType())) {
				// Encounter context.
				Long fhirEncounterId = contextReference.getReferenceElement().getIdPartAsLong();
				Long omopVisitOccurrenceId = IdMapping.getOMOPfromFHIR(fhirEncounterId,
						EncounterResourceProvider.getType());
				if (omopVisitOccurrenceId != null) {
					visitOccurrence = visitOccurrenceService.findById(omopVisitOccurrenceId);
				}
				if (visitOccurrence == null) {
					try {
						throw new FHIRException(
								"The Encounter (" + contextReference.getReference() + ") context couldn't be found.");
					} catch (FHIRException e) {
						e.printStackTrace();
					}
				} else {
					measurement.setVisitOccurrence(visitOccurrence);
				}
			} else {
				// Episode of Care context.
				// TODO: Do we have a mapping for the Episode of Care??
			}
		}

		List<CodeableConcept> categories = fhirResource.getCategory();
		Long typeConceptId = 0L;
		for (CodeableConcept category : categories) {
			codings = category.getCoding();
			for (Coding coding : codings) {
				String fhirSystem = coding.getSystem();
				String fhirCode = coding.getCode();
				if (fhirSystem == null || fhirSystem.isEmpty() || fhirCode == null || fhirCode.isEmpty()) {
					continue;
				}
				try {
					typeConceptId = OmopConceptMapping.omopForObservationCategoryCode(fhirCode);
				} catch (FHIRException e) {
					e.printStackTrace();
				}
				if (typeConceptId > 0L)
					break;
			}
			if (typeConceptId > 0L)
				break;
		}

		concept = new Concept();
		concept.setId(typeConceptId);
		measurement.setType(concept);

		retVal.add(measurement);

		return retVal;

	}

	public edu.gatech.chai.omopv5.model.entity.Observation constructOmopObservation(Long omopId,
			Observation fhirResource) {
		edu.gatech.chai.omopv5.model.entity.Observation observation = null;
		if (omopId == null) {
			// This is CREATE.
			observation = new edu.gatech.chai.omopv5.model.entity.Observation();
		} else {
			observation = observationService.findById(omopId);
			if (observation == null) {
				// We have no observation to update.
				try {
					throw new FHIRException("We have no matching FHIR Observation (Observation) to update.");
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}
		}

		Identifier identifier = fhirResource.getIdentifierFirstRep();
		if (identifier != null && !identifier.isEmpty()) {
			// This will be overwritten if we fail to get code mapped to concept id.
			observation.setSourceValue(identifier.getValue());
		}

		Long fhirSubjectId = fhirResource.getSubject().getReferenceElement().getIdPartAsLong();
		Long omopPersonId = IdMapping.getOMOPfromFHIR(fhirSubjectId, PatientResourceProvider.getType());

		FPerson tPerson = new FPerson();
		tPerson.setId(omopPersonId);
		observation.setFPerson(tPerson);

		CodeableConcept code = fhirResource.getCode();

		// code should NOT be null as this is required field.
		// And, validation should check this.
		List<Coding> codings = code.getCoding();
		Coding codingFound = null;
		Coding codingSecondChoice = null;
		String OmopSystem = null;
		String valueSourceString = null;
		for (Coding coding : codings) {
			String fhirSystemUri = coding.getSystem();

			if (code.getText() != null && !code.getText().isEmpty()) {
				valueSourceString = code.getText();
			} else {
				valueSourceString = coding.getSystem() + " " + coding.getCode() + " " + coding.getDisplay();
				valueSourceString = valueSourceString.trim();
			}

			if (fhirSystemUri.equals(OmopCodeableConceptMapping.LOINC.getFhirUri())) {
				// Found the code we want, which is LOINC
				codingFound = coding;
				break;
			} else {
				// See if we can handle this coding.
				try {
					if (fhirSystemUri != null && !fhirSystemUri.isEmpty()) {
//						OmopSystem = OmopCodeableConceptMapping.omopVocabularyforFhirUri(fhirSystemUri);
						OmopSystem = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(fhirSystemUri);
						if ("None".equals(OmopSystem) == false) {
							// We can at least handle this. Save it
							// We may find another one we can handle. Let it replace.
							// 2nd choice is just 2nd choice.
							codingSecondChoice = coding;
						}
					}
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}
		}

		// if (codingFound == null && codingSecondChoice == null) {
		// // We can't save this resource to OMOP.. sorry...
		// try {
		// throw new FHIRException("We couldn't support the code");
		// } catch (FHIRException e) {
		// e.printStackTrace();
		// }
		// }

		Concept concept = null;
		if (codingFound != null) {
			// Find the concept id for this coding.
			concept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService,
					OmopCodeableConceptMapping.LOINC.getOmopVocabulary(), codingFound.getCode());
//				if (concept == null) {
//					throw new FHIRException("We couldn't map the code - "
//							+ OmopCodeableConceptMapping.LOINC.getFhirUri() + ":" + codingFound.getCode());
//				}
		}
		if (codingSecondChoice != null) {
			// This is not our first choice. But, found one that we can
			// map.
			concept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, OmopSystem,
					codingSecondChoice.getCode());
//				if (concept == null) {
//					throw new FHIRException("We couldn't map the code - "
//							+ OmopCodeableConceptMapping.fhirUriforOmopVocabulary(OmopSystem) + ":"
//							+ codingSecondChoice.getCode());
//				}
		} else {
			concept = null;
		}

		if (concept == null) {
			concept = conceptService.findById(0L);
		}

		observation.setObservationConcept(concept);
		// Set this in the source column
		if (concept == null || concept.getIdAsLong() == 0L) {
			observation.setSourceValue(valueSourceString);
		}

		if (concept != null)
			observation.setSourceConcept(concept);

		/* Set the value of the observation */
		Type valueType = fhirResource.getValue();
		if (valueType instanceof Quantity) {
			observation.setValueAsNumber(((Quantity) valueType).getValue().doubleValue());

			// For unit, OMOP need unit concept
			String unitCode = ((Quantity) valueType).getCode();
			String unitSystem = ((Quantity) valueType).getSystem();

			String omopVocabulary = null;
			concept = null;
			if (unitCode != null && !unitCode.isEmpty()) {
				if (unitSystem == null || unitSystem.isEmpty()) {
					// If system is empty, then we check UCUM for the unit.
					omopVocabulary = OmopCodeableConceptMapping.UCUM.getOmopVocabulary();
				} else {
					try {
//						omopVocabulary = OmopCodeableConceptMapping.omopVocabularyforFhirUri(unitSystem);
						omopVocabulary = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(unitSystem);
					} catch (FHIRException e) {
						e.printStackTrace();
					}
				}
				concept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, omopVocabulary,
						unitCode);
			}

			// Save the unit in the unit source column to save the source value.
			String unitString = ((Quantity) valueType).getUnit();
			observation.setUnitSourceValue(unitString);

			if (concept != null) {
				// If we found the concept for unit, use it. Otherwise, leave it
				// empty.
				// We still have this in the unit source column.
				observation.setUnitConcept(concept);
			}

		} else if (valueType instanceof CodeableConcept) {
			// We have coeable concept value. Get System and Value.
			// FHIR allows one value[x].
			codings = ((CodeableConcept) valueType).getCoding();
			concept = null;
			for (Coding coding : codings) {
				String fhirSystem = coding.getSystem();
				String fhirCode = coding.getCode();

				if (fhirSystem == null || fhirSystem.isEmpty() || fhirCode == null || fhirCode.isEmpty()) {
					continue;
				}

				try {
//					String omopVocabulary = OmopCodeableConceptMapping.omopVocabularyforFhirUri(fhirSystem);
					String omopVocabulary = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(fhirSystem);
					concept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, omopVocabulary,
							fhirCode);

					if (concept == null) {
						throw new FHIRException(
								"We couldn't map the codeable concept value - " + fhirSystem + ":" + fhirCode);
					}
				} catch (FHIRException e) {
					e.printStackTrace();
				}

				break;
			}
			if (concept == null) {
				try {
					throw new FHIRException("We couldn't find a concept to map the codeable concept value.");
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}

			observation.setValueAsConcept(concept);
		}

		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		if (fhirResource.getEffective() instanceof DateTimeType) {
			Date date = ((DateTimeType) fhirResource.getEffective()).getValue();
			observation.setDate(date);
			observation.setTime(timeFormat.format(date));
		} else if (fhirResource.getEffective() instanceof Period) {
			Date startDate = ((Period) fhirResource.getEffective()).getStart();
			if (startDate != null) {
				observation.setDate(startDate);
				observation.setTime(timeFormat.format(startDate));
			}
		}
		/* Set visit occurrence */
		Reference contextReference = fhirResource.getContext();
		VisitOccurrence visitOccurrence = null;
		if (contextReference != null && !contextReference.isEmpty()) {
			if (contextReference.getReferenceElement().getResourceType().equals(EncounterResourceProvider.getType())) {
				// Encounter context.
				Long fhirEncounterId = contextReference.getReferenceElement().getIdPartAsLong();
				Long omopVisitOccurrenceId = IdMapping.getOMOPfromFHIR(fhirEncounterId,
						EncounterResourceProvider.getType());
				if (omopVisitOccurrenceId != null) {
					visitOccurrence = visitOccurrenceService.findById(omopVisitOccurrenceId);
				}
				if (visitOccurrence == null) {
					try {
						throw new FHIRException(
								"The Encounter (" + contextReference.getReference() + ") context couldn't be found.");
					} catch (FHIRException e) {
						e.printStackTrace();
					}
				} else {
					observation.setVisitOccurrence(visitOccurrence);
				}
			} else {
				// Episode of Care context.
				// TODO: Do we have a mapping for the Episode of Care??
			}
		}

		List<CodeableConcept> categories = fhirResource.getCategory();
		Long typeConceptId = 0L;
		for (CodeableConcept category : categories) {
			codings = category.getCoding();
			for (Coding coding : codings) {
				String fhirSystem = coding.getSystem();
				String fhirCode = coding.getCode();
				if (fhirSystem == null || fhirSystem.isEmpty() || fhirCode == null || fhirCode.isEmpty()) {
					continue;
				}
				try {
					typeConceptId = OmopConceptMapping.omopForObservationCategoryCode(fhirCode);
				} catch (FHIRException e) {
					e.printStackTrace();
				}
				if (typeConceptId > 0L)
					break;
			}
			if (typeConceptId > 0L)
				break;
		}

		concept = new Concept();
		concept.setId(typeConceptId);
		observation.setTypeConcept(concept);

		return observation;
	}

	private boolean is_measurement_by_valuetype(Observation fhirResource) {
		Type value = fhirResource.getValue();
		if (value instanceof Quantity)
			return true;

		return false;
	}

	public Map<String, Object> constructOmopMeasurementObservation(Long omopId, Observation fhirResource) {
		// returns a map that contains either OMOP measurement entity classes or
		// OMOP observation entity. The return map consists as follows,
		// "type": "Observation" or "Measurement"
		// "entity": omopObservation or List<Measurement>
		Map<String, Object> retVal = new HashMap<String, Object>();

		List<Measurement> measurements = null;
		edu.gatech.chai.omopv5.model.entity.Observation observation = null;

		for (Coding coding : fhirResource.getCode().getCoding()) {
			String code = coding.getCode();
			if (code == null) {
				code = "";
			}
			String system = coding.getSystem();
			if (system == null) {
				system = "";
			}

			List<Concept> conceptForCodes = conceptService.searchByColumnString("conceptCode", code);
			if (conceptForCodes.size() <= 0) {
				// we have no matching code. Put no matching code.
				conceptForCodes.add(conceptService.findById(0L));
			}

			for (Concept conceptForCode : conceptForCodes) {
				String domain = conceptForCode.getDomain();
				String systemName = conceptForCode.getVocabulary().getId();
				try {
//					List<Identifier> identifiers = fhirResource.getIdentifier();
//					String identifier_value = null;
//					if ((domain.equalsIgnoreCase("measurement")
//							&& systemName.equalsIgnoreCase(OmopCodeableConceptMapping.omopVocabularyforFhirUri(system)))
//							|| is_measurement_by_valuetype(fhirResource)) {

					if ((domain.equalsIgnoreCase("measurement") && systemName
							.equalsIgnoreCase(fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(system)))
							|| is_measurement_by_valuetype(fhirResource)) {

						// TODO: Omop does not have a place holder to track the source of measurement
						// data.
//						for (Identifier identifier : identifiers) {
//							identifier_value = identifier.getValue();
//							if (identifier_value != null) {
//								List<Measurement> results = measurementService.searchByColumnString("sourceValue",
//										identifier_value);
//								if (results.size() > 0) {
//									// We do not CREATE. Instead, we update
//									// this.
//									// set the measurement.
//									omopId = results.get(0).getId();
//									break;
//								}
//							}
//						}

						measurements = constructOmopMeasurement(omopId, fhirResource, system, code);
						if (measurements != null && measurements.size() > 0) {
							retVal.put("type", "Measurement");
							retVal.put("entity", measurements);
							return retVal;
						}
//					} else if (domain.equalsIgnoreCase("observation") && systemName
//							.equalsIgnoreCase(OmopCodeableConceptMapping.omopVocabularyforFhirUri(system))) {
					} else if (domain.equalsIgnoreCase("observation") && systemName
							.equalsIgnoreCase(fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(system))) {

						// TODO: Omop does not have a place holder to track the source of observation
						// data.
//						for (Identifier identifier : identifiers) {
//							identifier_value = identifier.getValue();
//							if (identifier_value != null) {
//								List<edu.gatech.chai.omopv5.jpa.entity.Observation> results = observationService
//										.searchByColumnString("sourceValue", identifier_value);
//								if (results.size() > 0) {
//									// We do not CREATE. Instead, we update
//									// this.
//									// set the measurement.
//									omopId = results.get(0).getId();
//									break;
//								}
//							}
//						}

						observation = constructOmopObservation(omopId, fhirResource);
						if (observation != null) {
							retVal.put("type", "Observation");
							retVal.put("entity", observation);
							return retVal;
						}
					}
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}
		}

		// Error... we don't know how to handle this coding...
		// TODO: add some exception or notification of the error here.
		logger.error(
				"we don't know how to handle this coding for this observation: " + fhirResource.getCode().toString());
		return null;
	}

	public void validation(Observation fhirResource, IdType fhirId) throws FHIRException {
		Reference subjectReference = fhirResource.getSubject();
		if (subjectReference == null) {
			throw new FHIRException("We requres subject to contain a Patient");
		}
		if (!subjectReference.getReferenceElement().getResourceType()
				.equalsIgnoreCase(PatientResourceProvider.getType())) {
			throw new FHIRException("We only support " + PatientResourceProvider.getType()
					+ " for subject. But provided [" + subjectReference.getReferenceElement().getResourceType() + "]");
		}

		Long fhirSubjectId = subjectReference.getReferenceElement().getIdPartAsLong();
		Long omopPersonId = IdMapping.getOMOPfromFHIR(fhirSubjectId, PatientResourceProvider.getType());
		if (omopPersonId == null) {
			throw new FHIRException("We couldn't find the patient in the Subject");
		}
	}

	@Override
	public Long toDbase(Observation fhirResource, IdType fhirId) throws FHIRException {
		Long fhirIdLong = null;
		Long omopId = null;
		if (fhirId != null) {
			fhirIdLong = fhirId.getIdPartAsLong();
			omopId = IdMapping.getOMOPfromFHIR(fhirIdLong, ObservationResourceProvider.getType());
			if (omopId < 0) {
				// This is observation table data in OMOP.
				omopId = -omopId; // convert to positive number;
			}
		} else {
			// check if we already have this entry by comparing
			// code, date, time and patient
			Long patientFhirId = fhirResource.getSubject().getReferenceElement().getIdPartAsLong();

			// get date and time
			Date date = null;
			if (fhirResource.getEffective() instanceof DateTimeType) {
				date = ((DateTimeType) fhirResource.getEffective()).getValue();
			} else if (fhirResource.getEffective() instanceof Period) {
				date = ((Period) fhirResource.getEffective()).getStart();
			}

			// get code
			Concept concept = null;
			List<Coding> codings = fhirResource.getCode().getCoding();
			String fhirSystem = null;
			String code = null;
			String display = null;
			for (Coding coding : codings) {
				fhirSystem = coding.getSystem();
				code = coding.getCode();
				display = coding.getDisplay();
				String omopSystem = null;
				if (fhirSystem != null) {
//					omopSystem = OmopCodeableConceptMapping.omopVocabularyforFhirUri(fhirSystem);
					omopSystem = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(fhirSystem);
					if (omopSystem != null)
						concept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, omopSystem,
								code);
				}
				if (concept != null)
					break;
			}

			if (patientFhirId != null && date != null) {
				List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper>();
				paramList.addAll(mapParameter("Patient:" + Patient.SP_RES_ID, String.valueOf(patientFhirId), false));

				DateParam dateParam = new DateParam();
				dateParam.setPrefix(ParamPrefixEnum.EQUAL);
				dateParam.setValue(date);
				paramList.addAll(mapParameter(Observation.SP_DATE, dateParam, false));

				if (concept == null) {
					ParameterWrapper pw = new ParameterWrapper();
					String sourceValueString = fhirSystem + " " + code + " " + display;
					pw.setParameterType("String");
					pw.setParameters(Arrays.asList("sourceValue"));
					pw.setOperators(Arrays.asList("="));
					pw.setValues(Arrays.asList(sourceValueString));
					pw.setRelationship("and");
					paramList.add(pw);
				} else {
					TokenParam tokenParam = new TokenParam();
					tokenParam.setSystem(fhirSystem);
					tokenParam.setValue(code);
					paramList.addAll(mapParameter(Observation.SP_CODE, tokenParam, false));
				}

				List<IBaseResource> resources = new ArrayList<IBaseResource>();
				List<String> includes = new ArrayList<String>();

				searchWithParams(0, 0, paramList, resources, includes, null);
				if (resources.size() > 0) {
					IBaseResource res = resources.get(0);
					fhirIdLong = res.getIdElement().getIdPartAsLong();
					omopId = IdMapping.getOMOPfromFHIR(fhirIdLong, ObservationResourceProvider.getType());
					if (omopId < 0) {
						// This is observation table data in OMOP.
						omopId = -omopId; // convert to positive number;
					}
				}
			}

		}

		validation(fhirResource, fhirId);

		List<Measurement> measurements = null;
		edu.gatech.chai.omopv5.model.entity.Observation observation = null;

		Map<String, Object> entityMap = constructOmopMeasurementObservation(omopId, fhirResource);
		Long retId = null;

		Date date = null;
		FPerson fPerson = null;
		Long domainConceptId = null;
		if (entityMap != null && ((String) entityMap.get("type")).equalsIgnoreCase("measurement")) {
			measurements = (List<Measurement>) entityMap.get("entity");

			Long retvalSystolic = null;
			Long retvalDiastolic = null;
			for (Measurement m : measurements) {
				if (m != null) {
					if (m.getId() != null) {
						retId = measurementService.update(m).getId();
					} else {
						retId = measurementService.create(m).getId();
					}
					if (m.getMeasurementConcept().getId() == OmopObservation.SYSTOLIC_CONCEPT_ID) {
						retvalSystolic = retId;
					} else if (m.getMeasurementConcept().getId() == OmopObservation.DIASTOLIC_CONCEPT_ID) {
						retvalDiastolic = retId;
					}

					date = m.getDate();
					fPerson = m.getFPerson();
				}
			}

			// Ok, done. now we return.
			if (retvalSystolic != null)
				retId = retvalSystolic;
			else if (retvalDiastolic != null)
				retId = retvalDiastolic;

			domainConceptId = 21L;
		} else {
			observation = (edu.gatech.chai.omopv5.model.entity.Observation) entityMap.get("entity");
			if (observation.getId() != null) {
				retId = observationService.update(observation).getId();
			} else {
				retId = observationService.create(observation).getId();
			}

			date = observation.getDate();
			fPerson = observation.getFPerson();

			domainConceptId = 27L;
		}

		if (retId == null)
			return null;

		// Check method in FHIR. If we hacve method, check the concept ID if it's
		// codeable concept and put
		// entry in the relationship table. If test, use Note table and put the
		// relationship in relationship table.
		CodeableConcept methodCodeable = fhirResource.getMethod();
		List<Coding> methodCodings = methodCodeable.getCoding();
		String methodString = methodCodeable.getText();
		if (methodCodings != null && !methodCodings.isEmpty()) {
			for (Coding methodCoding : methodCodings) {
				Concept methodConcept = CodeableConceptUtil.getOmopConceptWithFhirConcept(conceptService, methodCoding);
				if (methodConcept == null) {
					String methodCodingDisplay = methodCoding.getDisplay();
					if (methodCodingDisplay != null && !methodCodingDisplay.isEmpty()) {
						createFactRelationship(date, fPerson, methodCodingDisplay, domainConceptId, 26L, 44818800L,
								retId, null);
//						Note methodNote = new Note();
//						methodNote.setDate(date);
//						methodNote.setFPerson(fPerson);
//						methodNote.setNoteText(methodCodingDisplay);
//						methodNote.setType(new Concept(44814645L));
//						Note note = noteService.create(methodNote);
//
//						// Create relationship.
//						FactRelationship factRelationship = new FactRelationship();
//						factRelationship.setDomainConcept1(domainConceptId);
//						factRelationship.setFactId1(retId);
//						factRelationship.setDomainConcept2(26L);
//						factRelationship.setFactId2(note.getId());
//						factRelationship.setRelationshipConcept(new Concept(44818800L));
//						factRelationshipService.create(factRelationship);
					}
				} else {
					// Create relationship.
					createFactRelationship(null, null, null, domainConceptId, 58L, 44818800L, retId,
							methodConcept.getId());
//					FactRelationship factRelationship = new FactRelationship();
//					factRelationship.setDomainConcept1(domainConceptId);
//					factRelationship.setFactId1(retId);
//					factRelationship.setDomainConcept2(58L);
//					factRelationship.setFactId2(methodConcept.getId());
//					factRelationship.setRelationshipConcept(new Concept(44818800L));
//					factRelationshipService.create(factRelationship);
				}
			}
		} else {
			if (methodString != null && !methodString.isEmpty()) {
				createFactRelationship(date, fPerson, methodString, domainConceptId, 26L, 44818800L, retId, null);

//				Note methodNote = new Note();
//				methodNote.setDate(date);
//				methodNote.setFPerson(fPerson);
//				methodNote.setNoteText(methodString);
//				methodNote.setType(new Concept(44814645L));
//				Note note = noteService.create(methodNote);
//
//				// Create relationship.
//				FactRelationship factRelationship = new FactRelationship();
//				factRelationship.setDomainConcept1(domainConceptId);
//				factRelationship.setFactId1(retId);
//				factRelationship.setDomainConcept2(26L);
//				factRelationship.setFactId2(note.getId());
//				factRelationship.setRelationshipConcept(new Concept(44818800L));
//				factRelationshipService.create(factRelationship);
			}
		}

		// Check comments. If exists, put them in note table. And create relationship
		// entry.
		String comment = fhirResource.getComment();
		if (comment != null && !comment.isEmpty()) {
			createFactRelationship(date, fPerson, comment, domainConceptId, 26L, 44818721L, retId, null);
//			Note methodNote = new Note();
//			methodNote.setDate(date);
//			methodNote.setFPerson(fPerson);
//			methodNote.setNoteText(comment);
//			methodNote.setType(new Concept(44814645L));
//			Note note = noteService.create(methodNote);
//
//			// Create relationship.
//			FactRelationship factRelationship = new FactRelationship();
//			factRelationship.setDomainConcept1(domainConceptId);
//			factRelationship.setFactId1(retId);
//			factRelationship.setDomainConcept2(26L);
//			factRelationship.setFactId2(note.getId());
//			factRelationship.setRelationshipConcept(new Concept(44818721L));
//			factRelationshipService.create(factRelationship);
		}

		Long retFhirId = IdMapping.getFHIRfromOMOP(retId, ObservationResourceProvider.getType());
		return retFhirId;
	}

	private void createFactRelationship(Date noteDate, FPerson noteFPerson, String noteText, Long domainConceptId1,
			Long domainConceptId2, Long relationshipId, Long factId1, Long factId2) {
		// Create relationship.
		FactRelationship factRelationship = new FactRelationship();

		if (noteDate != null && noteFPerson != null && noteText != null) {
			// Check if this note exists.
			List<Note> existingNotes = noteService.searchByColumnString("noteText", noteText);
			Note note;
			boolean found = false;
			if (existingNotes != null && existingNotes.size() > 0) {
				// check other fields for all notes.
				for (Note existingNote : existingNotes) {
					Calendar cal = Calendar.getInstance();
					cal.setTime(noteDate);
					int noteYear = cal.get(Calendar.YEAR);
					int noteMonth = cal.get(Calendar.MONTH);
					int noteDay = cal.get(Calendar.DAY_OF_MONTH);

					cal.setTime(existingNote.getDate());
					int exNoteYear = cal.get(Calendar.YEAR);
					int exNoteMonth = cal.get(Calendar.MONTH);
					int exNoteDay = cal.get(Calendar.DAY_OF_MONTH);

					if (noteYear == exNoteYear && noteMonth == exNoteMonth && noteDay == exNoteDay
							&& noteFPerson.getId() == existingNote.getFPerson().getId()
							&& 44814645L == existingNote.getType().getId()) {

						// check if we have this in the fact relationship table. If so,
						// there is no further action required.
						List<FactRelationship> factRelationships = factRelationshipService.searchFactRelationship(
								domainConceptId1, factId1, domainConceptId2, factId2, relationshipId);
						if (factRelationships.size() > 0) {
							found = true;
						}

						note = existingNote;
						break;
					}
				}
			}
			if (found == false) {
				Note methodNote = new Note();
				methodNote.setDate(noteDate);
				methodNote.setFPerson(noteFPerson);
				methodNote.setNoteText(noteText);
				methodNote.setType(new Concept(44814645L));

				note = noteService.create(methodNote);
			} else {
				return;
			}
			factRelationship.setFactId2(note.getId());
		} else {
			// This is relationship to concept. Thus, we don't need to create entry for
			// concept.
			// But, check fact relationship table for its existence.
			List<FactRelationship> factRelationships = factRelationshipService.searchFactRelationship(domainConceptId1,
					factId1, domainConceptId2, factId2, relationshipId);

			if (factRelationships.size() > 0) {
				return;
			}

			factRelationship.setFactId2(factId2);
		}

		factRelationship.setDomainConcept1(domainConceptId1);
		factRelationship.setFactId1(factId1);
		factRelationship.setDomainConcept2(domainConceptId2);
		factRelationship.setRelationshipConcept(new Concept(relationshipId));
		factRelationshipService.create(factRelationship);
	}

	// Blood Pressure is stored in the component. So, we store two values in
	// the component section. We do this by selecting diastolic when systolic
	// is selected. Since we are selecting this already, we need to skip
	// diastolic.
	final ParameterWrapper exceptionParam = new ParameterWrapper("Long", Arrays.asList("measurementConcept.id"),
			Arrays.asList("!="), Arrays.asList(String.valueOf(OmopObservation.DIASTOLIC_CONCEPT_ID)), "or");

	final ParameterWrapper exceptionParam4Search = new ParameterWrapper("Long", Arrays.asList("observationConcept.id"),
			Arrays.asList("!="), Arrays.asList(String.valueOf(OmopObservation.DIASTOLIC_CONCEPT_ID)), "or");

	@Override
	public Long getSize() {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		return getSize(mapList);
		// mapList.add(exceptionParam);
		//
		// return measurementService.getSize() -
		// measurementService.getSize(mapList) + observationService.getSize();
	}

	@Override
	public Long getSize(List<ParameterWrapper> mapList) {
		// List<ParameterWrapper> exceptions = new
		// ArrayList<ParameterWrapper>();
		// exceptions.add(exceptionParam);
		// map.put(MAP_EXCEPTION_EXCLUDE, exceptions);
		// Map<String, List<ParameterWrapper>> exceptionMap = new
		// HashMap<String, List<ParameterWrapper>>(map);

		mapList.add(exceptionParam4Search);

		// return
		// getMyOmopService().getSize(map)-measurementService.getSize(exceptionMap);
		return getMyOmopService().getSize(mapList);
	}

	@Override
	public void searchWithoutParams(int fromIndex, int toIndex, List<IBaseResource> listResources,
			List<String> includes, String sort) {

		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper>();
		searchWithParams(fromIndex, toIndex, paramList, listResources, includes, sort);

		// List<ParameterWrapper> exceptions = new
		// ArrayList<ParameterWrapper>();
		// exceptions.add(exceptionParam);
		// map.put(MAP_EXCEPTION_EXCLUDE, exceptions);
		//
		// List<FObservationView> fObservationViews =
		// getMyOmopService().searchWithParams(fromIndex, toIndex, map);
		//
		// // We got the results back from OMOP database. Now, we need to
		// construct
		// // the list of
		// // FHIR Patient resources to be included in the bundle.
		// for (FObservationView fObservationView : fObservationViews) {
		// Long omopId = fObservationView.getId();
		// Long fhirId = IdMapping.getFHIRfromOMOP(omopId,
		// ObservationResourceProvider.getType());
		// Observation fhirResource = constructResource(fhirId,
		// fObservationView, includes);
		// if (fhirResource != null) {
		// listResources.add(fhirResource);
		// // Do the rev_include and add the resource to the list.
		// addRevIncludes(omopId, includes, listResources);
		// }
		// }
		//
	}

	@Override
	public void searchWithParams(int fromIndex, int toIndex, List<ParameterWrapper> paramList,
			List<IBaseResource> listResources, List<String> includes, String sort) {
		paramList.add(exceptionParam4Search);

		List<FObservationView> fObservationViews = getMyOmopService().searchWithParams(fromIndex, toIndex, paramList,
				sort);

		for (FObservationView fObservationView : fObservationViews) {
			Long omopId = fObservationView.getId();
			Long fhirId = IdMapping.getFHIRfromOMOP(omopId, ObservationResourceProvider.getType());
			Observation fhirResource = constructResource(fhirId, fObservationView, includes);
			if (fhirResource != null) {
				listResources.add(fhirResource);
				// Do the rev_include and add the resource to the list.
				addRevIncludes(omopId, includes, listResources);
			}
		}
	}

	private static Date createDateTime(FObservationView fObservationView) {
		Date myDate = null;
		if (fObservationView.getDate() != null) {
			SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
			String dateString = fmt.format(fObservationView.getDate());
			fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try {
				if (fObservationView.getTime() != null && fObservationView.getTime().isEmpty() == false) {
					myDate = fmt.parse(dateString + " " + fObservationView.getTime());
				} else {
					myDate = fObservationView.getDate();
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		return myDate;
	}

	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
		if (or)
			paramWrapper.setUpperRelationship("or");
		else
			paramWrapper.setUpperRelationship("and");

		switch (parameter) {
		case Observation.SP_RES_ID:
			String organizationId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(organizationId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Observation.SP_DATE:
			Date date = ((DateParam) value).getValue();
			ParamPrefixEnum prefix = ((DateParam) value).getPrefix();
			String inequality = "=";
			if (prefix.equals(ParamPrefixEnum.EQUAL))
				inequality = "=";
			else if (prefix.equals(ParamPrefixEnum.LESSTHAN))
				inequality = "<";
			else if (prefix.equals(ParamPrefixEnum.LESSTHAN_OR_EQUALS))
				inequality = "<=";
			else if (prefix.equals(ParamPrefixEnum.GREATERTHAN))
				inequality = ">";
			else if (prefix.equals(ParamPrefixEnum.GREATERTHAN_OR_EQUALS))
				inequality = ">=";
			else if (prefix.equals(ParamPrefixEnum.NOT_EQUAL))
				inequality = "!=";

			// get Date.
			SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
			String time = timeFormat.format(date);

			// get only date part.
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date dateWithoutTime = null;
			try {
				dateWithoutTime = sdf.parse(sdf.format(date));
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				break;
			}

			System.out.println("TIME VALUE:" + String.valueOf(dateWithoutTime.getTime()));
			paramWrapper.setParameterType("Date");
			paramWrapper.setParameters(Arrays.asList("date"));
			paramWrapper.setOperators(Arrays.asList(inequality));
			paramWrapper.setValues(Arrays.asList(String.valueOf(dateWithoutTime.getTime())));
			paramWrapper.setRelationship("and");
			mapList.add(paramWrapper);

			// Time
			ParameterWrapper paramWrapper_time = new ParameterWrapper();
			paramWrapper_time.setParameterType("String");
			paramWrapper_time.setParameters(Arrays.asList("time"));
			paramWrapper_time.setOperators(Arrays.asList(inequality));
			paramWrapper_time.setValues(Arrays.asList(time));
			paramWrapper_time.setRelationship("and");
			mapList.add(paramWrapper_time);

			break;
		case Observation.SP_CODE:
			String system = ((TokenParam) value).getSystem();
			String code = ((TokenParam) value).getValue();
			String omopVocabulary = null;
			if (system != null && !system.isEmpty()) {
				try {
//					omopVocabulary = OmopCodeableConceptMapping.omopVocabularyforFhirUri(system);
					omopVocabulary = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(system);
				} catch (FHIRException e) {
					e.printStackTrace();
					break;
				}
			} else {
				omopVocabulary = "None";
			}

			if (omopVocabulary.equals(OmopCodeableConceptMapping.LOINC.getOmopVocabulary())) {
				// This is LOINC code.
				// Check if this is for BP.
				if (code != null && !code.isEmpty()) {
					if (BP_SYSTOLIC_DIASTOLIC_CODE.equals(code)) {
						// In OMOP, we have systolic and diastolic as separate
						// entries.
						// We search for systolic. When constructing FHIR<,
						// constructFHIR
						// will search matching diastolic value.
						paramWrapper.setParameterType("String");
						paramWrapper.setParameters(
								Arrays.asList("observationConcept.vocabulary.id", "observationConcept.conceptCode"));
						paramWrapper.setOperators(Arrays.asList("like", "like"));
						paramWrapper.setValues(Arrays.asList(omopVocabulary, SYSTOLIC_LOINC_CODE));
						paramWrapper.setRelationship("and");
						mapList.add(paramWrapper);
					} else {
						paramWrapper.setParameterType("String");
						paramWrapper.setParameters(
								Arrays.asList("observationConcept.vocabulary.id", "observationConcept.conceptCode"));
						paramWrapper.setOperators(Arrays.asList("like", "like"));
						paramWrapper.setValues(Arrays.asList(omopVocabulary, code));
						paramWrapper.setRelationship("and");
						mapList.add(paramWrapper);
					}
				} else {
					// We have no code specified. Search by system.
					paramWrapper.setParameterType("String");
					paramWrapper.setParameters(Arrays.asList("observationConcept.vocabulary.id"));
					paramWrapper.setOperators(Arrays.asList("like"));
					paramWrapper.setValues(Arrays.asList(omopVocabulary));
					paramWrapper.setRelationship("or");
					mapList.add(paramWrapper);
				}
			} else {
				if (system == null || system.isEmpty()) {
					if (code == null || code.isEmpty()) {
						// nothing to do
						break;
					} else {
						// no system but code.
						paramWrapper.setParameterType("String");
						paramWrapper.setParameters(Arrays.asList("observationConcept.conceptCode"));
						paramWrapper.setOperators(Arrays.asList("like"));
						if (BP_SYSTOLIC_DIASTOLIC_CODE.equals(code))
							paramWrapper.setValues(Arrays.asList(SYSTOLIC_LOINC_CODE));
						else
							paramWrapper.setValues(Arrays.asList(code));
						paramWrapper.setRelationship("or");
						mapList.add(paramWrapper);
					}
				} else {
					if (code == null || code.isEmpty()) {
						// yes system but no code.
						paramWrapper.setParameterType("String");
						paramWrapper.setParameters(Arrays.asList("observationConcept.vocabulary.id"));
						paramWrapper.setOperators(Arrays.asList("like"));
						paramWrapper.setValues(Arrays.asList(omopVocabulary));
						paramWrapper.setRelationship("or");
						mapList.add(paramWrapper);
					} else {
						// We have both system and code.
						paramWrapper.setParameterType("String");
						paramWrapper.setParameters(
								Arrays.asList("observationConcept.vocabulary.id", "observationConcept.conceptCode"));
						paramWrapper.setOperators(Arrays.asList("like", "like"));
						paramWrapper.setValues(Arrays.asList(omopVocabulary, code));
						paramWrapper.setRelationship("and");
						mapList.add(paramWrapper);
					}
				}
			}
			break;
		case "Patient:" + Patient.SP_RES_ID:
			addParamlistForPatientIDName(parameter, (String) value, paramWrapper, mapList);
//			String pId = (String) value;
//			paramWrapper.setParameterType("Long");
//			paramWrapper.setParameters(Arrays.asList("fPerson.id"));
//			paramWrapper.setOperators(Arrays.asList("="));
//			paramWrapper.setValues(Arrays.asList(pId));
//			paramWrapper.setRelationship("or");
//			mapList.add(paramWrapper);
			break;
		case "Patient:" + Patient.SP_NAME:
			addParamlistForPatientIDName(parameter, (String) value, paramWrapper, mapList);
//			String patientName = ((String) value).replace("\"", "");
//			paramWrapper.setParameterType("String");
//			paramWrapper.setParameters(Arrays.asList("fPerson.familyName", "fPerson.givenName1", "fPerson.givenName2",
//					"fPerson.prefixName", "fPerson.suffixName"));
//			paramWrapper.setOperators(Arrays.asList("like", "like", "like", "like", "like"));
//			paramWrapper.setValues(Arrays.asList("%" + patientName + "%"));
//			paramWrapper.setRelationship("or");
//			mapList.add(paramWrapper);
			break;
		case "Patient:" + Patient.SP_IDENTIFIER:
			addParamlistForPatientIDName(parameter, (String) value, paramWrapper, mapList);
			break;
		default:
			mapList = null;
		}

		return mapList;
	}

	@Override
	public FObservationView constructOmop(Long omopId, Observation fhirResource) {
		// This is view. So, we can't update or create.
		// See the contructOmop for the actual tables such as
		// constructOmopMeasurement
		// or consturctOmopObservation.
		return null;
	}

}
