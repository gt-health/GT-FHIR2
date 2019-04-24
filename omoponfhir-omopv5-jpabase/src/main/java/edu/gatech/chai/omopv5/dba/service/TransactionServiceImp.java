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

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.gatech.chai.omopv5.jpa.dao.ConditionOccurrenceDao;
import edu.gatech.chai.omopv5.jpa.dao.FPersonDao;
import edu.gatech.chai.omopv5.jpa.dao.MeasurementDao;
import edu.gatech.chai.omopv5.jpa.dao.ObservationDao;
import edu.gatech.chai.omopv5.jpa.dao.ProcedureOccurrenceDao;
import edu.gatech.chai.omopv5.model.entity.BaseEntity;
import edu.gatech.chai.omopv5.model.entity.FPerson;
import edu.gatech.chai.omopv5.model.entity.Measurement;
import edu.gatech.chai.omopv5.model.entity.Observation;

@Service
public class TransactionServiceImp implements TransactionService {

//	@Autowired
//	private TransactionDao transactionDao;
	
	@Autowired
	private FPersonDao fPersonDao;
	@Autowired
	private ConditionOccurrenceDao conditionDao;
	@Autowired
	private ProcedureOccurrenceDao procedureDao;
	@Autowired
	private MeasurementDao measurementDao;
	@Autowired
	private ObservationDao observationDao;

//	public TransactionDao getEntityDao() {
//		return transactionDao;
//	}
	
	@Transactional
	public int writeTransaction(Map<String, List<BaseEntity>> transactionMap) {
//		EntityManager em = transactionDao.getEntityManager();

		System.out.println("At the writeTransaction");
		// It's not efficient. But, we need to process patients first. so...
		for (String key : transactionMap.keySet()) {
			String[] keyInfo = key.split("\\^");
			System.out.println("working on patient key=" + key + " with keyInfo length=" + keyInfo.length);
			if (keyInfo.length != 2) {
				// something is wrong.
				return -1;
			}
			String entityName = keyInfo[1];
			if (entityName.equals("FPerson")) {
				// process it.
				List<BaseEntity> entityClasses = transactionMap.get(key);
				if (entityClasses != null) {
					// We have patients, process this. 
					for (BaseEntity entity : entityClasses) {
						FPerson fPerson = (FPerson) entity;
						System.out.println("Writing fPerson name="+fPerson.getFamilyName()+", "+fPerson.getGivenName1());
						fPersonDao.add(fPerson);
					}
				}

			} else {
				continue;
			}			
		}
		
		for (String key : transactionMap.keySet()) {
			String[] keyInfo = key.split("\\^");
			System.out.println("working on key=" + key + " with keyInfo length=" + keyInfo.length);
			if (keyInfo.length != 2) {
				// something is wrong.
				return -1;
			}
			// 2nd part of keyInfo should be the entity table name.
			String entityName = keyInfo[1];
			if (entityName.equals("FPerson")) {
				// we have alreay done this.
				continue;
			}
			
			List<BaseEntity> entities = transactionMap.get(key);
			if (entities == null) {
				System.out.println("null entities for transactionMap.....");
				continue;
			}
			System.out.println("About to process " + entityName + " from Service");
			
			String subjectKey = keyInfo[0]+"^"+"FPerson";
			
			// list of the entity classes
			try {
				for (BaseEntity entity : entities) {
					if (entityName.equals("Measurement")) {
						System.out.println("Adding Measurement with subjectKey ("+key+") to OMOP");
						// Get patient information from subject key.
						FPerson subjectEntity = (FPerson) transactionMap.get(subjectKey).get(0);
						if (subjectEntity == null) {
							// This is an error. We must have subject.
							System.out.println("FPerson info not available for the Measurement");
							throw new Exception("FPerson info not available for the Measurement");
						}
						
						Measurement measurement = (Measurement) entity;
						measurement.setFPerson(subjectEntity);
						
						measurementDao.add(measurement);
					} else if (entityName.equals("Observation")) {
						System.out.println("Adding Observation to OMOP");
						FPerson subjectEntity = (FPerson) transactionMap.get(subjectKey).get(0);
						if (subjectEntity == null) {
							// This is an error. We must have subject.
							System.out.println("FPerson info not available for the Observation");
							throw new Exception("FPerson info not available for the Observation");
						}
						
						Observation observation = (Observation) entity;
						observation.setFPerson(subjectEntity);

						observationDao.add(observation);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				return -1;
			}
		}
		return 0;
	}	
}