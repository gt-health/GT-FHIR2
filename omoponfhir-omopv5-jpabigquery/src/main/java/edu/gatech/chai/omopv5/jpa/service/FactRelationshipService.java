package edu.gatech.chai.omopv5.jpa.service;

import java.util.List;

import edu.gatech.chai.omopv5.jpa.entity.BaseEntity;
import edu.gatech.chai.omopv5.jpa.entity.FactRelationship;
import edu.gatech.chai.omopv5.jpa.entity.Measurement;
import edu.gatech.chai.omopv5.jpa.entity.Note;

public interface FactRelationshipService extends IService<FactRelationship> {
	public <V extends BaseEntity> List<V> searchMeasurementUsingMethod(Long conceptId);

	public List<Note> searchMeasurementContainsComments(Long conceptId);

	public List<FactRelationship> searchFactRelationship(Long domainConcept1, Long factId1, Long domainConcept2,
			Long factId2, Long relationshipId);
}
