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
package edu.gatech.chai.omoponfhir.omopv5.stu3.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import edu.gatech.chai.omoponfhir.omopv5.stu3.mapping.OmopObservation;
import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.ThrowFHIRExceptions;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;

public class ObservationResourceProvider implements IResourceProvider {

	private WebApplicationContext myAppCtx;
	private String myDbType;
	private OmopObservation myMapper;
	private int preferredPageSize = 30;

	public ObservationResourceProvider() {
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
		myDbType = myAppCtx.getServletContext().getInitParameter("backendDbType");
		if (myDbType.equalsIgnoreCase("omopv5") == true) {
			myMapper = new OmopObservation(myAppCtx);
		} else {
			myMapper = new OmopObservation(myAppCtx);
		}
		
		String pageSizeStr = myAppCtx.getServletContext().getInitParameter("preferredPageSize");
		if (pageSizeStr != null && pageSizeStr.isEmpty() == false) {
			int pageSize = Integer.parseInt(pageSizeStr);
			if (pageSize > 0) {
				preferredPageSize = pageSize;
			} 
		}
	}
	
	public static String getType() {
		return "Observation";
	}

	public OmopObservation getMyMapper() {
		return myMapper;
	}
	
	private Integer getTotalSize(List<ParameterWrapper> paramList) {
		final Long totalSize;
		if (paramList.size() == 0) {
			totalSize = getMyMapper().getSize();
		} else {
			totalSize = getMyMapper().getSize(paramList);
		}
		
		return totalSize.intValue();
	}


