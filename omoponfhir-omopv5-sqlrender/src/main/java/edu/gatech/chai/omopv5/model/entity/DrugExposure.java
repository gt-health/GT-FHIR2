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

import java.util.Date;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(
		generator = ObjectIdGenerators.PropertyGenerator.class,
		property = "drug_exposure_id")
public class DrugExposure extends BaseEntity {
	public static String tableName = "drug_exposure";

	@JsonProperty("drug_exposure_id")
	private Long id;
	
	@JsonProperty("days_supply")
	private Integer daysSupply;
	
	@JsonProperty ("sig")
	private String sig;
	
	@JsonProperty("route_concept_id")
	private Concept routeConcept;
	
	@JsonProperty("effective_drug_dose")
	private Double effectiveDrugDose;
	
	@JsonProperty("dose_unit_concept_id")
	private Concept doseUnitConcept;
	
	@JsonProperty("lot_number")
	private String lotNumber;
	
	@JsonProperty("provider_id")
	private Provider provider;
	
	@JsonProperty("visit_occurrence_id")
	private VisitOccurrence visitOccurrence;

	@JsonProperty("drug_source_value")
	private String drugSourceValue;
	
	@JsonProperty("drug_source_concept_id")
	private Concept drugSourceConcept;
	
	@JsonProperty("person_id")
	private FPerson fPerson;
	
	@JsonProperty("route_source_value")
	private String routeSourceValue;
	
	@JsonProperty("dose_unit_source_value")
	private String doseUnitSourceValue;
	
	@JsonProperty("drug_concept_id")
	private Concept drugConcept;
	
	@JsonProperty("drug_exposure_start_date")
	private Date drugExposureStartDate;
	
	@JsonProperty("drug_exposure_end_date")
	private Date drugExposureEndDate;
	
	@JsonProperty("drug_type_concept_id")
	private Concept drugTypeConcept;
	
	@JsonProperty("stop_reason")
	private String stopReason;

	@JsonProperty("refills")
	private Integer refills;

	@JsonProperty("quantity")
	private Double quantity;

	
	public DrugExposure() {
	}

	public DrugExposure(Long id) {
		this.id = id;
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getDaysSupply() {
		return this.daysSupply;
	}
	
	public void setDaysSupply(Integer daysSupply) {
		this.daysSupply = daysSupply;
	}
	
	public String getSig() {
		return this.sig;
	}
	
	public void setSig(String sig) {
		this.sig = sig;
	}
	
	public Concept getRouteConcept() {
		return this.routeConcept;
	}
	
	public void setRouteConcept(Concept routeConcept) {
		this.routeConcept = routeConcept;
	}
	
	public Double getEffectiveDrugDose() {
		return effectiveDrugDose;
	}
	
	public void setEffectiveDrugDose(Double effectiveDrugDose) {
		this.effectiveDrugDose = effectiveDrugDose;
	}
	
	public Concept getDoseUnitConcept() {
		return doseUnitConcept;
	}
	
	public void setDoseUnitConcept(Concept doseUnitConcept) {
		this.doseUnitConcept = doseUnitConcept;
	}
	
	public String getLotNumber() {
		return this.lotNumber;
	}
	
	public void setLotNumber(String lotNumber) {
		this.lotNumber = lotNumber;
	}
	
	public Provider getProvider() {
		return this.provider;
	}
	
	public void setProvider(Provider provider) {
		this.provider = provider;
	}
	
	public VisitOccurrence getVisitOccurrence() {
		return this.visitOccurrence;
	}
	
	public void setVisitOccurrence(VisitOccurrence visitOccurrence) {
		this.visitOccurrence = visitOccurrence;
	}
	
	public String getDrugSourceValue() {
		return drugSourceValue;
	}
	
	public void setDrugSourceValue(String drugSourceValue) {
		this.drugSourceValue = drugSourceValue;
	}
	
	public Concept getDrugSourceConcept() {
		return this.drugSourceConcept;
	}
	
	public void setDrugSourceConcpet (Concept drugSourceConcept) {
		this.drugSourceConcept = drugSourceConcept;
	}

	public FPerson getFPerson() {
		return this.fPerson;
	}
	
	public void setFPerson(FPerson fPerson) {
		this.fPerson = fPerson;
	}
	
	public String getRouteSourceValue(){
		return this.routeSourceValue;
	}
	
	public void setRouteSourceValue(String routeSourceValue) {
		this.routeSourceValue = routeSourceValue;
	}
	
	public String getDoseUnitSourceValue() {
		return doseUnitSourceValue;
	}
	
	public void setDoseUnitSourceValue(String doseUnitSourceValue) {
		this.doseUnitSourceValue = doseUnitSourceValue;
	}
	
	public Concept getDrugConcept() {
		return this.drugConcept;
	}
	
	public void setDrugConcept(Concept drugConcept) {
		this.drugConcept = drugConcept;
	}
	
	public Date getDrugExposureStartDate() {
		return this.drugExposureStartDate;
	}

	public void setDrugExposureStartDate(Date drugExposureStartDate) {
		this.drugExposureStartDate = drugExposureStartDate;
	}

	public Date getDrugExposureEndDate() {
		return this.drugExposureEndDate;
	}

	public void setDrugExposureEndDate(Date drugExposureEndDate) {
		this.drugExposureEndDate = drugExposureEndDate;
	}
	
	public Concept getDrugTypeConcept() {
		return this.drugTypeConcept;
	}

	public void setDrugTypeConcept(Concept drugTypeConcept) {
		this.drugTypeConcept = drugTypeConcept;
	}
	
	public String getStopReason() {
		return this.stopReason;
	}
	
	public void setStopReason(String stopReason) {
		this.stopReason = stopReason;
	}

	public Integer getRefills() {
		return this.refills;
	}
	public void setRefills(Integer refills) {
		this.refills = refills;
	}

	public Double getQuantity() {
		return this.quantity;
	}

	public void setQuantity(Double quantity) {
		this.quantity = quantity;
	}

	@Override
	public Long getIdAsLong() {
		return getId();
	}
}
