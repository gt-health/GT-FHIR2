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
package edu.gatech.chai.smart.jpa.entity;

import java.io.Serializable;
import javax.persistence.*;

import edu.gatech.chai.omopv5.jpa.entity.BaseEntity;


/**
 * The persistent class for the smart_launch_context_params database table.
 * 
 */
@Entity
@Table(name="smart_launch_context_params")
@NamedQuery(name="SmartLaunchContextParam.findAll", query="SELECT s FROM SmartLaunchContextParam s")
public class SmartLaunchContextParam extends BaseEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@Column(name="param_name")
	private String paramName;

	@Column(name="param_value")
	private String paramValue;

	//bi-directional many-to-one association to SmartLaunchContext
	@ManyToOne(cascade=CascadeType.ALL,fetch = FetchType.EAGER)
	@JoinColumn(name="launch_context_id")
	private SmartLaunchContext smartLaunchContext;

	public SmartLaunchContextParam() {
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getParamName() {
		return this.paramName;
	}

	public void setParamName(String paramName) {
		this.paramName = paramName;
	}

	public String getParamValue() {
		return this.paramValue;
	}

	public void setParamValue(String paramValue) {
		this.paramValue = paramValue;
	}

	public SmartLaunchContext getSmartLaunchContext() {
		return this.smartLaunchContext;
	}

	public void setSmartLaunchContext(SmartLaunchContext smartLaunchContext) {
		this.smartLaunchContext = smartLaunchContext;
	}

	@Override
	public Long getIdAsLong() {
		return id;
	}

}