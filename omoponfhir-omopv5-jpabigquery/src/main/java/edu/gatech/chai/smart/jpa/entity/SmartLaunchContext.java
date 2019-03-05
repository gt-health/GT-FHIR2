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

import org.json.JSONObject;

import edu.gatech.chai.omopv5.jpa.entity.BaseEntity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


/**
 * The persistent class for the smart_launch_context database table.
 * 
 */
@Entity
@Table(name="smart_launch_context")
@NamedQuery(name="SmartLaunchContext.findAll", query="SELECT s FROM SmartLaunchContext s")
public class SmartLaunchContext extends BaseEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="smart_launch_context_launch_id_gen")
	@SequenceGenerator(name="smart_launch_context_launch_id_gen", sequenceName="smart_launch_context_seq", allocationSize=1)
	@Column(name="launch_id")
	private Long launchId;

	@Column(name="client_id")
	private String clientId;

	@Column(name="created_at")
	private Timestamp createdAt;

	@Column(name="created_by")
	private String createdBy;

	private String username;

	//bi-directional many-to-one association to SmartLaunchContextParam
	@OneToMany(fetch = FetchType.EAGER,cascade=CascadeType.ALL,mappedBy="smartLaunchContext")
	private List<SmartLaunchContextParam> smartLaunchContextParams;

	public SmartLaunchContext() {
	}

	public Long getLaunchId() {
		return this.launchId;
	}

	public void setLaunchId(Long launchId) {
		this.launchId = launchId;
	}

	public String getClientId() {
		return this.clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public Timestamp getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public String getCreatedBy() {
		return this.createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public List<SmartLaunchContextParam> getSmartLaunchContextParams() {
		return this.smartLaunchContextParams;
	}

	public void setSmartLaunchContextParams(List<SmartLaunchContextParam> smartLaunchContextParams) {
		this.smartLaunchContextParams = smartLaunchContextParams;
	}

	public SmartLaunchContextParam addSmartLaunchContextParam(SmartLaunchContextParam smartLaunchContextParam) {
		getSmartLaunchContextParams().add(smartLaunchContextParam);
		smartLaunchContextParam.setSmartLaunchContext(this);

		return smartLaunchContextParam;
	}

	public SmartLaunchContextParam removeSmartLaunchContextParam(SmartLaunchContextParam smartLaunchContextParam) {
		getSmartLaunchContextParams().remove(smartLaunchContextParam);
		smartLaunchContextParam.setSmartLaunchContext(null);

		return smartLaunchContextParam;
	}

	public JSONObject getJSONObject() {
		JSONObject ret = new JSONObject();
		ret.put("launch_id", this.getLaunchId());
		ret.put("username", this.getUsername());
		ret.put("created_by", this.getCreatedBy());
		ret.put("created_at", this.getCreatedAt());
		
		JSONObject param = new JSONObject();
		List<SmartLaunchContextParam> smartLaunchContextParams = this.getSmartLaunchContextParams();
		for (SmartLaunchContextParam launchParam : smartLaunchContextParams) {
			param.put(launchParam.getParamName(), launchParam.getParamValue());
		}
		param.put("need_patient_banner", true);
		param.put("smart_style_url", "https://fhir.smarthealthit.org/stylesheets/smart_v1.json");
		
		ret.put("parameters", param);
	    
		return ret;
	}

	@Override
	public Long getIdAsLong() {
		return launchId;
	}
}