	/**
	 * The "@Create" annotation indicates that this method implements "create=type", which adds a 
	 * new instance of a resource to the server.
	 */
	@Create()
	public MethodOutcome createPatient(@ResourceParam Observation theObservation) {
		validateResource(theObservation);
		
		Long id = null;
		try {
			id = getMyMapper().toDbase(theObservation, null);
		} catch (FHIRException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (id == null) {
			OperationOutcome outcome = new OperationOutcome();
			CodeableConcept detailCode = new CodeableConcept();
			detailCode.setText("Failed to create entity.");
			outcome.addIssue().setSeverity(IssueSeverity.FATAL).setDetails(detailCode);
			throw new UnprocessableEntityException(FhirContext.forDstu3(), outcome);
		}
		return new MethodOutcome(new IdDt(id));
	}

	@Delete()
	public void deleteObservation(@IdParam IdType theId) {
		if (getMyMapper().removeByFhirId(theId) <= 0) {
			throw new ResourceNotFoundException(theId);
		}
	}

	@Search()
	public IBundleProvider findObservationsById(
			@RequiredParam(name=Observation.SP_RES_ID) TokenParam theObservationId,

			@IncludeParam(allow={"Observation:based-on", "Observation:context", 
					"Observation:device", "Observation:encounter", "Observation:patient", 
					"Observation:performer", "Observation:related-target", 
					"Observation:specimen", "Observation:subject"})
			final Set<Include> theIncludes,
			
			@IncludeParam(reverse=true)
            final Set<Include> theReverseIncludes
			) {
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper> ();

		if (theObservationId != null) {
			paramList.addAll(getMyMapper().mapParameter (Observation.SP_RES_ID, theObservationId, false));
		}

		MyBundleProvider myBundleProvider = new MyBundleProvider(paramList, theIncludes, theReverseIncludes);
		myBundleProvider.setTotalSize(getTotalSize(paramList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);
		return myBundleProvider;
	}
	
	@Search()
	public IBundleProvider findObservationsByParams(
			@OptionalParam(name=Observation.SP_CODE) TokenOrListParam theOrCodes,
			@OptionalParam(name=Observation.SP_DATE) DateParam theDate,
			@OptionalParam(name=Observation.SP_PATIENT, chainWhitelist={"", Patient.SP_NAME, Patient.SP_IDENTIFIER}) ReferenceParam thePatient,
			@OptionalParam(name=Observation.SP_SUBJECT, chainWhitelist={"", Patient.SP_NAME, Patient.SP_IDENTIFIER}) ReferenceParam theSubject,

			@IncludeParam(allow={"Observation:based-on", "Observation:context", 
					"Observation:device", "Observation:encounter", "Observation:patient", 
					"Observation:performer", "Observation:related-target", 
					"Observation:specimen", "Observation:subject"})
			final Set<Include> theIncludes,
			
			@IncludeParam(reverse=true)
            final Set<Include> theReverseIncludes
			) {		
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper> ();

		if (theOrCodes != null) {
			List<TokenParam> codes = theOrCodes.getValuesAsQueryTokens();
			boolean orValue = true;
			if (codes.size() <= 1)
				orValue = false;
			for (TokenParam code : codes) {
				paramList.addAll(getMyMapper().mapParameter(Observation.SP_CODE, code, orValue));
			}
		}
		
		if (theDate != null) {
			paramList.addAll(getMyMapper().mapParameter(Observation.SP_DATE, theDate, false));
		}
		
		// With OMOP, we only support subject to be patient.
		// If the subject has only ID part, we assume that is patient.
		if (theSubject != null) {
			if (theSubject.getResourceType() != null && 
					theSubject.getResourceType().equals(PatientResourceProvider.getType())) {
				thePatient = theSubject;
			} else {
				// If resource is null, we assume Patient.
				if (theSubject.getResourceType() == null) {
					thePatient = theSubject;
				} else {
					ThrowFHIRExceptions.unprocessableEntityException("subject search allows Only Patient Resource, but provided "+theSubject.getResourceType());
				}
			}
		}
		
		if (thePatient != null) {
			String patientChain = thePatient.getChain();
			if (patientChain != null) {
				if (Patient.SP_NAME.equals(patientChain)) {
					String thePatientName = thePatient.getValue();
					paramList.addAll(getMyMapper().mapParameter ("Patient:"+Patient.SP_NAME, thePatientName, false));
				} else if (Patient.SP_IDENTIFIER.equals(patientChain)) {
					paramList.addAll(getMyMapper().mapParameter ("Patient:"+Patient.SP_IDENTIFIER, thePatient.getValue(), false));
				} else if ("".equals(patientChain)) {
					paramList.addAll(getMyMapper().mapParameter ("Patient:"+Patient.SP_RES_ID, thePatient.getValue(), false));
				}
			} else {
				paramList.addAll(getMyMapper().mapParameter ("Patient:"+Patient.SP_RES_ID, thePatient.getIdPart(), false));
			}
		}
		
		MyBundleProvider myBundleProvider = new MyBundleProvider(paramList, theIncludes, theReverseIncludes);
		myBundleProvider.setTotalSize(getTotalSize(paramList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);
		return myBundleProvider;
	}
	
	/**
	 * This is the "read" operation. The "@Read" annotation indicates that this method supports the read and/or vread operation.
	 * <p>
	 * Read operations take a single parameter annotated with the {@link IdParam} paramater, and should return a single resource instance.
	 * </p>
	 * 
	 * @param theId
	 *            The read operation takes one parameter, which must be of type IdDt and must be annotated with the "@Read.IdParam" annotation.
	 * @return Returns a resource matching this identifier, or null if none exists.
	 */
	@Read()
	public Observation readObservation(@IdParam IdType theId) {
		Observation retval = (Observation) getMyMapper().toFHIR(theId);
		if (retval == null) {
			throw new ResourceNotFoundException(theId);
		}
			
		return retval;
	}

	/**
	 * The "@Update" annotation indicates that this method supports replacing an existing 
	 * resource (by ID) with a new instance of that resource.
	 * 
	 * @param theId
	 *            This is the ID of the patient to update
	 * @param thePatient
	 *            This is the actual resource to save
	 * @return This method returns a "MethodOutcome"
	 */
	@Update()
	public MethodOutcome updateObservation(@IdParam IdType theId, @ResourceParam Observation theObservation) {
		validateResource(theObservation);
		
		Long fhirId=null;
		try {
			fhirId = getMyMapper().toDbase(theObservation, theId);
		} catch (FHIRException e) {
			e.printStackTrace();
		}

		if (fhirId == null) {
			throw new ResourceNotFoundException(theId);
		}

		return new MethodOutcome();
	}
	
	// TODO: Add more validation code here.
	private void validateResource(Observation theObservation) {
		OperationOutcome outcome = new OperationOutcome();
		CodeableConcept detailCode = new CodeableConcept();
		if (theObservation.getCode().isEmpty()) {
			detailCode.setText("No code is provided.");
			outcome.addIssue().setSeverity(IssueSeverity.FATAL).setDetails(detailCode);
			throw new UnprocessableEntityException(FhirContext.forDstu3(), outcome);
		}
		
		Reference subjectReference = theObservation.getSubject();
		if (subjectReference == null || subjectReference.isEmpty()) {
			detailCode.setText("Subject cannot be empty for OmopOnFHIR");
			outcome.addIssue().setSeverity(IssueSeverity.FATAL).setDetails(detailCode);
			throw new UnprocessableEntityException(FhirContext.forDstu3(), outcome);
		}
		
		String subjectId = subjectReference.getId();
		if (!subjectId.contentEquals("Patient")) {
			detailCode.setText("Subject must be Patient resource for OmopOnFHIR");
			outcome.addIssue().setSeverity(IssueSeverity.FATAL).setDetails(detailCode);
			throw new UnprocessableEntityException(FhirContext.forDstu3(), outcome);
		}
	}

	@Override
	public Class<Observation> getResourceType() {
		return Observation.class;
	}

	class MyBundleProvider extends OmopFhirBundleProvider implements IBundleProvider {
		Set<Include> theIncludes;
		Set<Include> theReverseIncludes;

		public MyBundleProvider(List<ParameterWrapper> paramList, Set<Include> theIncludes, Set<Include>theReverseIncludes) {
			super(paramList);
			setPreferredPageSize (preferredPageSize);
			this.theIncludes = theIncludes;
			this.theReverseIncludes = theReverseIncludes;
		}

		@Override
		public List<IBaseResource> getResources(int fromIndex, int toIndex) {
			List<IBaseResource> retv = new ArrayList<IBaseResource>();
			
			// _Include
			List<String> includes = new ArrayList<String>();
			
			if (theIncludes.contains(new Include("Observation:based-on"))) {
				includes.add("Observation:based-on");
			}
			
			if (theIncludes.contains(new Include("Observation:context"))) {
				includes.add("Observation:context");
			}

			if (theIncludes.contains(new Include("Observation:device"))) {
				includes.add("Observation:device");
			}
			
			if (theIncludes.contains(new Include("Observation:encounter"))) {
				includes.add("Observation:encounter");
			}

			if (theIncludes.contains(new Include("Observation:patient"))) {
				includes.add("Observation:patient");
			}

			if (theIncludes.contains(new Include("Observation:performer"))) {
				includes.add("Observation:performer");
			}

			if (theIncludes.contains(new Include("Observation:related-target"))) {
				includes.add("Observation:related-target");
			}

			if (theIncludes.contains(new Include("Observation:specimen"))) {
				includes.add("Observation:specimen");
			}

			if (theIncludes.contains(new Include("Observation:subject"))) {
				includes.add("Observation:subject");
			}

			if (paramList.size() == 0) {
				getMyMapper().searchWithoutParams(fromIndex, toIndex, retv, includes, null);
			} else {
				getMyMapper().searchWithParams(fromIndex, toIndex, paramList, retv, includes, null);
			}

			return retv;
		}
	}
}
