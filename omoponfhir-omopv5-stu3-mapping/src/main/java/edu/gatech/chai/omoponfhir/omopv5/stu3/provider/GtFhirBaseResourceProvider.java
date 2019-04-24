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

import org.hl7.fhir.dstu3.model.Resource;

import edu.gatech.chai.omoponfhir.omopv5.stu3.mapping.BaseOmopResource;
import edu.gatech.chai.omopv5.dba.service.IService;
import edu.gatech.chai.omopv5.model.entity.BaseEntity;

public class GtFhirBaseResourceProvider
	<v extends Resource, t extends BaseEntity, p extends IService<t>, x extends BaseOmopResource<v, t, p>> {
		private x myMapper;
		
		public x getMyMapper() {
			return myMapper;			
		}
}
