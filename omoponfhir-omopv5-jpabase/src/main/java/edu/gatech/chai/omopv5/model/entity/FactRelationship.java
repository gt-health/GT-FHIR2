package edu.gatech.chai.omopv5.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="fact_relationship")
public class FactRelationship extends BaseEntity {

	@Id
	@Column(name="domain_concept_id_1")
	private Long domainConcept1;

	@Column(name="fact_id_1")
	private Long factId1;

	@Column(name="domain_concept_id_2")
	private Long domainConcept2;

	@Column(name="fact_id_2")
	private Long factId2;
	
	@ManyToOne
	@JoinColumn(name="relationship_concept_id")
	private Concept relationshipConcept;
	
	public Long getDomainConcept1() {
		return this.domainConcept1;
	}
	
	public void setDomainConcept1(Long domainConcept1) {
		this.domainConcept1 = domainConcept1;
	}
	
	public Long getFactId1() {
		return this.factId1;
	}
	
	public void setFactId1(Long factId1) {
		this.factId1 = factId1;
	}
	
	public Long getDomainConcept2() {
		return this.domainConcept2;
	}
	
	public void setDomainConcept2(Long domainConcept2) {
		this.domainConcept2 = domainConcept2;
	}
	
	public Long getFactId2() {
		return this.factId2;
	}
	
	public void setFactId2(Long factId2) {
		this.factId2 = factId2;
	}
	
	public Concept getRelationshipConcept() {
		return this.relationshipConcept;
	}
	
	public void setRelationshipConcept(Concept relationshipConcept) {
		this.relationshipConcept = relationshipConcept;
	}
	
	@Override
	public Long getIdAsLong() {
		return this.domainConcept1;
	}
}
