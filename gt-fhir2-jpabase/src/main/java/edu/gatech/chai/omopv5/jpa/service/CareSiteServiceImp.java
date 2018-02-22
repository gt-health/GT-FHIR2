package edu.gatech.chai.omopv5.jpa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.gatech.chai.omopv5.jpa.dao.CareSiteDao;
import edu.gatech.chai.omopv5.jpa.entity.CareSite;
import edu.gatech.chai.omopv5.jpa.entity.Location;

@Service
public class CareSiteServiceImp implements CareSiteService {

	@Autowired
	private CareSiteDao careSiteDao;
	
	@Transactional(readOnly = true)
	@Override
	public CareSite findById(Long id) {
		return careSiteDao.findById(id);
	}

	@Transactional(readOnly = true)
	@Override
	public CareSite searchByColumnString(String column, String value) {
		EntityManager em = careSiteDao.getEntityManager();
		
		String query = "SELECT t FROM CareSite t WHERE "+column+" like :value";
		List<? extends CareSite> results = em.createQuery(query, CareSite.class)
				.setParameter("value", value).getResultList();
		if (results.size() > 0)
			return results.get(0);
		else
			return null;	
	}
	
	@Transactional(readOnly = true)
	@Override
	public CareSite searchByLocation(Location location) {
		EntityManager em = careSiteDao.getEntityManager();
		String query = "SELECT t FROM CareSite t WHERE location_id like :value:";
		List<? extends CareSite> results = em.createQuery(query, CareSite.class)
				.setParameter("value",location.getId()).getResultList();
		if (results.size() > 0) {
			return results.get(0);
		} else
			return null;
	}

	@Transactional
	@Override
	public CareSite createOrUpdate(CareSite entity) {
		if (entity.getId() != null) {
			careSiteDao.merge(entity);
		} else {
			careSiteDao.add(entity);
		}
		return entity;
	}

	@Transactional(readOnly = true)
	@Override
	public Long getSize() {
		EntityManager em = careSiteDao.getEntityManager();
		
		String query = "SELECT COUNT(t) FROM CareSite t";
		Long totalSize = em.createQuery(query, Long.class).getSingleResult();
		return totalSize;
	}

	@Override
	public Long getSize(Map<String, List<ParameterWrapper>> paramMap) {
		// Construct predicate from this map.
		EntityManager em = careSiteDao.getEntityManager();
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Long> careSiteQuery = builder.createQuery(Long.class);
		Root<CareSite> careSiteRoot = careSiteQuery.from(CareSite.class);

		List<Predicate> predicates = ParameterWrapper.constructPredicate(builder, paramMap, careSiteRoot);		
		if (predicates == null || predicates.isEmpty()) return 0L;

		careSiteQuery.select(builder.count(careSiteRoot));
		careSiteQuery.where(builder.and(predicates.toArray(new Predicate[predicates.size()])));
		
		return em.createQuery(careSiteQuery).getSingleResult();
	}

	@Override
	public List<CareSite> searchWithoutParams(int fromIndex, int toIndex) {
		int length = toIndex - fromIndex;
		EntityManager em = careSiteDao.getEntityManager();
		
		String query = "SELECT p FROM CareSite p ORDER BY id ASC";
		List<CareSite> retvals = (List<CareSite>) em.createQuery(query, CareSite.class)
				.setFirstResult(fromIndex)
				.setMaxResults(length)
				.getResultList();
		
		return retvals;
	}

	@Override
	public List<CareSite> searchWithParams(int fromIndex, int toIndex, Map<String, List<ParameterWrapper>> paramMap) {
		int length = toIndex - fromIndex;
		EntityManager em = careSiteDao.getEntityManager();
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<CareSite> careSiteQuery = builder.createQuery(CareSite.class);
		Root<CareSite> careSiteRoot = careSiteQuery.from(CareSite.class);
		
		List<CareSite> careSite = new ArrayList<CareSite>();
		List<Predicate> predicates = ParameterWrapper.constructPredicate(builder, paramMap, careSiteRoot);		
		if (predicates == null || predicates.isEmpty()) return careSite; // Nothing. return empty list
	
		careSiteQuery.select(careSiteRoot);
		careSiteQuery.where(builder.and(predicates.toArray(new Predicate[predicates.size()])));

		List<CareSite> retvals = em.createQuery(careSiteQuery)
				.setFirstResult(fromIndex)
				.setMaxResults(length)
				.getResultList();
		return retvals;
	}

	@Transactional(readOnly = true)
	@Override
	public CareSite searchByNameAndLocation(String careSiteName, Location location) {
		EntityManager em = careSiteDao.getEntityManager();
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
