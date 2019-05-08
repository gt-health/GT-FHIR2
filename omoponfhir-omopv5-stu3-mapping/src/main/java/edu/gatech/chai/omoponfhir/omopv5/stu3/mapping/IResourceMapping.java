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

import java.util.List;
import java.util.Map;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.rest.api.SortSpec;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.model.entity.BaseEntity;

public interface IResourceMapping<v extends Resource, t extends BaseEntity> {
	public v toFHIR(IdType id);
	public Long toDbase(v fhirResource, IdType fhirId) throws FHIRException;
	public void removeDbase(Long id);
	public Long removeByFhirId (IdType fhirId) throws FHIRException;
	public Long getSize();
	public Long getSize(List<ParameterWrapper> mapList);

	public v constructResource(Long fhirId, t entity, List<String> includes);
	public void searchWithoutParams(int fromIndex, int toIndex, List<IBaseResource> listResources, List<String> includes, String sort);
	public void searchWithParams(int fromIndex, int toIndex, List<ParameterWrapper> map, List<IBaseResource> listResources, List<String> includes, String sort);

	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or);
	public v constructFHIR(Long fhirId, t entity);
	public t constructOmop(Long omopId, v fhirResource);
	
	public String constructOrderParams(SortSpec theSort);
}
