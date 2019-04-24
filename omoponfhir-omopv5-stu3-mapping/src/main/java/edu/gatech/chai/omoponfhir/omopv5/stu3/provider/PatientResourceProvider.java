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
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.dstu3.model.Organization;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import edu.gatech.chai.omoponfhir.omopv5.stu3.mapping.OmopPatient;
import edu.gatech.chai.omoponfhir.omopv5.stu3.model.USCorePatient;
import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.ThrowFHIRExceptions;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;

/**
 * This is a resource provider which stores Patient resources in memory using a
 * HashMap. This is obviously not a production-ready solution for many reasons,
 * but it is useful to help illustrate how to build a fully-functional server.
 */
public class PatientResourceProvider implements IResourceProvider {

	private WebApplicationContext myAppCtx;
	private String myDbType;
	private OmopPatient myMapper;
	private int preferredPageSize = 30;

	public PatientResourceProvider() {
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
		myDbType = myAppCtx.getServletContext().getInitParameter("backendDbType");
		if (myDbType.equalsIgnoreCase("omopv5") == true) {
			myMapper = new OmopPatient(myAppCtx);
		} else {
			myMapper = new OmopPatient(myAppCtx);
		}

		String pageSizeStr = myAppCtx.getServletContext().getInitParameter("preferredPageSize");
		if (pageSizeStr != null && pageSizeStr.isEmpty() == false) {
			int pageSize = Integer.parseInt(pageSizeStr);
			if (pageSize > 0) {
				preferredPageSize = pageSize;
			}
		}
	}

	/**
	 * The getResourceType method comes from IResourceProvider, and must be
	 * overridden to indicate what type of resource this provider supplies.
	 */
	@Override
	public Class<Patient> getResourceType() {
		return Patient.class;
	}

	public static String getType() {
		return "Patient";
	}

