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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import edu.gatech.chai.omoponfhir.omopv5.stu3.model.USCorePatient;
import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.ExtensionUtil;
import edu.gatech.chai.omopv5.dba.service.FPersonService;
import edu.gatech.chai.omopv5.dba.service.MeasurementService;
import edu.gatech.chai.omopv5.dba.service.ObservationService;
import edu.gatech.chai.omopv5.model.entity.FPerson;

public class OmopServerOperations {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OmopServerOperations.class);

	private static OmopTransaction omopTransaction = new OmopTransaction();
	private FPersonService fPersonService;
	private ObservationService observationService;
	private MeasurementService measurementService;

	public OmopServerOperations(WebApplicationContext context) {
		initialize(context);
	}

	public OmopServerOperations() {
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}

	private void initialize(WebApplicationContext context) {
		fPersonService = context.getBean(FPersonService.class);
		observationService = context.getBean(ObservationService.class);
		measurementService = context.getBean(MeasurementService.class);
	}

	public static OmopTransaction getInstance() {
		return omopTransaction;
	}

	private IdType linkToPatient(Reference subject, Map<String, Long> patientMap) {
		if (subject == null || subject.isEmpty()) {
			// We must have subject information to link this to patient.
			// This is OMOP requirement. We skip this for Transaction Messages.
			return null;
		}

		Long fhirId = patientMap.get(subject.getReference());
		if (fhirId == null || fhirId == 0L) {
			// See if we have this patient in OMOP DB.
			IIdType referenceIdType = subject.getReferenceElement();
			if (referenceIdType == null || referenceIdType.isEmpty()) {
				// Giving up...
				return null;
			}
			try {
				fhirId = referenceIdType.getIdPartAsLong();
			} catch (Exception e) {
				// Giving up...
				return null;
			}
			if (fhirId == null || fhirId == 0L) {
				// giving up again...
				return null;
			}
			Long omopId = IdMapping.getOMOPfromFHIR(fhirId, referenceIdType.getResourceType());
			if (omopId == null || omopId == 0L) {
				// giving up... :(
				return null;
			}
			FPerson refFPerson = fPersonService.findById(omopId);
			if (refFPerson == null) {
				// giving up...
				return null;
			}
			return new IdType("Patient", refFPerson.getId());
		} else {
			return new IdType("Patient", fhirId);
		}
	}

	public BundleEntryComponent addResponseEntry(String status, String location) {
		BundleEntryComponent entryBundle = new BundleEntryComponent();
		UUID uuid = UUID.randomUUID();
		entryBundle.setFullUrl("urn:uuid:" + uuid.toString());
		BundleEntryResponseComponent responseBundle = new BundleEntryResponseComponent();
		responseBundle.setStatus(status);
		if (location != null)
			responseBundle.setLocation(location);
		entryBundle.setResponse(responseBundle);

		return entryBundle;
	}

	public List<BundleEntryComponent> createEntries(List<Resource> resources) throws FHIRException {
		List<BundleEntryComponent> responseEntries = new ArrayList<BundleEntryComponent>();
		Map<String, Long> patientMap = new HashMap<String, Long>();

		// do patient first.
		for (Resource resource : resources) {
			if (resource.getResourceType() == ResourceType.Patient) {
				String originalId = resource.getId();
				Long fhirId = OmopPatient.getInstance().toDbase(ExtensionUtil.usCorePatientFromResource(resource),
						null);
				patientMap.put(originalId, fhirId);
				logger.debug("Adding patient info to patientMap " + originalId + "->" + fhirId);
				responseEntries.add(addResponseEntry("201 Created", "Patient/" + fhirId));
			}
		}

		// Now process the rest.
		for (Resource resource : resources) {
			if (resource.getResourceType() == ResourceType.Patient) {
				// already done.
				continue;
			}

			if (resource.getResourceType() == ResourceType.Observation) {
				Observation observation = (Observation) resource;
				Reference subject = observation.getSubject();
				IdType refIdType = linkToPatient(subject, patientMap);
				if (refIdType == null)
					continue;
				observation.setSubject(new Reference(refIdType));

				logger.debug("Setting patient to Obs: "+observation.getSubject().getReference());
				Long fhirId = OmopObservation.getInstance().toDbase(observation, null);
				BundleEntryComponent newEntry;
				if (fhirId == null || fhirId == 0L) {
					newEntry = addResponseEntry("400 Bad Request", null);
					newEntry.setResource(observation);
				} else {
					newEntry = addResponseEntry("201 Created", "Observation/" + fhirId);
				}

				responseEntries.add(newEntry);
			}
		}

		return responseEntries;
	}
}
