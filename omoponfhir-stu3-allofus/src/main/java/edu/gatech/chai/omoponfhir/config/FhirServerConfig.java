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
package edu.gatech.chai.omoponfhir.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource({
    "classpath:database-config.xml"
})
public class FhirServerConfig {
	@Autowired
	DataSource dataSource;
//	@Bean(destroyMethod = "close")
//	public DataSource dataSource() {
//		BasicDataSource retVal = new BasicDataSource();
//		retVal.setDriver(new org.postgresql.Driver());
//		retVal.setUrl("jdbc:postgresql://localhost:5432/postgres?currentSchema=omop_v5");
//		retVal.setUsername("omop_v5");
//		retVal.setPassword("i3lworks");
//		return retVal;
//	}

}
