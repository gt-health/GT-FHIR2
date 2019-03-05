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
package edu.gatech.chai.omopv5.jpa.entity;

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

@Entity
@Table(name="drug_exposure")
public class DrugExposure extends BaseEntity {
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="drug_exposure_seq_gen")
	@SequenceGenerator(name="drug_exposure_seq_gen", sequenceName="drug_exposure_id_seq", allocationSize=1)
	@Column(name="drug_exposure_id", updatable= false)
	@Access(AccessType.PROPERTY)
	private Long id;
	
	@Column(name = "days_supply")
	private Integer daysSupply;
	
	@Column (name = "sig")
	private String sig;
	
	@ManyToOne()
	@JoinColumn(name = "route_concept_id")
	private Concept routeConcept;
	
	@Column(name = "effective_drug_dose")
	private Double effectiveDrugDose;
	
	@ManyToOne()
	@JoinColumn(name = "dose_unit_concept_id")
	private Concept doseUnitConcept;
	
	@Column(name = "lot_number")
	private String lotNumber;
	
	@ManyToOne
	@JoinColumn(name = "provider_id")
	private Provider provider;
	
	@ManyToOne
	@JoinColumn(name = "visit_occurrence_id")
	private VisitOccurrence visitOccurrence;

	@Column(name = "drug_source_value")
	private String drugSourceValue;
	
	@ManyToOne
	@JoinColumn(name = "drug_source_concept_id")
	private Concept drugSourceConcept;
	
	@ManyToOne
	@JoinColumn(name = "person_id", nullable = false)
	private FPerson fPerson;
	
	@Column(name = "route_source_value")
	private String routeSourceValue;
	
	@Column(name = "dose_unit_source_value")
	private String doseUnitSourceValue;
	
	@ManyToOne
	@JoinColumn(name = "drug_concept_id", nullable = false)
	private Concept drugConcept;
	
	@Column(name ="drug_exposure_start_date", nullable = false)
	private Date drugExposureStartDate;
	
	@Column(name ="drug_exposure_end_date")
	private Date drugExposureEndDate;
	
	@ManyToOne
	@JoinColumn(name = "drug_type_concept_id", nullable = false)
	private Concept drugTypeConcept;
	
	@Column(name = "stop_reason")
	private String stopReason;

	@Column(name = "refills")
	private Integer refills;

	@Column(name = "quantity")
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
