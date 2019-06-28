package edu.gatech.chai.omopv5.dba.service;

import org.springframework.stereotype.Service;

import edu.gatech.chai.omopv5.jpa.dao.ConceptRelationshipDao;
import edu.gatech.chai.omopv5.model.entity.ConceptRelationship;

@Service
public class ConceptRelationshipServiceImp extends BaseEntityServiceImp<ConceptRelationship, ConceptRelationshipDao>
		implements ConceptRelationshipService {

	public ConceptRelationshipServiceImp() {
		super(ConceptRelationship.class);
	}

}
