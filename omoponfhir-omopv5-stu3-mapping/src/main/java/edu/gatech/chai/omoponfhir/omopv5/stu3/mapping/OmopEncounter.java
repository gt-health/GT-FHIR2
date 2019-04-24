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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Encounter.EncounterParticipantComponent;
import org.hl7.fhir.dstu3.model.Encounter.EncounterStatus;
import org.hl7.fhir.dstu3.model.codesystems.V3ActCode;
import org.hl7.fhir.exceptions.FHIRException;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.EncounterResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.OrganizationResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.PatientResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.PractitionerResourceProvider;
import edu.gatech.chai.omopv5.dba.service.CareSiteService;
import edu.gatech.chai.omopv5.dba.service.FPersonService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.ProviderService;
import edu.gatech.chai.omopv5.dba.service.VisitOccurrenceService;
import edu.gatech.chai.omopv5.model.entity.CareSite;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.FPerson;
import edu.gatech.chai.omopv5.model.entity.Provider;
import edu.gatech.chai.omopv5.model.entity.VisitOccurrence;

public class OmopEncounter extends BaseOmopResource<Encounter, VisitOccurrence, VisitOccurrenceService>
		implements IResourceMapping<Encounter, VisitOccurrence> {

	private static OmopEncounter omopEncounter = new OmopEncounter();
	private FPersonService fPersonService;
	private CareSiteService careSiteService;
	private ProviderService providerService;

	public OmopEncounter() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), VisitOccurrence.class,
				VisitOccurrenceService.class, EncounterResourceProvider.getType());
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}

	public OmopEncounter(WebApplicationContext context) {
		super(context, VisitOccurrence.class, VisitOccurrenceService.class, EncounterResourceProvider.getType());
		initialize(context);
	}

	private void initialize(WebApplicationContext context) {
		// Get bean for other services that we need for mapping.
		fPersonService = context.getBean(FPersonService.class);
		careSiteService = context.getBean(CareSiteService.class);
		providerService = context.getBean(ProviderService.class);
	}

	public static OmopEncounter getInstance() {
		return OmopEncounter.omopEncounter;
	}

	@Override
	public Encounter constructFHIR(Long fhirId, VisitOccurrence visitOccurrence) {
		Encounter encounter = new Encounter();
		encounter.setId(new IdType(fhirId));

		if (visitOccurrence.getVisitConcept() != null) {
			String visitString = visitOccurrence.getVisitConcept().getName().toLowerCase();
			Coding coding = new Coding();
			if (visitString.contains("inpatient")) {
				coding.setSystem(V3ActCode.IMP.getSystem());
				coding.setCode(V3ActCode.IMP.toCode());
				coding.setDisplay(V3ActCode.IMP.getDisplay());
			} else if (visitString.toLowerCase().contains("outpatient")) {
				coding.setSystem(V3ActCode.AMB.getSystem());
				coding.setCode(V3ActCode.AMB.toCode());
				coding.setDisplay(V3ActCode.AMB.getDisplay());
			} else if (visitString.toLowerCase().contains("ambulatory")
					|| visitString.toLowerCase().contains("office")) {
				coding.setSystem(V3ActCode.AMB.getSystem());
				coding.setCode(V3ActCode.AMB.toCode());
				coding.setDisplay(V3ActCode.AMB.getDisplay());
			} else if (visitString.toLowerCase().contains("home")) {
				coding.setSystem(V3ActCode.HH.getSystem());
				coding.setCode(V3ActCode.HH.toCode());
				coding.setDisplay(V3ActCode.HH.getDisplay());
			} else if (visitString.toLowerCase().contains("emergency")) {
				coding.setSystem(V3ActCode.EMER.getSystem());
				coding.setCode(V3ActCode.EMER.toCode());
				coding.setDisplay(V3ActCode.EMER.getDisplay());
			} else if (visitString.toLowerCase().contains("field")) {
				coding.setSystem(V3ActCode.FLD.getSystem());
				coding.setCode(V3ActCode.FLD.toCode());
				coding.setDisplay(V3ActCode.FLD.getDisplay());
			} else if (visitString.toLowerCase().contains("daytime")) {
				coding.setSystem(V3ActCode.SS.getSystem());
				coding.setCode(V3ActCode.SS.toCode());
				coding.setDisplay(V3ActCode.SS.getDisplay());
			} else if (visitString.toLowerCase().contains("virtual")) {
				coding.setSystem(V3ActCode.VR.getSystem());
				coding.setCode(V3ActCode.VR.toCode());
				coding.setDisplay(V3ActCode.VR.getDisplay());
			} else {
				coding = null;
			}

			if (coding != null)
				encounter.setClass_(coding);

		}

		encounter.setStatus(EncounterStatus.FINISHED);

		// set Patient Reference
		Reference patientReference = new Reference(
				new IdType(PatientResourceProvider.getType(), visitOccurrence.getFPerson().getId()));
		patientReference.setDisplay(visitOccurrence.getFPerson().getNameAsSingleString());
		encounter.setSubject(patientReference);

		// set Period
		Period visitPeriod = new Period();
		DateFormat dateOnlyFormat = new SimpleDateFormat("yyyy/MM/dd");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
		try {
			// For start Date
			String timeString = "00:00:00";
			if (visitOccurrence.getStartTime() != null && !visitOccurrence.getStartTime().isEmpty()) {
				timeString = visitOccurrence.getStartTime();
			}
			String dateTimeString = dateOnlyFormat.format(visitOccurrence.getStartDate()) + " " + timeString;
			Date DateTime = dateFormat.parse(dateTimeString);
			visitPeriod.setStart(DateTime);

			// For end Date
			timeString = "00:00:00";
			if (visitOccurrence.getEndTime() != null && !visitOccurrence.getEndTime().isEmpty()) {
				timeString = visitOccurrence.getEndTime();
			}
			dateTimeString = dateOnlyFormat.format(visitOccurrence.getEndDate()) + " " + timeString;
			DateTime = dateFormat.parse(dateTimeString);
			visitPeriod.setEnd(DateTime);

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		encounter.setPeriod(visitPeriod);

		if (visitOccurrence.getCareSite() != null) {
			Reference serviceProviderReference = new Reference(
				new IdType(OrganizationResourceProvider.getType(), 
					IdMapping.getFHIRfromOMOP(visitOccurrence.getCareSite().getId(), OrganizationResourceProvider.getType())));
			serviceProviderReference.setDisplay(visitOccurrence.getCareSite().getCareSiteName());
			encounter.setServiceProvider(serviceProviderReference);
		}

		if (visitOccurrence.getProvider() != null) {
			Reference individualReference = new Reference(
				new IdType(PractitionerResourceProvider.getType(), 
					IdMapping.getFHIRfromOMOP(visitOccurrence.getProvider().getId(), PractitionerResourceProvider.getType())));
			individualReference.setDisplay(visitOccurrence.getProvider().getProviderName());
			EncounterParticipantComponent participate = new EncounterParticipantComponent();
			participate.setIndividual(individualReference);
			encounter.addParticipant(participate);
		}

		return encounter;
	}

	@Override
	public Long toDbase(Encounter fhirResource, IdType fhirId) throws FHIRException {
		Long retval;
		Long omopId = null;
		if (fhirId != null) {
			omopId = IdMapping.getOMOPfromFHIR(fhirId.getIdPartAsLong(), getMyFhirResourceType());
		}

		VisitOccurrence visitOccurrence = constructOmop(omopId, fhirResource);

		if (visitOccurrence.getId() != null) {
			retval = getMyOmopService().update(visitOccurrence).getId();
		} else {
			retval = getMyOmopService().create(visitOccurrence).getId();
		}

		return IdMapping.getFHIRfromOMOP(retval, getMyFhirResourceType());
	}

	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
        if (or) paramWrapper.setUpperRelationship("or");
        else paramWrapper.setUpperRelationship("and");

		switch (parameter) {
		case Encounter.SP_RES_ID:
			String encounterId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(encounterId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case "Patient:" + Patient.SP_RES_ID:
			addParamlistForPatientIDName(parameter, (String)value, paramWrapper, mapList);
			break;
		case "Patient:" + Patient.SP_NAME:
			addParamlistForPatientIDName(parameter, (String)value, paramWrapper, mapList);
			break;
		default:
			mapList = null;
		}

		return mapList;

	}

	@Override
	public VisitOccurrence constructOmop(Long omopId, Encounter encounter) {
		FPerson fPerson;
		VisitOccurrence visitOccurrence = null;
		
		if (omopId != null) {
			visitOccurrence = getMyOmopService().findById(omopId);
		}
		if (visitOccurrence == null) {
			visitOccurrence = new VisitOccurrence();				
		}

		Reference patientReference = encounter.getSubject();
		if (patientReference == null || patientReference.isEmpty())
			return null; // We have to have a patient

		// get the Subject
		if (encounter.getSubject() != null) {
			Long subjectId = encounter.getSubject().getReferenceElement().getIdPartAsLong();
			Long subjectFhirId = IdMapping.getOMOPfromFHIR(subjectId, PatientResourceProvider.getType());
			fPerson = fPersonService.findById(subjectFhirId);
			visitOccurrence.setFPerson(fPerson);
		} else {
			// throw an error
			try {
				throw new FHIRException("FHIR Resource does not contain a Subject.");
			} catch (FHIRException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// We are writing to the database. Keep the source so we know where it
		// is coming from
		if (encounter.getId() != null) {
			// See if we already have this in the source field. If so,
			// then we want update not create
			VisitOccurrence origVisit = getMyOmopService().findById(omopId);
			if (origVisit == null)
				visitOccurrence.setVisitSourceValue(encounter.getId());
			else
				visitOccurrence.setId(origVisit.getId());
		}

		/* Set Period */
		SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
		Period tempPeriod = encounter.getPeriod();
		if (tempPeriod != null) {
			Date tempDate = tempPeriod.getStart();
			if (tempDate != null) {
				visitOccurrence.setStartDate(tempDate);
				visitOccurrence.setStartTime(fmt.format(tempDate));
			} else {
				visitOccurrence.setStartDate(new Date(0));
			}

			tempDate = tempPeriod.getEnd();
			if (tempDate != null) {
				visitOccurrence.setEndDate(tempDate);
				visitOccurrence.setEndTime(fmt.format(tempDate));
			} else {
				visitOccurrence.setEndDate(new Date(0));
			}
		}

		/*
		 * Set Class - IP: Inpatient Visit - OP: Outpient Visit - ER: Emergency
		 * Room Visit - LTCP: Long Term Care Visit -
		 */
		Coding classCoding = encounter.getClass_();
		String code = classCoding.getCode();

		Long omopConceptCode;
		try {
			omopConceptCode = (OmopConceptMapping.omopForEncounterClassCode(code));
			Concept visitConcept = new Concept();
			visitConcept.setId(omopConceptCode);
			visitOccurrence.setVisitConcept(visitConcept);
		} catch (FHIRException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/* Set Visit Type - we hardcode this */
		Concept visitTypeConcept = new Concept();
		visitTypeConcept.setId(44818518L); // This is Visit derived from EHR
		visitOccurrence.setVisitTypeConcept(visitTypeConcept);

		/* Set provider, which is practitioner in FHIR */
		EncounterParticipantComponent participant = encounter.getParticipantFirstRep();
		if (participant != null && !participant.isEmpty()) {
			 Reference individualRef = participant.getIndividual();
			if (individualRef != null) {
				if (individualRef.getReferenceElement().getResourceType().equals(PractitionerResourceProvider.getType())) {
					Long providerId = IdMapping.getOMOPfromFHIR(individualRef.getReferenceElement().getIdPartAsLong(), PractitionerResourceProvider.getType());
					Provider provider = providerService.findById(providerId);
					if (provider != null) {
						visitOccurrence.setProvider(provider);
					}
				}
			}
		}
		
		// Set care site, which is organization in FHIR
		Reference serviceProvider = encounter.getServiceProvider();
		if (serviceProvider != null && !serviceProvider.isEmpty()) {
			// service provider is Organization in Omop on FHIR.
			if (serviceProvider.getReferenceElement().getResourceType().equals(OrganizationResourceProvider.getType())) {
				Long careSiteId = IdMapping.getOMOPfromFHIR(serviceProvider.getReferenceElement().getIdPartAsLong(), OrganizationResourceProvider.getType());
				CareSite careSite = careSiteService.findById(careSiteId);
				if (careSite != null) {
					visitOccurrence.setCareSite(careSite);
				}
			}
		}

		// TODO: How do we handle Location Resource. This is different from
		// Location table in OMOP v5.
		// List<ca.uhn.fhir.model.dstu2.resource.Encounter.Location> locations =
		// encounter.getLocation();
		// if (locations.size() > 0) {
		// ca.uhn.fhir.model.dstu2.resource.Encounter.Location location =
		// locations.get(0);
		// ResourceReferenceDt locationResourceRef = location.getLocation();
		// if (locationResourceRef != null) {
		// Location locationResource = (Location)
		// locationResourceRef.getResource();
		// AddressDt address = locationResource.getAddress();
		// if (address != null) {
		// edu.gatech.i3l.fhir.dstu2.entities.Location myLocation =
		// edu.gatech.i3l.fhir.dstu2.entities.Location.searchAndUpdate(address,
		// null);
		// this.set
		// }
		// }
		// }

		return visitOccurrence;
	}
}
