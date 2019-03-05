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
package edu.gatech.chai.smart.jpa.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.gatech.chai.smart.jpa.dao.SmartOnFhirLaunchContextDao;
import edu.gatech.chai.smart.jpa.entity.SmartLaunchContext;

@Service
public class SmartOnFhirLaunchContextServiceImp implements SmartOnFhirLaunchContextService {

	@Autowired
	private SmartOnFhirLaunchContextDao vDao;
	private Class<SmartLaunchContext> entityClass;

	public SmartOnFhirLaunchContextServiceImp() {
		this.entityClass = SmartLaunchContext.class;
	}
	
	public SmartOnFhirLaunchContextDao getEntityDao() {
		return vDao;
	}
	
	public Class<SmartLaunchContext> getEntityClass() {
		return this.entityClass;
	}

	public SmartLaunchContext getContext(Long id) {
		return vDao.findOneAndDelete(id);
	}

	public void setContext(SmartLaunchContext context) {
		vDao.saveAndDelete(context);
	}
}
