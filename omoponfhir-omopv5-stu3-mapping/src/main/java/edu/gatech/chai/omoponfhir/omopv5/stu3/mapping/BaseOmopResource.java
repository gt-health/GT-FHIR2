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

import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.api.SortSpec;
import edu.gatech.chai.omoponfhir.local.dao.FhirOmopCodeMapImpl;
import edu.gatech.chai.omoponfhir.local.dao.FhirOmopVocabularyMapImpl;
import edu.gatech.chai.omopv5.dba.service.IService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.model.entity.BaseEntity;

public abstract class BaseOmopResource<v extends Resource, t extends BaseEntity, p extends IService<t>>
		implements IResourceMapping<v, t> {

	protected FhirOmopVocabularyMapImpl fhirOmopVocabularyMap;
	protected FhirOmopCodeMapImpl fhirOmopCodeMap;
	
	private p myOmopService;
	private Class<t> myEntityClass;
	private Class<p> myServiceClass;
	private String myFhirResourceType;

	public static String MAP_EXCEPTION_FILTER = "FILTER";
	public static String MAP_EXCEPTION_EXCLUDE = "EXCLUDE";

	public BaseOmopResource(WebApplicationContext context, Class<t> entityClass, Class<p> serviceClass,
			String fhirResourceType) {
		myOmopService = context.getBean(serviceClass);
		myEntityClass = entityClass;
		myFhirResourceType = fhirResourceType;
		fhirOmopVocabularyMap = new FhirOmopVocabularyMapImpl();
		fhirOmopCodeMap = new FhirOmopCodeMapImpl();
	}

	public String getMyFhirResourceType() {
		return this.myFhirResourceType;
	}

	public p getMyOmopService() {
		return this.myOmopService;
	}

	public void setMyOmopService(WebApplicationContext context) {
		this.myOmopService = context.getBean(myServiceClass);
	}

	public Class<t> getMyEntityClass() {
		return this.myEntityClass;
	}

	public void removeDbase(Long id) {
		myOmopService.removeById(id);
	}

	public Long removeByFhirId(IdType fhirId) {
		Long id_long_part = fhirId.getIdPartAsLong();
		Long myId = IdMapping.getOMOPfromFHIR(id_long_part, getMyFhirResourceType());

		return myOmopService.removeById(myId);
	}

	public Long getSize() {
		return myOmopService.getSize();
	}

	public Long getSize(List<ParameterWrapper> mapList) {
		return myOmopService.getSize(mapList);
	}

	/***
	 * constructResource: Overwrite this if you want to implement includes.
	 */
	public v constructResource(Long fhirId, t entity, List<String> includes) {
		v fhirResource = constructFHIR(fhirId, entity);

		return fhirResource;
	}

	// This needs to be overridden at every Omop[x] class.
//	public v constructFHIR(Long fhirId, t entity) {
//		return null;
//	}

	/***
	 * toFHIR this is called from FHIR provider for read operation.
	 */
	public v toFHIR(IdType id) {
		Long id_long_part = id.getIdPartAsLong();
		Long myId = IdMapping.getOMOPfromFHIR(id_long_part, getMyFhirResourceType());

		t entityClass = (t) getMyOmopService().findById(myId);
		if (entityClass == null)
			return null;

		Long fhirId = IdMapping.getFHIRfromOMOP(myId, getMyFhirResourceType());

		return constructFHIR(fhirId, entityClass);
	}

	public void searchWithoutParams(int fromIndex, int toIndex, List<IBaseResource> listResources,
			List<String> includes, String sort) {
		List<t> entities = getMyOmopService().searchWithoutParams(fromIndex, toIndex, sort);

		// We got the results back from OMOP database. Now, we need to construct
		// the list of
		// FHIR Patient resources to be included in the bundle.
		for (t entity : entities) {
			Long omopId = entity.getIdAsLong();
			Long fhirId = IdMapping.getFHIRfromOMOP(omopId, getMyFhirResourceType());
			v fhirResource = constructResource(fhirId, entity, includes);
			if (fhirResource != null) {
				listResources.add(fhirResource);
				addRevIncludes(omopId, includes, listResources);
			}
		}
	}

	public void searchWithParams(int fromIndex, int toIndex, List<ParameterWrapper> mapList,
			List<IBaseResource> listResources, List<String> includes, String sort) {
		List<t> entities = getMyOmopService().searchWithParams(fromIndex, toIndex, mapList, sort);

		for (t entity : entities) {
			Long omopId = entity.getIdAsLong();
			Long fhirId = IdMapping.getFHIRfromOMOP(omopId, getMyFhirResourceType());
			v fhirResource = constructResource(fhirId, entity, includes);
			if (fhirResource != null) {
				listResources.add(fhirResource);
				// Do the rev_include and add the resource to the list.
				addRevIncludes(omopId, includes, listResources);
			}
		}
	}

	// Override the this method to provide rev_includes.
	public void addRevIncludes(Long omopId, List<String> includes, List<IBaseResource> listResources) {

	}

	// Some common functions that are repetitively used.
	protected void addParamlistForPatientIDName(String parameter, String value, ParameterWrapper paramWrapper,
			List<ParameterWrapper> mapList) {
		switch (parameter) {
		case "Patient:" + Patient.SP_RES_ID:
			String pId = value;
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("fPerson.id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(pId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case "Patient:" + Patient.SP_NAME:
			String patientName = value.replace("\"", "");
			patientName = patientName.replace("'", "");
			
			paramWrapper.setParameterType("String");
			paramWrapper.setParameters(Arrays.asList("fPerson.familyName", "fPerson.givenName1", "fPerson.givenName2",
					"fPerson.prefixName", "fPerson.suffixName"));
			paramWrapper.setOperators(Arrays.asList("like", "like", "like", "like", "like"));
			paramWrapper.setValues(Arrays.asList("%" + patientName + "%"));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case "Patient:" + Patient.SP_IDENTIFIER:
			String identifier = value.replace("\"", "");
			identifier = identifier.replace("'", "");
			
			paramWrapper.setParameterType("String");
			paramWrapper.setParameters(Arrays.asList("fPerson.personSourceValue"));
			paramWrapper.setOperators(Arrays.asList("like"));
			paramWrapper.setValues(Arrays.asList("%" + identifier + "%"));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		default:
			return;
		}
	}
	
	public String constructOrderParams(SortSpec theSort) {
		String direction;
		
		if (theSort.getOrder() != null) direction = theSort.getOrder().toString();
		else direction = "ASC";

		String orderParam = "id " + direction;
		
		return orderParam;
	}
}
