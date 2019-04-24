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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(
		generator = ObjectIdGenerators.PropertyGenerator.class,
		property = "provider_id")
public class Provider extends BaseEntity {
	public static String tableName = "provider";

	@JsonProperty("provider_id")
	private Long id;
	
	@JsonProperty("provider_name")
	private String providerName;

	@JsonProperty("npi")
	private String npi;
	
	@JsonProperty("dea")
	private String dea;
	
	@JsonProperty("specialty_concept_id")
	private Concept specialtyConcept;
	
	@JsonProperty("care_site_id")
	private CareSite careSite;
	
	@JsonProperty("year_of_birth")
	private Integer yearOfBirth;
	
	@JsonProperty("gender_concept_id")
	private Concept genderConcept;

	@JsonProperty("provider_source_value")
	private String providerSourceValue;
	
	@JsonProperty("specialty_source_value")
	private String specialtySourceValue;
	
	@JsonProperty("specialty_source_concept_id")
	private Concept specialtySourceConcept;

	@JsonProperty("gender_source_value")
	private String genderSourceValue;
	
	@JsonProperty("gender_source_concept_id")
	private Concept genderSourceConcept;

	public Provider() {
		super();
	}
	
	public Provider(Long id, String providerName, String npi, String dea, Concept specialtyConcept, 
			CareSite careSite, Integer yearOfBirth, Concept genderConcept, String providerSourceValue, 
			String specialtySourceValue, Concept specialtySourceConcept, String genderSourceValue,
			Concept genderSourceConcept) {
		this.id = id;
		this.providerName = providerName;
		this.npi = npi;
		this.dea = dea;
		this.specialtyConcept = specialtyConcept;
		this.careSite = careSite;
		this.yearOfBirth = yearOfBirth;
		this.genderConcept = genderConcept;
		this.providerSourceValue = providerSourceValue;
		this.specialtySourceValue = specialtySourceValue;
		this.specialtySourceConcept = specialtySourceConcept;
		this.genderSourceValue = genderSourceValue;
		this.genderSourceConcept = genderSourceConcept;
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}

	public String getProviderName() {
		return providerName;
	}
	
	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}
	
	public String getNpi() {
		return npi;
	}
	
	public void setNpi(String npi) {
		this.npi = npi;
	}
	
	public String getDea() {
		return dea;
	}
	
	public void setDea(String dea) {
		this.dea = dea;
	}
	
	public Concept getSpecialtyConcept() {
		return specialtyConcept;
	}
	
	public void setSpecialtyConcept(Concept specialtyConcept) {
		this.specialtyConcept = specialtyConcept;
	}
	
	public CareSite getCareSite() {
		return careSite;
	}
	
	public void setCareSite(CareSite careSite) {
		this.careSite = careSite;
	}
	
	public Integer getYearOfBirth() {
		return yearOfBirth;
	}
	
	public void setYearOfBirth(Integer yearOfBirth) {
		this.yearOfBirth = yearOfBirth;
	}
	
	public Concept getGenderConcept() {
		return genderConcept;
	}

	public void setGenderConcept(Concept genderConcept) {
		this.genderConcept = genderConcept;
	}
	
	public String getProviderSourceValue() {
		return providerSourceValue;
	}
	
	public void setProviderSourceValue(String providerSourceValue) {
		this.providerSourceValue = providerSourceValue;
	}

	public Concept getSpecialtySourceConcept() {
		return specialtySourceConcept;
	}

	public void setSpecialtySourceConcept(Concept specialtySourceConcept) {
		this.specialtySourceConcept = specialtySourceConcept;
	}

	public String getSpecialtySourceValue() {
		return specialtySourceValue;
	}
	
	public void setSpecialtySourceValue(String specialtySourceValue) {
		this.specialtySourceValue = specialtySourceValue;
	}
	
	public String getGenderSourceValue() {
		return genderSourceValue;
	}

	public void setGenderSourceValue(String genderSourceValue) {
		this.genderSourceValue = genderSourceValue;
	}

	public Concept getGenderSourceConcept() {
		return genderSourceConcept;
	}

	public void setGenderSourceConcept(Concept genderSourceConcept) {
		this.genderSourceConcept = genderSourceConcept;
	}

	@Override
	public Long getIdAsLong() {
		return getId();
	}
	
}
