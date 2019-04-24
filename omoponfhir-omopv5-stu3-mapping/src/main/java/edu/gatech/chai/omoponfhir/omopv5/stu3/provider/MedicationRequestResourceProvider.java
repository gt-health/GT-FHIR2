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

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
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
import edu.gatech.chai.omoponfhir.omopv5.stu3.mapping.OmopMedicationRequest;
import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.ThrowFHIRExceptions;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;

public class MedicationRequestResourceProvider implements IResourceProvider {

	private WebApplicationContext myAppCtx;
	private String myDbType;
	private OmopMedicationRequest myMapper;
	private int preferredPageSize = 30;

	public MedicationRequestResourceProvider() {
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
		myDbType = myAppCtx.getServletContext().getInitParameter("backendDbType");
		if (myDbType.equalsIgnoreCase("omopv5") == true) {
			myMapper = new OmopMedicationRequest(myAppCtx);
		} else {
			myMapper = new OmopMedicationRequest(myAppCtx);
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
		return "MedicationRequest";
	}

    public OmopMedicationRequest getMyMapper() {
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

	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return MedicationRequest.class;
	}

	/**
	 * The "@Create" annotation indicates that this method implements "create=type", which adds a 
	 * new instance of a resource to the server.
	 */
	@Create()
	public MethodOutcome createMedicationRequest(@ResourceParam MedicationRequest theMedicationRequest) {
		validateResource(theMedicationRequest);
		
		Long id=null;
		try {
			id = myMapper.toDbase(theMedicationRequest, null);
		} catch (FHIRException e) {
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
	public void deleteMedicationRequest(@IdParam IdType theId) {
		if (myMapper.removeByFhirId(theId) <= 0) {
			throw new ResourceNotFoundException(theId);
		}
	}

	@Update()
	public MethodOutcome updateMedicationRequest(@IdParam IdType theId, @ResourceParam MedicationRequest theMedicationRequest) {
		validateResource(theMedicationRequest);
		
		Long fhirId=null;
		try {
			fhirId = myMapper.toDbase(theMedicationRequest, theId);
		} catch (FHIRException e) {
			e.printStackTrace();
		}

		if (fhirId == null) {
			throw new ResourceNotFoundException(theId);
		}

		return new MethodOutcome();
	}

	@Read()
	public MedicationRequest readMedicationRequest(@IdParam IdType theId) {
		MedicationRequest retval = (MedicationRequest) myMapper.toFHIR(theId);
		if (retval == null) {
			throw new ResourceNotFoundException(theId);
		}
			
		return retval;
	}
	
	@Search()
	public IBundleProvider findMedicationRequetsById(
			@RequiredParam(name = MedicationRequest.SP_RES_ID) TokenParam theMedicationRequestId
			) {
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper> ();

		if (theMedicationRequestId != null) {
			paramList.addAll(myMapper.mapParameter (MedicationRequest.SP_RES_ID, theMedicationRequestId, false));
		}
				
		MyBundleProvider myBundleProvider = new MyBundleProvider(paramList);
		myBundleProvider.setTotalSize(getTotalSize(paramList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);

		return myBundleProvider;
	}

	@Search()
	public IBundleProvider findMedicationRequestsByParams(
			@OptionalParam(name = MedicationRequest.SP_CODE) TokenOrListParam theOrCodes,
			@OptionalParam(name = MedicationRequest.SP_CONTEXT) ReferenceParam theContext,
			@OptionalParam(name = MedicationRequest.SP_AUTHOREDON) DateParam theDate,
			@OptionalParam(name = MedicationRequest.SP_PATIENT) ReferenceParam thePatient,
			@OptionalParam(name = MedicationRequest.SP_SUBJECT) ReferenceParam theSubject
			) {
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper> ();
		
		if (theOrCodes != null) {
			List<TokenParam> codes = theOrCodes.getValuesAsQueryTokens();
			boolean orValue = true;
			if (codes.size() <= 1)
				orValue = false;
			for (TokenParam code : codes) {
				paramList.addAll(myMapper.mapParameter(MedicationRequest.SP_CODE, code, orValue));
			}
		}

		if (theContext != null) {
			paramList.addAll(myMapper.mapParameter (MedicationRequest.SP_CONTEXT, theContext, false));
		}

		if (theDate != null) {
			paramList.addAll(myMapper.mapParameter (MedicationRequest.SP_AUTHOREDON, theDate, false));
		}


		if (theSubject != null) {
			if (theSubject.getResourceType().equals(PatientResourceProvider.getType())) {
				thePatient = theSubject;
			} else {
				ThrowFHIRExceptions.unprocessableEntityException("We only support Patient resource for subject");
			}
		}
		if (thePatient != null) {
			paramList.addAll(myMapper.mapParameter(MedicationRequest.SP_PATIENT, thePatient, false));
		}

		MyBundleProvider myBundleProvider = new MyBundleProvider(paramList);
		myBundleProvider.setTotalSize(getTotalSize(paramList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);
		
		return myBundleProvider;
	}
	
//	private void mapParameter(Map<String, List<ParameterWrapper>> paramMap, String FHIRparam, Object value, boolean or) {
//		List<ParameterWrapper> paramList = myMapper.mapParameter(FHIRparam, value, or);
//		if (paramList != null) {
//			paramMap.put(FHIRparam, paramList);
//		}
//	}

	private void validateResource(MedicationRequest theMedication) {
		// TODO: implement validation method
	}
	
	class MyBundleProvider extends OmopFhirBundleProvider implements IBundleProvider {
		public MyBundleProvider(List<ParameterWrapper> paramList) {
			super(paramList);
			setPreferredPageSize (preferredPageSize);
		}

		@Override
		public List<IBaseResource> getResources(int fromIndex, int toIndex) {
			List<IBaseResource> retv = new ArrayList<IBaseResource>();

			// _Include
			List<String> includes = new ArrayList<String>();

			if (paramList.size() == 0) {
				myMapper.searchWithoutParams(fromIndex, toIndex, retv, includes, null);
			} else {
				myMapper.searchWithParams(fromIndex, toIndex, paramList, retv, includes, null);
			}

			return retv;
		}		
	}
}
