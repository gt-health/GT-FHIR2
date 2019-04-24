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

import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
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
import edu.gatech.chai.omoponfhir.omopv5.stu3.mapping.OmopDocumentReference;
import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.ThrowFHIRExceptions;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;

public class DocumentReferenceResourceProvider implements IResourceProvider {

	private WebApplicationContext myAppCtx;
	private String myDbType;
	private OmopDocumentReference myMapper;
	private int preferredPageSize = 30;

	public DocumentReferenceResourceProvider() {
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
		myDbType = myAppCtx.getServletContext().getInitParameter("backendDbType");
		if (myDbType.equalsIgnoreCase("omopv5") == true) {
			myMapper = new OmopDocumentReference(myAppCtx);
		} else {
			myMapper = new OmopDocumentReference(myAppCtx);
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
		return "DocumentReference";
	}

	@Override
	public Class<DocumentReference> getResourceType() {
		return DocumentReference.class;
	}
	
	public OmopDocumentReference getMyMapper() {
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

	@Create()
	public MethodOutcome createDocumentReference(@ResourceParam DocumentReference theDocumentReference) {
		validateResource(theDocumentReference);

		Long id = null;
		try {
			id = getMyMapper().toDbase(theDocumentReference, null);
		} catch (FHIRException e) {
			e.printStackTrace();
			ThrowFHIRExceptions.unprocessableEntityException(e.getMessage());
		}
		
		return new MethodOutcome(new IdDt(id));
	}
	
	@Delete()
	public void deleteDocumentReference(@IdParam IdType theId) {
		if (getMyMapper().removeByFhirId(theId) <= 0) {
			throw new ResourceNotFoundException(theId);
		}
	}
	
	@Read()
	public DocumentReference readDocumentReference(@IdParam IdType theId) {
		DocumentReference retval = (DocumentReference) getMyMapper().toFHIR(theId);
		if (retval == null) {
			throw new ResourceNotFoundException(theId);
		}

		return retval;
	}

	@Update()
	public MethodOutcome updateDocumentReference(@IdParam IdType theId, @ResourceParam DocumentReference theDocumentReference) {
		validateResource(theDocumentReference);

		Long fhirId = null;
		try {
			fhirId = getMyMapper().toDbase(theDocumentReference, theId);
		} catch (FHIRException e) {
			e.printStackTrace();
		}
		if (fhirId == null) {
			throw new ResourceNotFoundException(theId);
		}

		return new MethodOutcome();
	}

	@Search()
	public IBundleProvider findDocumentReferenceById(
			@RequiredParam(name=DocumentReference.SP_RES_ID) TokenParam theDocumentReferenceId,
			
			@IncludeParam(allow={"DocumentReference:patient", "DocumentReference:subject", 
			"DocumentReference:encounter"})
			final Set<Include> theIncludes,
			
			@IncludeParam(reverse=true)
            final Set<Include> theReverseIncludes
			) {
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper> ();

		if (theDocumentReferenceId != null) {
			paramList.addAll(getMyMapper().mapParameter (DocumentReference.SP_RES_ID, theDocumentReferenceId, false));
		}

		MyBundleProvider myBundleProvider = new MyBundleProvider(paramList, theIncludes, theReverseIncludes);
		myBundleProvider.setTotalSize(getTotalSize(paramList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);
		return myBundleProvider;
	}
	
	@Search()
	public IBundleProvider findDocumentReferenceByParams(
			@OptionalParam(name=DocumentReference.SP_PATIENT, chainWhitelist = { "", Patient.SP_NAME }) ReferenceParam thePatient,
			@OptionalParam(name=DocumentReference.SP_SUBJECT, chainWhitelist = { "", Patient.SP_NAME }) ReferenceParam theSubject,
			@OptionalParam(name=DocumentReference.SP_ENCOUNTER) ReferenceParam theEncounter,
			@OptionalParam(name=DocumentReference.SP_TYPE) TokenOrListParam theOrType,
			@OptionalParam(name=DocumentReference.SP_CREATED) DateParam theCreated,
			@OptionalParam(name=DocumentReference.SP_INDEXED) DateParam theIndexed,
			
			@IncludeParam(allow={"DocumentReference:patient", "DocumentReference:subject", 
					"DocumentReference:encounter"})
			final Set<Include> theIncludes,
			
			@IncludeParam(reverse=true)
            final Set<Include> theReverseIncludes
			) {
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper> ();

		if (theOrType != null) {
			List<TokenParam> codes = theOrType.getValuesAsQueryTokens();
			boolean orValue = true;
			if (codes.size() <= 1)
				orValue = false;
			for (TokenParam code : codes) {
				paramList.addAll(getMyMapper().mapParameter(DocumentReference.SP_TYPE, code, orValue));
			}
		}

		if (theCreated != null) {
			paramList.addAll(getMyMapper().mapParameter(DocumentReference.SP_CREATED, theCreated, false));
		}

		if (theIndexed != null) {
			paramList.addAll(getMyMapper().mapParameter(DocumentReference.SP_INDEXED, theIndexed, false));
		}

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
				} else if ("".equals(patientChain)) {
					paramList.addAll(getMyMapper().mapParameter ("Patient:"+Patient.SP_RES_ID, thePatient.getValue(), false));
				}
			} else {
				paramList.addAll(getMyMapper().mapParameter ("Patient:"+Patient.SP_RES_ID, thePatient.getIdPart(), false));
			}
		}

		if (theEncounter != null) {
			paramList.addAll(myMapper.mapParameter(DocumentReference.SP_ENCOUNTER, theEncounter, false));
		}

		MyBundleProvider myBundleProvider = new MyBundleProvider(paramList, theIncludes, theReverseIncludes);
		myBundleProvider.setTotalSize(getTotalSize(paramList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);
		return myBundleProvider;		
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

			if (theIncludes.contains(new Include("DocumentReference:encounter"))) {
				includes.add("DocumentReference:encounter");
			}

			if (theIncludes.contains(new Include("DocumentReference:patient"))) {
				includes.add("DocumentReference:patient");
			}

			if (theIncludes.contains(new Include("DocumentReference:subject"))) {
				includes.add("DocumentReference:subject");
			}

			if (paramList.size() == 0) {
				getMyMapper().searchWithoutParams(fromIndex, toIndex, retv, includes, null);
			} else {
				getMyMapper().searchWithParams(fromIndex, toIndex, paramList, retv, includes, null);
			}

			return retv;
		}
	}
	
	// TODO: Add more validation code here.
	private void validateResource(DocumentReference theDocumentReference) {
	}

}
