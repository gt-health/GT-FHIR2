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
/**
 * 
 */
package edu.gatech.chai.smart.jpa.dao;

import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Repository;

import edu.gatech.chai.smart.jpa.entity.SmartLaunchContext;

/**
 * @author mc142
 *
 */
@Repository
public class SmartOnFhirLaunchContextDao implements ISmartFhirDao<SmartLaunchContext> {
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(SmartOnFhirLaunchContextDao.class);

//	public SmartOnFhirLaunchContextDao() {
//		super();
//		setClazz(SmartLaunchContext.class);
//	}

	@PersistenceContext
	private EntityManager em;

	public EntityManager getEntityManager() {
		return em;
	}

	@SuppressWarnings("unchecked")
	public SmartLaunchContext findOne(Long id) {
		return (SmartLaunchContext) em.find(SmartLaunchContext.class, id);
	}

	public void deleteOneDayOld () {
		// Get today's date. And, delete any entries that are one day old.
		Date yesterday = DateUtils.addDays(new Date(), -1);
		Query q = em.createQuery("DELETE FROM SmartLaunchContext s WHERE s.createdAt < :yesterday or s.createdAt is NULL");
		q.setParameter("yesterday", yesterday);		
		q.executeUpdate();		
	}
	
	public SmartLaunchContext findOneAndDelete(Long id) {
		deleteOneDayOld();		
		return findOne(id);
	}
	
	public void saveAndDelete (SmartLaunchContext entity) {
		save (entity);
		deleteOneDayOld();
		
		
//		SmartLaunchContext smartLaunchContext = (SmartLaunchContext) entity;
//		EntityManager em = getBaseFhirDao().getEntityManager();
//		Query q = em.createQuery("select s from SmartLaunchContext s where s.username=:username and s.createdBy=:who and s.clientId=:what")
//				.setParameter("username", smartLaunchContext.getUsername())
//				.setParameter("who", smartLaunchContext.getCreatedBy())
//				.setParameter("what", smartLaunchContext.getClientId());
//		
//		List<SmartLaunchContext> launchContextEntities = q.getResultList();
//		if (launchContextEntities.isEmpty()) {
//			save (entity);
//		} else {
//			// We may have multiple entries (not supposed to). But if so, we just use the first one.
//			SmartLaunchContext toBeUpdated = launchContextEntities.get(0);
//			smartLaunchContext.setLaunchId(toBeUpdated.getLaunchId());
//			update((T) smartLaunchContext);
//		}
	}
	
	public void save(SmartLaunchContext entity) {
		em.persist(entity);
		em.flush();
	}

	public void update(SmartLaunchContext entity) {
		em.merge(entity);
	}

	public void delete(SmartLaunchContext entity) {
		em.remove(entity);
	}

	public void deleteById(Long entityId) {
		SmartLaunchContext entity = this.findOne(entityId);

		this.delete(entity);
	}

}
