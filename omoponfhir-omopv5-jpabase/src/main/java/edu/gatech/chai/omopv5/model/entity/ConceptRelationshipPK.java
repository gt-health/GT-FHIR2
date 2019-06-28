package edu.gatech.chai.omopv5.model.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class ConceptRelationshipPK implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Column(name = "concept_id_1")
    private Long concept1;

    @Column(name = "concept_id_2")
    private Long concept2;
    
    @Column(name="relationship_id")
	private String relationshipId;
    
    public ConceptRelationshipPK() {
    }
    
	public Long getConcept1() {
		return this.concept1;
	}
	
	public void setConcept1(Long concept1) {
		this.concept1 = concept1;
	}

	public Long getConcept2() {
		return this.concept2;
	}
	
	public void setConcept2(Long concept2) {
		this.concept2 = concept2;
	}
	
	public String getRelationshipId() {
		return relationshipId;
	}
	
	public void setRelationshipId(String relationshipId) {
		this.relationshipId = relationshipId;
	}
	
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((relationshipId == null) ? 0 : relationshipId.hashCode());
		result = prime * result + concept1.intValue() + concept2.intValue();
		return result;
	}
    
    @Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConceptRelationshipPK other = (ConceptRelationshipPK) obj;
		if (relationshipId == null) {
			if (other.relationshipId != null)
				return false;
		} else if (!relationshipId.equals(other.relationshipId))
			return false;
		if (concept1 != other.concept1)
			return false;
		if (concept2 != other.concept2)
			return false;
		
		return true;
	}
}
