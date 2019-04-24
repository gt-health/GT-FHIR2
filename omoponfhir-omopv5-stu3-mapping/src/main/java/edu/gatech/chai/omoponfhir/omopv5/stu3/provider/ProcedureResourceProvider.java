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

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Procedure;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

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
import edu.gatech.chai.omoponfhir.omopv5.stu3.mapping.OmopProcedure;
import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.ThrowFHIRExceptions;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;

public class ProcedureResourceProvider implements IResourceProvider {

	private WebApplicationContext myAppCtx;
	private String myDbType;
	private OmopProcedure myMapper;
	private int preferredPageSize = 30;

	public ProcedureResourceProvider() {
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
		myDbType = myAppCtx.getServletContext().getInitParameter("backendDbType");
		if (myDbType.equalsIgnoreCase("omopv5") == true) {
			myMapper = new OmopProcedure(myAppCtx);
		} else {
			myMapper = new OmopProcedure(myAppCtx);
		}

		String pageSizeStr = myAppCtx.getServletContext().getInitParameter("preferredPageSize");
		if (pageSizeStr != null && pageSizeStr.isEmpty() == false) {
			int pageSize = Integer.parseInt(pageSizeStr);
			if (pageSize > 0) {
				preferredPageSize = pageSize;
			}
		}
	}

	@Override
	public Class<Procedure> getResourceType() {
		return Procedure.class;
	}

	public static String getType() {
		return "Procedure";
	}

