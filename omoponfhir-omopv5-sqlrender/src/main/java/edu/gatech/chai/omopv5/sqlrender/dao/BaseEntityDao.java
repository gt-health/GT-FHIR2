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
 *******************************************************************************/
package edu.gatech.chai.omopv5.sqlrender.dao;

import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import edu.gatech.chai.omopv5.model.entity.BaseEntity;

@Component
public abstract class BaseEntityDao<T extends BaseEntity> implements IDao<T> {
	@Autowired
	private DataSource ds;

	public DataSource getDataSource() {
		return ds;
	}
	
	public Connection getConnection() throws SQLException {
		return ds.getConnection();
	}
	
	public void add(T baseEntity) {
	}
	
	public T findById(Class<T> entityClass, Long id) {
		return null;
	}
	
	public void merge(T baseEntity) {
	}

	public void rollback() {
	}

	public Long delete(Class<T> entityClass, Long id) {		
		return 0L;
	}
	
}
