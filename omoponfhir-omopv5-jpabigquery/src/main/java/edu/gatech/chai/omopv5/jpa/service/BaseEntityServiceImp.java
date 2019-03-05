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
package edu.gatech.chai.omopv5.jpa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import edu.gatech.chai.omopv5.jpa.dao.BaseEntityDao;
import edu.gatech.chai.omopv5.jpa.entity.BaseEntity;

public abstract class BaseEntityServiceImp<T extends BaseEntity, V extends BaseEntityDao<T>>  implements IService<T> {

	@Autowired
	private V vDao;
	private Class<T> entityClass;
	
	public BaseEntityServiceImp(Class<T> entityClass) {
		this.entityClass = entityClass;
	}
	
	public V getEntityDao() {
		return vDao;
	}
	
	public Class<T> getEntityClass() {
		return this.entityClass;
	}
	
	@Transactional(readOnly = true)
	public T findById(Long id) {
		return vDao.findById(entityClass, id);
	}

	@Transactional(readOnly = true)
	public List<T> searchByColumnString(String column, String value) {
		EntityManager em = vDao.getEntityManager();
		List<T> retvals = new ArrayList<T>();
		
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<T> query = builder.createQuery(entityClass);
		Root<T> root = query.from(entityClass);
		
		Path<String> path = root.get(column);
		Predicate where = builder.like(builder.lower(path), value.toLowerCase());

		query.select(root);
		query.where(where);
		
		retvals = em.createQuery(query).getResultList();
		return retvals;	
	}

	@Transactional
	public T create(T entity) {
		vDao.add(entity);
		return entity;
	}
	
	@Transactional
	public Long removeById(Long id) {
		return vDao.delete(entityClass, id);
	}

	@Transactional
	public T update(T entity) {
		vDao.merge(entity);
		return entity;
	}
	
	@Transactional(readOnly = true)
	public Long getSize() {
		EntityManager em = vDao.getEntityManager();
		CriteriaBuilder builder = em.getCriteriaBuilder();

//		Table t = entityClass.getAnnotation(Table.class);
//		if (t != null) {
//			String tableName = t.name();
//			String queryString = "SELECT p.reltuples AS approximate_row_count FROM pg_class p WHERE p.relname = :table_name";
//			Query query = em.createNativeQuery(queryString);
//			query = query.setParameter("table_name", tableName);
//			float res = (float) query.getSingleResult();
//			return (long) res;
//		} else {
			CriteriaQuery<Long> query = builder.createQuery(Long.class);
			Root<T> root = query.from(entityClass);
	
			query.select(builder.count(root));		
			return em.createQuery(query).getSingleResult();		
//		}
	}

	@Transactional(readOnly = true)
	public Long getSize(List<ParameterWrapper> paramList) {
		// Construct predicate from this map.
		EntityManager em = vDao.getEntityManager();
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Long> query = builder.createQuery(Long.class);
		Root<T> root = query.from(entityClass);

		List<Predicate> predicates = ParameterWrapper.constructPredicate(builder, paramList, root);		
		if (predicates == null || predicates.isEmpty()) return 0L;

		query.select(builder.count(root));
		query.where(builder.and(predicates.toArray(new Predicate[predicates.size()])));
		
		return em.createQuery(query).getSingleResult();
	}
	
	protected List<Order> addSort (CriteriaBuilder builder, Root<T> root, String sort) {
		List<Order> orders = new ArrayList<Order>();
		if (sort != null && !sort.isEmpty()) {
			String[] sort_ = sort.split(",");
			for (int i=0; i<sort_.length; i++) {
				String[] items = sort_[i].split(" ");
				if ("ASC".equals(items[1])) {
					orders.add(builder.asc(root.get(items[0])));
				} else {
					orders.add(builder.desc(root.get(items[0])));
				}
			}
		} else {
			orders.add(builder.asc(root.get("id")));
		}
		
		return orders;
	}
	
	@Transactional(readOnly = true)
	public List<T> searchWithoutParams(int fromIndex, int toIndex, String sort) {
		int length = toIndex - fromIndex;
		EntityManager em = vDao.getEntityManager();		
		List<T> retvals = new ArrayList<T>();
		
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<T> query = builder.createQuery(entityClass);
		Root<T> root = query.from(entityClass);
		
		query.select(root);
		
		// Sort		
		query.orderBy(addSort(builder, root, sort));
		
		if (length <= 0) {
			retvals = em.createQuery(query)
					.getResultList();
		} else {
			retvals = em.createQuery(query)
					.setFirstResult(fromIndex)
					.setMaxResults(length)
					.getResultList();
		}
		return retvals;	
	}

	@Transactional(readOnly = true)
	public List<T> searchWithParams(int fromIndex, int toIndex, List<ParameterWrapper> paramList, String sort) {
		int length = toIndex - fromIndex;
		EntityManager em = vDao.getEntityManager();
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<T> query = builder.createQuery(entityClass);
		Root<T> root = query.from(entityClass);
		
		List<T> retvals = new ArrayList<T>();
		
		List<Predicate> predicates = ParameterWrapper.constructPredicate(builder, paramList, root);		
		if (predicates == null || predicates.isEmpty()) return retvals; // Nothing. return empty list
	
		query.select(root);
		query.where(builder.and(predicates.toArray(new Predicate[predicates.size()])));

		// Sort		
		query.orderBy(addSort(builder, root, sort));

		if (length <= 0) {
			retvals = em.createQuery(query)
					.getResultList();			
		} else {
			retvals = em.createQuery(query)
					.setFirstResult(fromIndex)
					.setMaxResults(length)
					.getResultList();
		}
		return retvals;
	}

}