	public OmopProcedure getMyMapper() {
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
	 * The "@Create" annotation indicates that this method implements
	 * "create=type", which adds a new instance of a resource to the server.
	 */
	@Create()
	public MethodOutcome createProcedure(@ResourceParam Procedure theProcedure) {
		validateResource(theProcedure);

		Long id = null;
		try {
			id = myMapper.toDbase(theProcedure, null);
		} catch (FHIRException e) {
			e.printStackTrace();
			ThrowFHIRExceptions.unprocessableEntityException(e.getMessage());
		}
		return new MethodOutcome(new IdDt(id));
	}

	/**
	 * The "@Update" annotation indicates that this method supports replacing an
	 * existing resource (by ID) with a new instance of that resource.
	 * 
	 * @param theId
	 *            This is the ID of the patient to update
	 * @param theProcedure
	 *            This is the actual resource to save
	 * @return This method returns a "MethodOutcome"
	 */
	@Update()
	public MethodOutcome updateProcedure(@IdParam IdType theId, @ResourceParam Procedure theProcedure) {
		validateResource(theProcedure);

		Long fhirId = null;
		try {
			fhirId = myMapper.toDbase(theProcedure, theId);
		} catch (FHIRException e) {
			e.printStackTrace();
			ThrowFHIRExceptions.unprocessableEntityException(e.getMessage());
		}
		if (fhirId == null) {
			throw new ResourceNotFoundException(theId);
		}

		return new MethodOutcome();
	}

	@Delete()
	public void deleteProcedure(@IdParam IdType theId) {
		if (myMapper.removeByFhirId(theId) <= 0) {
			throw new ResourceNotFoundException(theId);
		}
	}

	/**
	 * This is the "read" operation. The "@Read" annotation indicates that this
	 * method supports the read and/or vread operation.
	 * <p>
	 * Read operations take a single parameter annotated with the
	 * {@link IdParam} paramater, and should return a single resource instance.
	 * </p>
	 * 
	 * @param theId
	 *            The read operation takes one parameter, which must be of type
	 *            IdDt and must be annotated with the "@Read.IdParam"
	 *            annotation.
	 * @return Returns a resource matching this identifier, or null if none
	 *         exists.
	 */
	@Read()
	public Procedure readProcedure(@IdParam IdType theId) {
		Procedure retval = myMapper.toFHIR(theId);
		if (retval == null) {
			throw new ResourceNotFoundException(theId);
		}

		return retval;
	}

	@Search()
	public IBundleProvider findProcedureById(
			@RequiredParam(name = Procedure.SP_RES_ID) TokenParam theProcedureId, @IncludeParam(allow = {
					"Procedure:patient", "Procedure:performer", "Procedure:context" }) final Set<Include> theIncludes) {
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper>();

		if (theProcedureId != null) {
			paramList.addAll(myMapper.mapParameter(Procedure.SP_RES_ID, theProcedureId, false));
		}

		MyBundleProvider myBundleProvider = new MyBundleProvider(paramList, theIncludes, null);
		myBundleProvider.setTotalSize(getTotalSize(paramList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);
		return myBundleProvider;
	}

	/**
	 * The "@Search" annotation indicates that this method supports the search
	 * operation. You may have many different method annotated with this
	 * annotation, to support many different search criteria. This example
	 * searches by family name.
	 * 
	 * @param theFamilyName
	 *            This operation takes one parameter which is the search
	 *            criteria. It is annotated with the "@Required" annotation.
	 *            This annotation takes one argument, a string containing the
	 *            name of the search criteria. The datatype here is StringParam,
	 *            but there are other possible parameter types depending on the
	 *            specific search criteria.
	 * @return This method returns a list of Patients in bundle. This list may
	 *         contain multiple matching resources, or it may also be empty.
	 */
	@Search()
	public IBundleProvider findProceduresByParams(@OptionalParam(name = Procedure.SP_CODE) TokenOrListParam theOrCodes,
			@OptionalParam(name = Procedure.SP_CONTEXT) ReferenceParam theContextParam,
			@OptionalParam(name = Procedure.SP_DATE) DateParam theDateParm,
			@OptionalParam(name = Procedure.SP_ENCOUNTER) ReferenceParam theEncounterParam,
			@OptionalParam(name = Procedure.SP_SUBJECT) ReferenceParam theSubjectParam,
			@OptionalParam(name = Procedure.SP_PATIENT) ReferenceParam thePatientParam,
			@OptionalParam(name = Procedure.SP_PERFORMER) ReferenceParam thePerformerParam,

			@IncludeParam(allow = { "Procedure:patient", "Procedure:performer",
					"Procedure:context" }) final Set<Include> theIncludes) {
		/*
		 * Create parameter map, which will be used later to construct
		 * predicate. The predicate construction should depend on the DB schema.
		 * Therefore, we should let our mapper to do any necessary mapping on
		 * the parameter(s). If the FHIR parameter is not mappable, the mapper
		 * should return null, which will be skipped when predicate is
		 * constructed.
		 */
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper>();

		if (theOrCodes != null) {
			List<TokenParam> codes = theOrCodes.getValuesAsQueryTokens();
			boolean orValue = true;
			if (codes.size() <= 1)
				orValue = false;
			for (TokenParam code : codes) {
				paramList.addAll(myMapper.mapParameter(Procedure.SP_CODE, code, orValue));
			}
		}

		if (theContextParam != null) {
			paramList.addAll(myMapper.mapParameter(Procedure.SP_CONTEXT, theContextParam, false));
		}
		if (theDateParm != null) {
			paramList.addAll(myMapper.mapParameter(Procedure.SP_DATE, theDateParm, false));
		}
		if (theEncounterParam != null) {
			paramList.addAll(myMapper.mapParameter(Procedure.SP_ENCOUNTER, theEncounterParam, false));
		}
		if (theSubjectParam != null) {
			if (theSubjectParam.getResourceType().equals(PatientResourceProvider.getType())) {
				thePatientParam = theSubjectParam;
			} else {
				ThrowFHIRExceptions.unprocessableEntityException("We only support Patient resource for subject");
			}
		}
		if (thePatientParam != null) {
			paramList.addAll(myMapper.mapParameter(Procedure.SP_PATIENT, thePatientParam, false));
		}
		if (thePerformerParam != null) {
			paramList.addAll(myMapper.mapParameter(Procedure.SP_PERFORMER, thePerformerParam, false));
		}

		MyBundleProvider myBundleProvider = new MyBundleProvider(paramList, theIncludes, null);
		myBundleProvider.setTotalSize(getTotalSize(paramList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);
		return myBundleProvider;
	}

	/**
	 * This method just provides simple business validation for resources we are
	 * storing.
	 * 
	 * @param thePractitioner
	 *            The thePractitioner to validate
	 */
	private void validateResource(Procedure theProcedure) {
	}

	class MyBundleProvider extends OmopFhirBundleProvider implements IBundleProvider {
		Set<Include> theIncludes;
		Set<Include> theReverseIncludes;

		public MyBundleProvider(List<ParameterWrapper> paramList, Set<Include> theIncludes,
				Set<Include> theReverseIncludes) {
			super(paramList);
			setPreferredPageSize(preferredPageSize);
			this.theIncludes = theIncludes;
			this.theReverseIncludes = theReverseIncludes;
		}

		@Override
		public List<IBaseResource> getResources(int fromIndex, int toIndex) {
			List<IBaseResource> retv = new ArrayList<IBaseResource>();

			// _Include
			List<String> includes = new ArrayList<String>();
			if (theIncludes.contains(new Include("Procedure:patient"))) {
				includes.add("Procedure:patient");
			}

			if (theIncludes.contains(new Include("Procedure:performer"))) {
				includes.add("Procedure:performer");
			}

			if (theIncludes.contains(new Include("Procedure:context"))) {
				includes.add("Procedure:context");
			}

			if (paramList.size() == 0) {
				myMapper.searchWithoutParams(fromIndex, toIndex, retv, includes, null);
			} else {
				myMapper.searchWithParams(fromIndex, toIndex, paramList, retv, includes, null);
			}

			return retv;
		}

	}
}
