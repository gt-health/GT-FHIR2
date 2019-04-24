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
//import org.hl7.fhir.dstu3.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
//import ca.uhn.fhir.model.dstu2.composite.ContactPointDt;
//import ca.uhn.fhir.model.dstu2.valueset.ContactPointUseEnum;
//import ca.uhn.fhir.model.primitive.CodeDt;
//import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import edu.gatech.chai.omoponfhir.omopv5.stu3.mapping.OmopOrganization;
import edu.gatech.chai.omoponfhir.omopv5.stu3.model.MyOrganization;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;

/**
 * This is a simple resource provider which only implements "read/GET" methods,
 * but which uses a custom subclassed resource definition to add statically
 * bound extensions.
 * 
 * See the MyOrganization definition to see how the custom resource definition
 * works.
 */
public class OrganizationResourceProvider implements IResourceProvider {
	// private CareSiteService careSiteService;
	private WebApplicationContext myAppCtx;
	private String myDbType;
	private OmopOrganization myMapper;
	private int preferredPageSize = 30;

	public OrganizationResourceProvider() {
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
		myDbType = myAppCtx.getServletContext().getInitParameter("backendDbType");
		if (myDbType.equalsIgnoreCase("omopv5") == true) {
			myMapper = new OmopOrganization(myAppCtx);
		} else {
			myMapper = new OmopOrganization(myAppCtx);
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
		return "Organization";
	}
	
	public OmopOrganization getMyMapper() {
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
	public MethodOutcome createOrganization(@ResourceParam MyOrganization theOrganization) {
		// validateResource(thePatient);

		Long id=null;
		try {
			id = myMapper.toDbase(theOrganization, null);
		} catch (FHIRException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new MethodOutcome(new IdDt(id));
	}

	@Delete()
	public void deleteOrganization(@IdParam IdType theId) {
		if (myMapper.removeByFhirId(theId) <= 0) {
			throw new ResourceNotFoundException(theId);
		}
	}

	/**
	 * The getResourceType method comes from IResourceProvider, and must be
	 * overridden to indicate what type of resource this provider supplies.
	 */
	@Override
	public Class<MyOrganization> getResourceType() {
		return MyOrganization.class;
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
	public MyOrganization getResourceById(@IdParam IdType theId) {
		MyOrganization retVal = (MyOrganization) myMapper.toFHIR(theId);
		if (retVal == null) {
			throw new ResourceNotFoundException(theId);
		}

		return retVal;
	}

	@Search()
	public IBundleProvider findOrganizationByParams(
			@OptionalParam(name = MyOrganization.SP_RES_ID) TokenParam theOrganizationId,
			@OptionalParam(name = MyOrganization.SP_NAME) StringParam theName,
			
			@IncludeParam(allow={"Organization:partof"})
			final Set<Include> theIncludes
			) {
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper> ();

		if (theOrganizationId != null) {
			paramList.addAll(myMapper.mapParameter (MyOrganization.SP_RES_ID, theOrganizationId, false));
		}
		if (theName != null) {
			paramList.addAll(myMapper.mapParameter (MyOrganization.SP_NAME, theName, false));
		}
		
		MyBundleProvider myBundleProvider = new MyBundleProvider(paramList, theIncludes, null);
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
	public MyOrganization readOrganization(@IdParam IdType theId) {
		MyOrganization retval = (MyOrganization) myMapper.toFHIR(theId);
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
	public MethodOutcome updateOrganization(@IdParam IdType theId, @ResourceParam MyOrganization theOrganization) {
		validateResource(theOrganization);

		Long fhirId=null;
		try {
			fhirId = myMapper.toDbase(theOrganization, theId);
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
	private void validateResource(MyOrganization theOrganization) {
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
			if (theIncludes.contains(new Include("Organization:partof"))) {
				includes.add("Organization:partof");
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
