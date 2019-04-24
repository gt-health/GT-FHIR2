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
package edu.gatech.chai.omopv5.dba.service;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.sqlrender.dao.ConceptDao;

@Service
public class ConceptServiceImp extends BaseEntityServiceImp<Concept, ConceptDao> implements ConceptService {
	public ConceptServiceImp() {
		super(Concept.class);
	}
	
	@Transactional(readOnly = true)
	public List<Concept> getIngredient(Concept concept) {
		EntityManager em = getEntityDao().getEntityManager();
		
		List<Concept> concepts = new ArrayList<Concept>();
		
		if ("Ingredient".equals(concept.getConceptClass())) {
			// This is ingredient. Just return null
			return concepts;
		}
		
		String sqlQuery = null;
		if ("NDC".equals(concept.getVocabulary().getId())) {
			// Use JPQL
			sqlQuery = "select c "
					+ "FROM Concept src "
					+ "JOIN ConceptRelationship cr on src.id = cr.conceptId1 "
			        + "AND cr.relationship_id = 'Maps to' "
			        + "AND cr.invalid_reason is null "
			        + "JOIN Concept tar on cr.conceptId2 = tar.id "
			        + "AND tar.standardConcept = 'S' "
			        + "AND tar.invalidReason is null "
			        + "JOIN ConceptAncestor ca ON ca.ancestorConcept = tar.id "
			        + "JOIN Concept c ON ca.ancestorConcept = c.id "
			        + "WHERE src.conceptCode = :med_code "
			        + "AND 'NDC' = src.vocabulary "
			        + "AND c.vocabulary = 'RxNorm' "
			        + "AND c.conceptClass = 'Ingredient' "
			        + "AND src.invalidReason is null";
		} else if ("RxNorm".equals(concept.getVocabulary().getId())) {
			// when RxNorm.
			sqlQuery = "select c "
					+ "FROM Concept src " 
					+ "JOIN ConceptAncestor ca ON ca.descendantConcept = src.id "
					+ "JOIN Concept c ON ca.ancestorConcept = c.id "
					+ "WHERE src.conceptCode = :med_code "
					+ "AND 'RxNorm' = src.vocabulary "
					+ "AND c.vocabulary = 'RxNorm' "
					+ "AND c.conceptClass = 'Ingredient' "
					+ "AND src.invalidReason is null "
					+ "AND c.invalidReason is null";
		} else {
			return concepts;
		}
		
		TypedQuery<Concept> query = em.createQuery(sqlQuery, Concept.class);
		query = query.setParameter("med_code", concept.getConceptCode());
		return query.getResultList();
//		List<Concept> results = query.getResultList();
//		if (results.size() > 0) {
//			return results.get(0);
//		} else {
//			return null;
//		}
	}
}
