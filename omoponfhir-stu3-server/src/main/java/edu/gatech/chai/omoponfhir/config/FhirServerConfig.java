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

import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

//import edu.gatech.chai.omopv5.jpa.service.CareSiteService;
//import edu.gatech.chai.omopv5.jpa.service.CareSiteServiceImp;

@Configuration
@EnableTransactionManagement
@ComponentScans(value = { @ComponentScan("edu.gatech.chai.omopv5.jpa.dao"),
		@ComponentScan("edu.gatech.chai.omopv5.dba.service"),
		@ComponentScan("edu.gatech.chai.smart.jpa.dao"),
		@ComponentScan("edu.gatech.chai.smart.jpa.service") })
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

	@Bean()
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean retVal = new LocalContainerEntityManagerFactoryBean();
		retVal.setPersistenceUnitName("GT-FHIR2");
//		retVal.setDataSource(dataSource());
		retVal.setDataSource(dataSource);
		retVal.setPackagesToScan("edu.gatech.chai.omopv5.model.entity", "edu.gatech.chai.smart.jpa.entity");
		retVal.setPersistenceProvider(new HibernatePersistenceProvider());
		retVal.setJpaProperties(jpaProperties());
		return retVal;
	}

	private Properties jpaProperties() {
		Properties extraProperties = new Properties();
		extraProperties.put("hibernate.dialect", org.hibernate.dialect.PostgreSQL94Dialect.class.getName());
//		extraProperties.put("hibernate.dialect", edu.gatech.chai.omopv5.jpa.enity.noomop.OmopPostgreSQLDialect.class.getName());
		extraProperties.put("hibernate.format_sql", "true");
		extraProperties.put("hibernate.show_sql", "false");
		extraProperties.put("hibernate.hbm2ddl.auto", "update");
//		extraProperties.put("hibernate.hbm2ddl.auto", "none");
//		extraProperties.put("hibernate.enable_lazy_load_no_trans", "true");
		extraProperties.put("hibernate.jdbc.batch_size", "20");
		extraProperties.put("hibernate.cache.use_query_cache", "false");
		extraProperties.put("hibernate.cache.use_second_level_cache", "false");
		extraProperties.put("hibernate.cache.use_structured_entries", "false");
		extraProperties.put("hibernate.cache.use_minimal_puts", "false");
		// extraProperties.put("hibernate.search.model_mapping",
		// SearchMappingFactory.class.getName());
		extraProperties.put("hibernate.search.default.directory_provider", "filesystem");
		extraProperties.put("hibernate.search.default.indexBase", "target/lucenefiles");
		extraProperties.put("hibernate.search.lucene_version", "LUCENE_CURRENT");
		// extraProperties.put("hibernate.search.default.worker.execution",
		// "async");
		return extraProperties;
	}

	@Bean()
	public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		JpaTransactionManager retVal = new JpaTransactionManager();
		retVal.setEntityManagerFactory(entityManagerFactory);
		return retVal;
	}

}
