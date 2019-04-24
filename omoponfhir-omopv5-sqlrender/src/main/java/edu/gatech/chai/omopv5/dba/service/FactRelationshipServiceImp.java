package edu.gatech.chai.omopv5.dba.service;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.gatech.chai.omopv5.model.entity.BaseEntity;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.FactRelationship;
import edu.gatech.chai.omopv5.model.entity.Note;
import edu.gatech.chai.omopv5.sqlrender.dao.FactRelationshipDao;

@Service
public class FactRelationshipServiceImp extends BaseEntityServiceImp<FactRelationship, FactRelationshipDao>
		implements FactRelationshipService {

	public FactRelationshipServiceImp() {
		super(FactRelationship.class);
	}

	@Transactional(readOnly = true)
	public <V extends BaseEntity> List<V> searchMeasurementUsingMethod(Long domainId) {
		List<V> retVal = new ArrayList<V>();

		EntityManager em = getEntityDao().getEntityManager();

		// Concept ID:
		// 21 = Measurement, 27 = Observation,
		// 44818800 = Using finding method
		// 58 = Type Concept, 26 = Note Type
		// 44818721 = Contains
		String query = "SELECT t FROM FactRelationship t WHERE domain_concept_id_1 = 21 AND relationship_concept_id = 44818800 AND fact_id_1 = :fact1";
		List<FactRelationship> results = em.createQuery(query, FactRelationship.class).setParameter("fact1", domainId)
				.getResultList();

		if (results.size() > 0) {
			for (FactRelationship result : results) {
				
				Long domainConcept2 = result.getDomainConcept2();
				Long fact2 = result.getFactId2();

				if (domainConcept2 == 58L) {
					Concept obsConcept = new Concept(fact2);
					retVal.add((V) obsConcept);
				} else if (domainConcept2 == 26L) {
					Note obsNote = new Note(fact2);
					retVal.add((V) obsNote);
				}
			}
		}

		return retVal;
	}

	@Transactional(readOnly = true)
	public List<Note> searchMeasurementContainsComments(Long domainId) {
		List<Note> retVal = new ArrayList<Note>();

		EntityManager em = getEntityDao().getEntityManager();

		// 44818721 = Contains
		String query = "SELECT t FROM FactRelationship t WHERE domain_concept_id_1 = 21 AND fact_id_1 = :fact1 AND domain_concept_id_2 = 26 AND relationship_concept_id = 44818721";
		List<FactRelationship> results = em.createQuery(query, FactRelationship.class).setParameter("fact1", domainId)
				.getResultList();
		if (results.size() > 0) {
			for (FactRelationship result : results) {
				Long fact2 = result.getFactId2();
				retVal.add(new Note(fact2));
			}
		}

		return retVal;
	}

	@Transactional(readOnly = true)
	public List<FactRelationship> searchFactRelationship(Long domainConcept1, Long factId1, Long domainConcept2,
			Long factId2, Long relationshipId) {

		EntityManager em = getEntityDao().getEntityManager();

		String query;
		List<FactRelationship> factRelationships;
		if (factId2 != null) {
			query = "SELECT t FROM FactRelationship t WHERE domain_concept_id_1 = :domain1 AND fact_id_1 = :fact1 AND domain_concept_id_2 = :domain2 AND fact_id_2 = :fact2 AND relationship_concept_id = :relationship";
			factRelationships = em.createQuery(query, FactRelationship.class)
					.setParameter("domain1", domainConcept1.intValue()).setParameter("domain2", domainConcept2.intValue())
					.setParameter("fact1", factId1.intValue()).setParameter("fact2", factId2.intValue())
					.setParameter("relationship", relationshipId.intValue()).getResultList();
		} else {
			query = "SELECT t FROM FactRelationship t WHERE domain_concept_id_1 = :domain1 AND fact_id_1 = :fact1 AND domain_concept_id_2 = :domain2 AND relationship_concept_id = :relationship";
			factRelationships = em.createQuery(query, FactRelationship.class)
					.setParameter("domain1", domainConcept1.intValue()).setParameter("domain2", domainConcept2.intValue())
					.setParameter("fact1", factId1.intValue())
					.setParameter("relationship", relationshipId.intValue()).getResultList();
		}

		return factRelationships;
	}

}
