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
import org.hl7.fhir.dstu3.model.MedicationStatement;
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
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import edu.gatech.chai.omoponfhir.omopv5.stu3.mapping.OmopMedicationStatement;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;

public class MedicationStatementResourceProvider implements IResourceProvider {

	private WebApplicationContext myAppCtx;
	private String myDbType;
	private OmopMedicationStatement myMapper;
	private int preferredPageSize = 30;

	public MedicationStatementResourceProvider() {
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
		myDbType = myAppCtx.getServletContext().getInitParameter("backendDbType");
		if (myDbType.equalsIgnoreCase("omopv5") == true) {
			myMapper = new OmopMedicationStatement(myAppCtx);
		} else {
			myMapper = new OmopMedicationStatement(myAppCtx);
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
		return "MedicationStatement";
	}

	public OmopMedicationStatement getMyMapper() {
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
	public MethodOutcome createMedicationStatement(@ResourceParam MedicationStatement theMedicationStatement) {
		validateResource(theMedicationStatement);
		
		Long id=null;
		try {
			id = myMapper.toDbase(theMedicationStatement, null);
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
	public void deleteMedicationStatement(@IdParam IdType theId) {
		if (myMapper.removeByFhirId(theId) <= 0) {
			throw new ResourceNotFoundException(theId);
		}
	}

	@Update()
	public MethodOutcome updateMedicationStatement(@IdParam IdType theId, @ResourceParam MedicationStatement theMedicationStatement) {
		validateResource(theMedicationStatement);
		
		Long fhirId=null;
		try {
			fhirId = myMapper.toDbase(theMedicationStatement, theId);
		} catch (FHIRException e) {
			e.printStackTrace();
		}

		if (fhirId == null) {
			throw new ResourceNotFoundException(theId);
		}

		return new MethodOutcome();
	}

	@Read()
	public MedicationStatement readMedicationStatement(@IdParam IdType theId) {
		MedicationStatement retval = (MedicationStatement) myMapper.toFHIR(theId);
		if (retval == null) {
			throw new ResourceNotFoundException(theId);
		}
			
		return retval;
	}

	@Search()
	public IBundleProvider findMedicationStatementsById(
			@RequiredParam(name = MedicationStatement.SP_RES_ID) TokenParam theMedicationStatementId
			) {
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper> ();

		if (theMedicationStatementId != null) {
			paramList.addAll(myMapper.mapParameter (MedicationStatement.SP_RES_ID, theMedicationStatementId, false));
		}

		MyBundleProvider myBundleProvider = new MyBundleProvider(paramList);
		myBundleProvider.setTotalSize(getTotalSize(paramList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);
		return myBundleProvider;
	}
	
	@Search()
	public IBundleProvider findMedicationStatementsByParams(
			@OptionalParam(name = MedicationStatement.SP_CODE) TokenOrListParam theOrCodes,
			@OptionalParam(name = MedicationStatement.SP_CONTEXT) ReferenceParam theContext,
			@OptionalParam(name = MedicationStatement.SP_EFFECTIVE) DateParam theDate,
			@OptionalParam(name = MedicationStatement.SP_PATIENT) ReferenceParam thePatient,
			@OptionalParam(name = MedicationStatement.SP_SUBJECT) ReferenceParam theSubject,
			@OptionalParam(name = MedicationStatement.SP_SOURCE) ReferenceParam theSource
			) {
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper> ();
		
		if (theOrCodes != null) {
			List<TokenParam> codes = theOrCodes.getValuesAsQueryTokens();

			if (codes.size() == 1) {
				TokenParam theCode = codes.get(0);
				if (theCode.getModifier() != null && theCode.getModifier().compareTo(TokenParamModifier.IN) == 0) {
					// We have modifier to search data in certain code value set.
					// With this modifier, the code is URI for value set.
					String valueSetValue = theCode.getValue();
					if (valueSetValue.split("?").length > 1) {
						errorProcessing("code:in="+valueSetValue+" is not supported. We only support simple value set URL");
					} 
				}
				paramList.addAll(myMapper.mapParameter (MedicationStatement.SP_CODE, theCode, false));

			} else {
				for (TokenParam code : codes) {
					paramList.addAll(myMapper.mapParameter(MedicationStatement.SP_CODE, code, true));
				}
			}
		}		
		if (theContext != null) {
			paramList.addAll(myMapper.mapParameter (MedicationStatement.SP_CONTEXT, theContext, false));
		}
		if (theDate != null) {
			paramList.addAll(myMapper.mapParameter (MedicationStatement.SP_EFFECTIVE, theDate, false));
		}
		if (theSubject != null) {
			if (theSubject.getResourceType().equals(PatientResourceProvider.getType())) {
				thePatient = theSubject;
			} else {
				errorProcessing("subject search allows Only Patient Resource.");
			}
		}
		if (thePatient != null) {
			paramList.addAll(myMapper.mapParameter (MedicationStatement.SP_PATIENT, thePatient, false));
		}
		if (theSource != null) {
			paramList.addAll(myMapper.mapParameter (MedicationStatement.SP_SOURCE, theSource, false));
		}

		MyBundleProvider myBundleProvider = new MyBundleProvider(paramList);
		myBundleProvider.setTotalSize(getTotalSize(paramList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);
		return myBundleProvider;
		
	}

	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return MedicationStatement.class;
	}

	private void errorProcessing(String msg) {
		OperationOutcome outcome = new OperationOutcome();
		CodeableConcept detailCode = new CodeableConcept();
		detailCode.setText(msg);
		outcome.addIssue().setSeverity(IssueSeverity.FATAL).setDetails(detailCode);
		throw new UnprocessableEntityException(FhirContext.forDstu3(), outcome);		
	}
	
	/**
	 * This method just provides simple business validation for resources we are storing.
	 * 
	 * @param theMedication
	 *            The medication statement to validate
	 */
	private void validateResource(MedicationStatement theMedication) {
		/*
		 * Our server will have a rule that patients must have a family name or we will reject them
		 */
//		if (thePatient.getNameFirstRep().getFamily().isEmpty()) {
//			OperationOutcome outcome = new OperationOutcome();
//			CodeableConcept detailCode = new CodeableConcept();
//			detailCode.setText("No family name provided, Patient resources must have at least one family name.");
//			outcome.addIssue().setSeverity(IssueSeverity.FATAL).setDetails(detailCode);
//			throw new UnprocessableEntityException(FhirContext.forDstu3(), outcome);
//		}
	}

	class MyBundleProvider extends OmopFhirBundleProvider implements IBundleProvider {

		public MyBundleProvider(List<ParameterWrapper> paramList) {
			super(paramList);
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
