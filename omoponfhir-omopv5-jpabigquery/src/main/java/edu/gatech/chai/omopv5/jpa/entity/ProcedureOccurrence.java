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
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(
		name="procedure_occurrence",
		indexes = { 
				@Index(name = "idx_procedure_concept_id", columnList = "procedure_concept_id"), 
				@Index(name = "idx_procedure_fperson_id", columnList = "person_id")
				}
		)
public class ProcedureOccurrence extends BaseEntity {

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="procedure_occurrence_seq_gen")
	@SequenceGenerator(name="procedure_occurrence_seq_gen", sequenceName="procedure_occurrence_id_seq", allocationSize=1)
	@Column(name = "procedure_occurrence_id")
	@Access(AccessType.PROPERTY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "person_id", nullable = false)
	private FPerson fPerson;

	@ManyToOne
	@JoinColumn(name = "procedure_concept_id")
	private Concept procedureConcept;

	@Column(name = "procedure_date", nullable = false)
//	@Temporal(TemporalType.DATE)
	@Temporal(TemporalType.TIMESTAMP)
	private Date procedureDate;

	@ManyToOne
	@JoinColumn(name = "procedure_type_concept_id")
	private Concept procedureTypeConcept;

	@ManyToOne
	@JoinColumn(name = "modifier_concept_id")
	private Concept modifierConcept;
	
	@Column(name="quantity")
	private Long quantity;
	
	@ManyToOne
	@JoinColumn(name = "provider_id")
	private Provider provider;

	@ManyToOne
	@JoinColumn(name = "visit_occurrence_id")
	private VisitOccurrence visitOccurrence;

	@Column(name = "procedure_source_value")
	private String procedureSourceValue;

	@ManyToOne
	@JoinColumn(name = "procedure_source_concept_id")
	private Concept procedureSourceConcept;

	@Column(name = "qualifier_source_value")
	private String qualifierSourceValue;

	public ProcedureOccurrence() {
		super();
	}

	public ProcedureOccurrence(Long id) {
		super();
		this.id = id;
	}
	
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

	public Concept getProcedureConcept() {
		return procedureConcept;
	}

	public void setProcedureConcept(Concept procedureConcept) {
		this.procedureConcept = procedureConcept;
	}

	public Date getProcedureDate() {
		return procedureDate;
	}

	public void setProcedureDate(Date procedureDate) {
		this.procedureDate = procedureDate;
	}

	public Concept getProcedureTypeConcept() {
		return procedureTypeConcept;
	}

	public void setProcedureTypeConcept(Concept procedureTypeConcept) {
		this.procedureTypeConcept = procedureTypeConcept;
	}

	public Concept getModifierConcept() {
		return modifierConcept;
	}
	
	public void setModifierConcept(Concept modifierConcept) {
		this.modifierConcept = modifierConcept;
	}
	
	public Long getQuantity() {
		return quantity;
	}
	
	public void setQuantity(Long quantity) {
		this.quantity = quantity;
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

	public String getProcedureSourceValue() {
		return procedureSourceValue;
	}

	public void setProcedureSourceValue(String procedureSourceValue) {
		this.procedureSourceValue = procedureSourceValue;
	}

	public Concept getProcedureSourceConcept() {
		return procedureSourceConcept;
	}

	public void setProcedureSourceConcept(Concept procedureSourceConcept) {
		this.procedureSourceConcept = procedureSourceConcept;
	}

	public String getQualifierSourceValue() {
		return qualifierSourceValue;
	}
	
	public void setQualifierSourceValue(String qualifierSourceValue) {
		this.qualifierSourceValue = qualifierSourceValue;
	}

	@Override
	public Long getIdAsLong() {
		return getId();
	}

}
