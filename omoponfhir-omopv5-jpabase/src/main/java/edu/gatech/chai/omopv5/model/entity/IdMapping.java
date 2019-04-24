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
 *
 *******************************************************************************/
package edu.gatech.chai.omopv5.model.entity;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="id_mapping")
public class IdMapping {

	@Id
	@Column(name="id")
	@Access(AccessType.PROPERTY)
	private Long id;

	@Column(name="fhir_id")
	private Long fhirId;
	
	@Column(name="omop_id")
	private Long omopId;
	
	@Column(name="related_resource")
	private String relatedResource;
	
	public IdMapping() {}
	
	public Long getId() {
		return this.id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public Long getFhirId() {
		return this.fhirId;
	}
	
	public void setFhirId(Long fhirId) {
		this.fhirId = fhirId;
	}
	
	public Long getOmopId() {
		return this.omopId;
	}
	
	public void setOmopId(Long omopId) {
		this.omopId = omopId;
	}
	
	public String getRelatedResource() {
		return this.relatedResource;
	}
	
	public void setRelatedResource(String relatedResource) {
		this.relatedResource = relatedResource;
	}
}
