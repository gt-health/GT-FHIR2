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

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.gatech.chai.omopv5.model.entity.CareSite;
import edu.gatech.chai.omopv5.model.entity.Location;
import edu.gatech.chai.omopv5.sqlrender.dao.CareSiteDao;

@Service
public class CareSiteServiceImp extends BaseEntityServiceImp<CareSite, CareSiteDao> implements CareSiteService{
	
	public CareSiteServiceImp() {
		super(CareSite.class);
	}
	
	@Transactional(readOnly = true)
	public CareSite searchByLocation(Location location) {
		EntityManager em = getEntityDao().getEntityManager();
		String query = "SELECT t FROM CareSite t WHERE location_id like :value";
		List<? extends CareSite> results = em.createQuery(query, CareSite.class)
				.setParameter("value",location.getId()).getResultList();
		if (results.size() > 0) {
			return results.get(0);
		} else
			return null;
	}

	@Transactional(readOnly = true)
	public CareSite searchByNameAndLocation(String careSiteName, Location location) {
		EntityManager em = getEntityDao().getEntityManager();
		String queryString = "SELECT t FROM CareSite t WHERE";
		
		// Construct where clause here.
		String where_clause = "";
		if (careSiteName != null)  {
			where_clause = "careSiteName like :cName";
		}
		
		if (location != null) {
			if (where_clause == "") where_clause = "location = :location";
			else where_clause += " AND location = :location";
		}
		
		queryString += " "+where_clause;
		System.out.println("Query for FPerson"+queryString);
		
		TypedQuery<? extends CareSite> query = em.createQuery(queryString, CareSite.class);
		if (careSiteName != null) query = query.setParameter("cName", careSiteName);
		if (location != null) query = query.setParameter("location", location);
		
		System.out.println("cName:"+careSiteName);
		List<? extends CareSite> results = query.getResultList();
		if (results.size() > 0) {
			return results.get(0);
		} else {
			return null;	
		}
	}

}
