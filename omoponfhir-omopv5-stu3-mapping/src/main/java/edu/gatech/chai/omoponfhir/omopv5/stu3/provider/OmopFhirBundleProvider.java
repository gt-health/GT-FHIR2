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

import java.util.Date;
import java.util.List;

import org.hl7.fhir.dstu3.model.InstantType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;

public abstract class OmopFhirBundleProvider implements IBundleProvider {
	InstantType searchTime;
	List<ParameterWrapper> paramList;
	Integer preferredPageSize;
	Integer totalSize;
	String orderParams = null;

	public OmopFhirBundleProvider (List<ParameterWrapper> paramList) {
		this.searchTime = InstantType.withCurrentTime();
		this.paramList = paramList;
	}
	
	public void setPreferredPageSize(Integer preferredPageSize) {
		this.preferredPageSize = preferredPageSize;
	}
	
	public void setTotalSize(Integer totalSize) {
		this.totalSize = totalSize;
	}

	@Override
	public IPrimitiveType<Date> getPublished() {
		return searchTime;
	}

	@Override
	public String getUuid() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer preferredPageSize() {
		return this.preferredPageSize;
	}

	@Override
	public Integer size() {
		return this.totalSize;
	}

	public String getOrderParams() {
		return this.orderParams;
	}
	
	public void setOrderParams(String orderParams) {
		this.orderParams = orderParams;
	}
	
}
