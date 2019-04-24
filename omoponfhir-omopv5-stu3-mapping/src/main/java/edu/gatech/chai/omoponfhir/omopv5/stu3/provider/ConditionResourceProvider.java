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

import edu.gatech.chai.omoponfhir.omopv5.stu3.mapping.OmopCondition;
import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.ThrowFHIRExceptions;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;

import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
//import ca.uhn.fhir.model.dstu2.composite.ContactPointDt;
//import ca.uhn.fhir.model.dstu2.valueset.ContactPointUseEnum;
//import ca.uhn.fhir.model.primitive.CodeDt;
//import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

/**
 * This is a simple resource provider which only implements "read/GET" methods,
 * but which uses a custom subclassed resource definition to add statically
 * bound extensions.
 *
 * See the MyOrganization definition to see how the custom resource definition
 * works.
 */
public class ConditionResourceProvider implements IResourceProvider {
	// private CareSiteService careSiteService;
	private WebApplicationContext myAppCtx;
	private String myDbType;
	private OmopCondition myMapper;
	private int preferredPageSize = 30;

	public ConditionResourceProvider() {
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
		myDbType = myAppCtx.getServletContext().getInitParameter("backendDbType");
		if (myDbType.equalsIgnoreCase("omopv5") == true) {
			myMapper = new OmopCondition(myAppCtx);
		} else {
			myMapper = new OmopCondition(myAppCtx);
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
		return "Condition";
	}

	public OmopCondition getMyMapper() {
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
	public MethodOutcome createCondition(@ResourceParam Condition condition) {

		Long id = null;
		try {
			id = myMapper.toDbase(condition, null);
		} catch (FHIRException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new MethodOutcome(new IdDt(id));
	}

	/**
	 * The getResourceType method comes from IResourceProvider, and must be
	 * overridden to indicate what type of resource this provider supplies.
	 */
	@Override
	public Class<Condition> getResourceType() {
		return Condition.class;
	}

	@Delete()
	public void deleteCondition(@IdParam IdType theId) {
		if (myMapper.removeByFhirId(theId) <= 0) {
			throw new ResourceNotFoundException(theId);
		}
	}

	/**
	 * The "@Read" annotation indicates that this method supports the read
	 * operation. It takes one argument, the Resource type being returned.
	 *
	 * @param theId
	 *            The read operation takes one parameter, which must be of type
	 *            IdDt and must be annotated with the "@Read.IdParam"
	 *            annotation.
	 * @return Returns a resource matching this identifier, or null if none
	 *         exists.
	 */
	@Read()
	public Condition getResourceById(@IdParam IdType theId) {
		Condition retVal = (Condition) myMapper.toFHIR(theId);
		if (retVal == null) {
			throw new ResourceNotFoundException(theId);
		}

		return retVal;
	}

	@Search()
	public IBundleProvider findConditionById(
			@RequiredParam(name = Condition.SP_RES_ID) TokenParam theConditionId
			) {
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper>();

		if (theConditionId != null) {
			paramList.addAll(myMapper.mapParameter(Condition.SP_RES_ID, theConditionId, false));
		}
		
		MyBundleProvider myBundleProvider = new MyBundleProvider(paramList);
		myBundleProvider.setTotalSize(getTotalSize(paramList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);

		return myBundleProvider;
	}

	@Search()
	public IBundleProvider findConditionByParams(
			@OptionalParam(name = Condition.SP_CODE) TokenOrListParam theOrCodes,
			@OptionalParam(name = Condition.SP_SUBJECT) ReferenceParam theSubjectId,
			@OptionalParam(name = Condition.SP_PATIENT) ReferenceParam thePatientId) {
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper>();

		if (theOrCodes != null) {
			List<TokenParam> codes = theOrCodes.getValuesAsQueryTokens();
			boolean orValue = true;
			if (codes.size() <= 1)
				orValue = false;
			for (TokenParam code : codes) {
				paramList.addAll(myMapper.mapParameter(Condition.SP_CODE, code, orValue));
			}
		}

		if (theSubjectId != null) {
			if (theSubjectId.getResourceType().equals(PatientResourceProvider.getType())) {
				thePatientId = theSubjectId;
			} else {
				ThrowFHIRExceptions.unprocessableEntityException("We only support Patient resource for subject");
			}
		}
		if (thePatientId != null) {
			paramList.addAll(myMapper.mapParameter(Condition.SP_PATIENT, thePatientId, false));
		}

		MyBundleProvider myBundleProvider = new MyBundleProvider(paramList);
		myBundleProvider.setTotalSize(getTotalSize(paramList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);

		return myBundleProvider;
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
	public Condition readCondition(@IdParam IdType theId) {
		Condition retval = (Condition) myMapper.toFHIR(theId);
		if (retval == null) {
			throw new ResourceNotFoundException(theId);
		}

		return retval;
	}

	/**
	 * The "@Update" annotation indicates that this method supports replacing an
	 * existing resource (by ID) with a new instance of that resource.
	 *
	 * @param theId
	 *            This is the ID of the patient to update
	 * @param theCondition
	 *            This is the actual resource to save
	 * @return This method returns a "MethodOutcome"
	 */
	@Update()
	public MethodOutcome updateCondition(@IdParam IdType theId, @ResourceParam Condition theCondition) {
		validateResource(theCondition);

		Long fhirId = null;
		try {
			fhirId = myMapper.toDbase(theCondition, theId);
		} catch (FHIRException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (fhirId == null) {
			throw new ResourceNotFoundException(theId);
		}

		return new MethodOutcome();
	}

	// TODO: Add more validation code here.
	private void validateResource(Condition theCondition) {
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