	public OmopPatient getMyMapper() {
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
	 * The "@Create" annotation indicates that this method implements "create=type",
	 * which adds a new instance of a resource to the server.
	 */
	@Create()
	public MethodOutcome createPatient(@ResourceParam USCorePatient thePatient) {
		validateResource(thePatient);

		Long id = null;
		try {
			id = getMyMapper().toDbase(thePatient, null);
		} catch (FHIRException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new MethodOutcome(new IdType(id));
	}

	@Delete()
	public void deletePatient(@IdParam IdType theId) {
		if (getMyMapper().removeByFhirId(theId) <= 0) {
			throw new ResourceNotFoundException(theId);
		}
	}

	private String constructOrderParams(SortSpec theSort) {
		if (theSort == null) return null;
		
		String orderParams = new String();
		String direction;
		
		if (theSort.getOrder() != null) direction = theSort.getOrder().toString();
		else direction = "ASC";

		String orderParam = getMyMapper().constructSort(theSort.getParamName(), direction);
		if (orderParams.isEmpty()) orderParams = orderParams.concat(orderParam);
		else orderParams = orderParams.concat(","+orderParam);
		
		if (theSort.getChain() != null) { 
			orderParams = orderParams.concat(constructOrderParams(theSort.getChain()));
		}
		
		return orderParams;
	}
	
	/**
	 * The "@Search" annotation indicates that this method supports the search
	 * operation. You may have many different method annotated with this annotation,
	 * to support many different search criteria. This example searches by family
	 * name.
	 * 
	 * @param theFamilyName This operation takes one parameter which is the search
	 *                      criteria. It is annotated with the "@Required"
	 *                      annotation. This annotation takes one argument, a string
	 *                      containing the name of the search criteria. The datatype
	 *                      here is StringParam, but there are other possible
	 *                      parameter types depending on the specific search
	 *                      criteria.
	 * @return This method returns a list of Patients in bundle. This list may
	 *         contain multiple matching resources, or it may also be empty.
	 */
	@Search(allowUnknownParams=true)
	public IBundleProvider findPatientsByParams(RequestDetails theRequestDetails, @OptionalParam(name = Patient.SP_RES_ID) TokenParam thePatientId,
			@OptionalParam(name = Patient.SP_IDENTIFIER) TokenParam thePatientIdentifier,
			@OptionalParam(name = Patient.SP_ACTIVE) TokenParam theActive,
			@OptionalParam(name = Patient.SP_FAMILY) StringParam theFamilyName,
			@OptionalParam(name = Patient.SP_GIVEN) StringParam theGivenName,
			@OptionalParam(name = Patient.SP_NAME) StringParam theName,
			@OptionalParam(name = Patient.SP_BIRTHDATE) DateParam theBirthDate,
			@OptionalParam(name = Patient.SP_ADDRESS) StringParam theAddress,
			@OptionalParam(name = Patient.SP_ADDRESS_CITY) StringParam theAddressCity,
			@OptionalParam(name = Patient.SP_ADDRESS_STATE) StringParam theAddressState,
			@OptionalParam(name = Patient.SP_ADDRESS_POSTALCODE) StringParam theAddressZip,
			@OptionalParam(name = Patient.SP_EMAIL) TokenParam theEmail,
			@OptionalParam(name = Patient.SP_PHONE) TokenParam thePhone,
			@OptionalParam(name = Patient.SP_TELECOM) TokenParam theTelecom,
			@OptionalParam(name = Patient.SP_ORGANIZATION, chainWhitelist = { "",
					Organization.SP_NAME }) ReferenceParam theOrganization,
			@Sort SortSpec theSort,
			
			@IncludeParam(allow = { "Patient:general-practitioner", "Patient:organization",
					"Patient:link" }) final Set<Include> theIncludes,

			@IncludeParam(allow = { "Encounter:subject",
					"Observation:subject" }, reverse = true) final Set<Include> theReverseIncludes) {
		
		/*
		 * Create parameter map, which will be used later to construct predicate. The
		 * predicate construction should depend on the DB schema. Therefore, we should
		 * let our mapper to do any necessary mapping on the parameter(s). If the FHIR
		 * parameter is not mappable, the mapper should return null, which will be
		 * skipped when predicate is constructed.
		 */
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper>();

		if (thePatientId != null) {
			paramList.addAll(getMyMapper().mapParameter(Patient.SP_RES_ID, thePatientId, false));
		}
		if (thePatientIdentifier != null) {
			paramList.addAll(getMyMapper().mapParameter(Patient.SP_IDENTIFIER, thePatientIdentifier, false));
		}
		if (theActive != null) {
			paramList.addAll(getMyMapper().mapParameter(Patient.SP_ACTIVE, theActive, false));
		}
		if (theEmail != null) {
			paramList.addAll(getMyMapper().mapParameter(Patient.SP_EMAIL, theEmail, false));
		}
		if (thePhone != null) {
			paramList.addAll(getMyMapper().mapParameter(Patient.SP_PHONE, thePhone, false));
		}
		if (theTelecom != null) {
			paramList.addAll(getMyMapper().mapParameter(Patient.SP_TELECOM, theTelecom, false));
		}
		if (theFamilyName != null) {
			paramList.addAll(getMyMapper().mapParameter(Patient.SP_FAMILY, theFamilyName, false));
		}
		if (theName != null) {
			paramList.addAll(getMyMapper().mapParameter(Patient.SP_NAME, theName, false));
		}
		if (theGivenName != null) {
			paramList.addAll(getMyMapper().mapParameter(Patient.SP_GIVEN, theGivenName, false));
		}
		if (theBirthDate != null) {
			paramList.addAll(getMyMapper().mapParameter(Patient.SP_BIRTHDATE, theBirthDate, false));
		}
		if (theAddress != null) {
			paramList.addAll(getMyMapper().mapParameter(Patient.SP_ADDRESS, theAddress, false));
		}
		if (theAddressCity != null) {
			paramList.addAll(getMyMapper().mapParameter(Patient.SP_ADDRESS_CITY, theAddressCity, false));
		}
		if (theAddressState != null) {
			paramList.addAll(getMyMapper().mapParameter(Patient.SP_ADDRESS_STATE, theAddressState, false));
		}
		if (theAddressZip != null) {
			paramList.addAll(getMyMapper().mapParameter(Patient.SP_ADDRESS_POSTALCODE, theAddressZip, false));
		}

		// Chain Search.
		// Chain search is a searching by reference with specific field name (including
		// reference ID).
		// As SP names are not unique across the FHIR resources, we need to tag the name
		// of the resource in front to indicate our OMOP* can handle these parameters.
		if (theOrganization != null) {
			String orgChain = theOrganization.getChain();
			if (orgChain != null) {
				if (Organization.SP_NAME.equals(orgChain)) {
					String theOrgName = theOrganization.getValue();
					paramList.addAll(
							getMyMapper().mapParameter("Organization:" + Organization.SP_NAME, theOrgName, false));
				} else if ("".equals(orgChain)) {
					paramList.addAll(getMyMapper().mapParameter("Organization:" + Organization.SP_RES_ID,
							theOrganization.getValue(), false));
				}
			} else {
				paramList.addAll(getMyMapper().mapParameter("Organization:" + Organization.SP_RES_ID,
						theOrganization.getIdPart(), false));
			}
		}
		
		String orderParams = constructOrderParams(theSort);
		System.out.println("MYSORT!!! "+orderParams);

		MyBundleProvider myBundleProvider = new MyBundleProvider(paramList, theIncludes, theReverseIncludes);
		myBundleProvider.setTotalSize(getTotalSize(paramList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);
		myBundleProvider.setOrderParams(orderParams);
		
		return myBundleProvider;

	}

	/**
	 * This is the "read" operation. The "@Read" annotation indicates that this
	 * method supports the read and/or vread operation.
	 * <p>
	 * Read operations take a single parameter annotated with the {@link IdParam}
	 * paramater, and should return a single resource instance.
	 * </p>
	 * 
	 * @param theId The read operation takes one parameter, which must be of type
	 *              IdDt and must be annotated with the "@Read.IdParam" annotation.
	 * @return Returns a resource matching this identifier, or null if none exists.
	 */
	@Read()
	public Patient readPatient(@IdParam IdType theId) {
		Patient retval = (Patient) getMyMapper().toFHIR(theId);
		if (retval == null) {
			throw new ResourceNotFoundException(theId);
		}

		return retval;
	}

	/**
	 * The "@Update" annotation indicates that this method supports replacing an
	 * existing resource (by ID) with a new instance of that resource.
	 * 
	 * @param theId      This is the ID of the patient to update
	 * @param thePatient This is the actual resource to save
	 * @return This method returns a "MethodOutcome"
	 */
	@Update()
	public MethodOutcome updatePatient(@IdParam IdType theId, @ResourceParam USCorePatient thePatient) {
		validateResource(thePatient);

		Long fhirId = null;
		try {
			fhirId = getMyMapper().toDbase(thePatient, theId);
		} catch (FHIRException e) {
			e.printStackTrace();
		}
		if (fhirId == null) {
			throw new ResourceNotFoundException(theId);
		}

		return new MethodOutcome();
	}

	/**
	 * $everything operation for a single patient.
	 */
	@Operation(name = "$everything", idempotent = true, bundleType = BundleTypeEnum.SEARCHSET)
	public IBundleProvider patientEverythingOperation(RequestDetails theRequestDetails, @IdParam IdType thePatientId, @OperationParam(name = "start") DateType theStart,
			@OperationParam(name = "end") DateType theEnd) {

		if (thePatientId == null) {
			ThrowFHIRExceptions.unprocessableEntityException("Patient Id must be present");
		}
		
		Date startDate = null;
		if (theStart != null) startDate = theStart.getValue();
		
		Date endDate = null;
		if (theEnd != null) endDate = theEnd.getValue();

//		final IdType patientId = thePatientId;
//		final String baseUrl = theRequestDetails.getFhirServerBase();
		
		List<IBaseResource> resources = new ArrayList<IBaseResource>();
		resources.add(getMyMapper().toFHIR(thePatientId));

		getMyMapper().getEverthingfor(resources, thePatientId.getIdPartAsLong(), startDate, endDate);
		
		final List<IBaseResource> retv = resources;
		final Integer totalsize = retv.size();
		final Integer pageSize = totalsize; // Some clients do not support pagination. preferredPageSize;
		
		return new IBundleProvider() {
			@Override
			public IPrimitiveType<Date> getPublished() {
				return null;
			}

			@Override
			public List<IBaseResource> getResources(int theFromIndex, int theToIndex) {
				return retv.subList(theFromIndex, theToIndex);
			}

			@Override
			public String getUuid() {
				return null;
			}

			@Override
			public Integer preferredPageSize() {
				return pageSize;
			}

			@Override
			public Integer size() {
				return totalsize;
			}
		};
	}

	/**
	 * This method just provides simple business validation for resources we are
	 * storing.
	 * 
	 * @param thePatient The patient to validate
	 */
	private void validateResource(Patient thePatient) {
		/*
		 * Our server will have a rule that patients must have a family name or we will
		 * reject them
		 */
		if (thePatient.getNameFirstRep().getFamily().isEmpty()) {
			OperationOutcome outcome = new OperationOutcome();
			CodeableConcept detailCode = new CodeableConcept();
			detailCode.setText("No family name provided, Patient resources must have at least one family name.");
			outcome.addIssue().setSeverity(IssueSeverity.FATAL).setDetails(detailCode);
			throw new UnprocessableEntityException(FhirContext.forDstu3(), outcome);
		}
	}

	class MyBundleProvider extends OmopFhirBundleProvider implements IBundleProvider {
		Set<Include> theIncludes;
		Set<Include> theReverseIncludes;
		String orderParams = null;

		public MyBundleProvider(List<ParameterWrapper> paramList, Set<Include> theIncludes,
				Set<Include> theReverseIncludes) {
			super(paramList);
			setPreferredPageSize(preferredPageSize);
			this.theIncludes = theIncludes;
			this.theReverseIncludes = theReverseIncludes;
		}

		public String getOrderParams() {
			return this.orderParams;
		}
		
		public void setOrderParams(String orderParams) {
			this.orderParams = orderParams;
		}
		
		@Override
		public List<IBaseResource> getResources(int fromIndex, int toIndex) {
			List<IBaseResource> retv = new ArrayList<IBaseResource>();

			// _Include
			List<String> includes = new ArrayList<String>();
			if (theIncludes.contains(new Include("Patient:general-practitioner"))) {
				includes.add("Patient:general-practitioner");
			}

			if (theIncludes.contains(new Include("Patient:organization"))) {
				includes.add("Patient:organization");
			}

			if (theIncludes.contains(new Include("Patient:link"))) {
				includes.add("Patient:link");
			}

			if (theReverseIncludes.contains(new Include("*"))) {
				// This is to include all the reverse includes...
				includes.add("Encounter:subject");
				includes.add("Observation:subject");
				includes.add("Device:patient");
				includes.add("Condition:subject");
				includes.add("Procedure:subject");
				includes.add("MedicationRequest:subject");
				includes.add("MedicationAdministration:subject");
				includes.add("MedicationDispense:subject");
				includes.add("MedicationStatement:subject");
			} else {
				if (theReverseIncludes.contains(new Include("Encounter:subject"))) {
					includes.add("Encounter:subject");
				}
				if (theReverseIncludes.contains(new Include("Observation:subject"))) {
					includes.add("Observation:subject");
				}
				if (theReverseIncludes.contains(new Include("Device:patient"))) {
					includes.add("Device:patient");
				}
				if (theReverseIncludes.contains(new Include("Condition:subject"))) {
					includes.add("Condition:subject");
				}
				if (theReverseIncludes.contains(new Include("Procedure:subject"))) {
					includes.add("Procedure:subject");
				}
				if (theReverseIncludes.contains(new Include("MedicationRequest:subject"))) {
					includes.add("MedicationRequest:subject");
				}
				if (theReverseIncludes.contains(new Include("MedicationAdministration:subject"))) {
					includes.add("MedicationAdministration:subject");
				}
				if (theReverseIncludes.contains(new Include("MedicationDispense:subject"))) {
					includes.add("MedicationDispense:subject");
				}
				if (theReverseIncludes.contains(new Include("MedicationStatement:subject"))) {
					includes.add("MedicationStatement:subject");
				}
			}

			System.out.println("SORT!!!!!! "+orderParams);
			if (paramList.size() == 0) {
				getMyMapper().searchWithoutParams(fromIndex, toIndex, retv, includes, orderParams);
			} else {
				getMyMapper().searchWithParams(fromIndex, toIndex, paramList, retv, includes, orderParams);
			}

			return retv;
		}

	}

}