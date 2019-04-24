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
		property = "condition_occurrence_id")
public class ConditionOccurrence extends BaseEntity{
	public static String tableName = "condition_occurrence";

	@JsonProperty("condition_occurrence_id")
	private Long id;
	
	@JsonProperty("person_id")
	private FPerson fPerson;

	@JsonProperty("condition_concept_id")
	private Concept conceptId;
	
	@JsonProperty("condition_start_date")
	private Date startDate;

	@JsonProperty("condition_end_date")
	private Date endDate;

	@JsonProperty("condition_type_concept_id")
	private Concept typeConceptId;

	@JsonProperty("stop_reason")
	private String stopReason;

	@JsonProperty("provider_id")
	private Provider provider;

	@JsonProperty("visit_occurrence_id")
	private VisitOccurrence visitOccurrence;

	@JsonProperty("condition_source_value")
	private String conditionSourceValue;

	@JsonProperty("condition_source_concept_id")
	private Concept sourceConceptId;


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

	public Concept getConceptId() {
		return conceptId;
	}

	public void setConceptId(Concept conceptId) {
		this.conceptId = conceptId;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public Concept getTypeConceptId() {
		return typeConceptId;
	}

	public void setTypeConceptId(Concept typeConceptId) {
		this.typeConceptId = typeConceptId;
	}

	public String getStopReason() {
		return stopReason;
	}

	public void setStopReason(String stopReason) {
		this.stopReason = stopReason;
	}

	public Provider getProvider() {
		return provider;
	}

	public void setProvider(Provider provider) {
		this.provider = provider;
	}

	public VisitOccurrence getVisitOccurrence() {
		return visitOccurrence;
	}

	public void setVisitOccurrence(VisitOccurrence visitOccurrence) {
		this.visitOccurrence = visitOccurrence;
	}

	public String getConditionSourceValue() {
		return conditionSourceValue;
	}

	public void setConditionSourceValue(String conditionSourceValue) {
		this.conditionSourceValue = conditionSourceValue;
	}

	public Concept getSourceConceptId() {
		return sourceConceptId;
	}

	public void setSourceConceptId(Concept sourceConceptId) {
		this.sourceConceptId = sourceConceptId;
	}

	@Override
	public Long getIdAsLong() {
		return getId();
	}
}
