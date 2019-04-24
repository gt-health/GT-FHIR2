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
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(
		name="measurement",
		indexes = { 
				@Index(name = "idx_measurement_concept_id", columnList = "measurement_concept_id"), 
				@Index(name = "idx_measurement_fperson_id", columnList = "person_id")
				}
		)
public class Measurement extends BaseEntity {
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="measurement_occurrence_seq_gen")
	@SequenceGenerator(name="measurement_occurrence_seq_gen", sequenceName="measurement_occurrence_id_seq", allocationSize=1)
	@Column(name = "measurement_id")
	@Access(AccessType.PROPERTY)
	private Long id;

	@ManyToOne
	@JoinColumn(name="person_id", nullable=false)
	private FPerson fPerson;

	@Column(name="measurement_source_value")
	private String sourceValue; 

	@ManyToOne
	@JoinColumn(name="measurement_source_concept_id")
	private Concept sourceValueConcept; 

	@ManyToOne
	@JoinColumn(name="measurement_concept_id", nullable=false)
	private Concept measurementConcept;

	@Column(name = "value_as_number")
	private Double valueAsNumber;

	@ManyToOne
	@JoinColumn(name = "value_as_concept_id")
	private Concept valueAsConcept;

	@Column(name="value_source_value")
	private String valueSourceValue;
	
	@ManyToOne
	@JoinColumn(name = "operator_concept_id")
	private Concept operatorConcept;

	@ManyToOne
	@JoinColumn(name = "unit_concept_id")
	private Concept unitConcept;
	
	@Column(name="unit_source_value")
	private String unitSourceValue; 

	@Column(name = "range_low")
	private Double rangeLow;

	@Column(name = "range_high")
	private Double rangeHigh;

	@ManyToOne
	@JoinColumn(name="provider_id")
	private Provider provider;

	@Column(name = "measurement_date", nullable = false)
	@Temporal(TemporalType.DATE)
	private Date date;

	@Column(name = "measurement_time")
	// @Temporal(TemporalType.TIME)
	private String time;

	@ManyToOne
	@JoinColumn(name = "visit_occurrence_id")
	private VisitOccurrence visitOccurrence;

	@ManyToOne
	@JoinColumn(name = "measurement_type_concept_id")
	private Concept type;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public FPerson getFPerson() {
		return fPerson;
	}
	
	public void setFPerson(FPerson fPerson) {
		this.fPerson = fPerson;
	}
	
	public String getSourceValue() {
		return sourceValue;
	}
	
	public void setSourceValue(String sourceValue) {
		this.sourceValue = sourceValue;
	}

	public Concept getSourceValueConcept() {
		return sourceValueConcept;
	}
	
	public void setSourceValueConcept(Concept sourceValueConcept) {
		this.sourceValueConcept = sourceValueConcept;
	}

	public Concept getOperatorConcept() {
		return operatorConcept;
	}
	
	public void setOperationConcept(Concept operatorConcept) {
		this.operatorConcept = operatorConcept;
	}

	public Concept getMeasurementConcept() {
		return measurementConcept;
	}
	
	public void setMeasurementConcept(Concept measurementConcept) {
		this.measurementConcept = measurementConcept;
	}
	
	public Double getValueAsNumber() {
		return valueAsNumber;
	}
	
	public void setValueAsNumber(Double valueAsNumber) {
		this.valueAsNumber = valueAsNumber;
	}
	
	public Concept getValueAsConcept() {
		return valueAsConcept;
	}
	
	public void setValueAsConcept(Concept valueAsConcept) {
		this.valueAsConcept = valueAsConcept;
	}
	
	public String getValueSourceValue() {
		return valueSourceValue;
	}
	
	public void setValueSourceValue(String valueSourceValue) {
		this.valueSourceValue = valueSourceValue;
	}
	
	public Concept getUnitConcept() {
		return unitConcept;
	}
	
	public void setUnitConcept(Concept unitConcept) {
		this.unitConcept = unitConcept;
	}
	
	public String getUnitSourceValue() {
		return unitSourceValue;
	}
	
	public void setUnitSourceValue(String unitSourceValue) {
		this.unitSourceValue = unitSourceValue;
	}

	public Double getRangeLow() {
		return rangeLow;
	}
	
	public void setRangeLow(Double rangeLow) {
		this.rangeLow =rangeLow;
	}
	
	public Double getRangeHigh() {
		return rangeHigh;
	}
	
	public void setRangeHigh(Double rangeHigh) {
		this.rangeHigh = rangeHigh;
	}
	
	public Provider getProvider() {
		return provider;
	}
	
	public void setProvider(Provider provider) {
		this.provider = provider;
	}

	public Date getDate() {
		return date;
	}
	
	public void setDate(Date date) {
		this.date = date;
	}
	
	public String getTime() {
		return time;
	}
	
	public void setTime(String time) {
		this.time = time;
	}
	
	public VisitOccurrence getVisitOccurrence() {
		return visitOccurrence;
	}
	
	public void setVisitOccurrence (VisitOccurrence visitOccurrence) {
		this.visitOccurrence = visitOccurrence;
	}
	
	public Concept getType() {
		return type;
	}
	
	public void setType(Concept type) {
		this.type = type;
	}

	@Override
	public Long getIdAsLong() {
		return getId();
	}
}
