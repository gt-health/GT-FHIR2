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
package edu.gatech.chai.omopv5.jpa.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/** 
 * This class adds some properties to the Omop data model Person, in order to provide
 * all the data specified for FHIR.
 * @author Ismael Sarmento
 */
@Entity
@Table(name="f_person")
@PrimaryKeyJoinColumn(name="person_id")
public class FPerson extends Person {
	@Column(name="family_name")
	private String familyName;
	
	@Column(name="given1_name")
	private String givenName1;
	
	@Column(name="given2_name")
	private String givenName2;
	
	@Column(name="prefix_name")
	private String prefixName;
	
	@Column(name="suffix_name")
	private String suffixName;
	
	@Column(name="preferred_language")
	private String preferredLanguage;
	
	@Column(name="ssn")
	private String ssn;
	
	@Column(name="maritalstatus")
	private String maritalStatus;
	
	@Column(name="active")
	private Short active;
	
	@Column(name="contact_point1")
	String contactPoint1;
	
	@Column(name="contact_point2")
	String contactPoint2;
	
	@Column(name="contact_point3")
	String contactPoint3;
	
	public FPerson() {
		super();
	}

	public String getFamilyName() {
		return familyName;
	}

	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}

	public String getGivenName1() {
		return givenName1;
	}

	public void setGivenName1(String givenName1) {
		this.givenName1 = givenName1;
	}

	public String getGivenName2() {
		return givenName2;
	}

	public void setGivenName2(String givenName2) {
		this.givenName2 = givenName2;
	}

	public String getPrefixName() {
		return prefixName;
	}

	public void setPrefixName(String prefixName) {
		this.prefixName = prefixName;
	}

	public String getSuffixName() {
		return suffixName;
	}

	public void setSuffixName(String suffixName) {
		this.suffixName = suffixName;
	}

	public String getPreferredLanguage() {
		return preferredLanguage;
	}

	public void setPreferredLanguage(String preferredLanguage) {
		this.preferredLanguage = preferredLanguage;
	}

	public String getSsn() {
		return ssn;
	}

	public void setSsn(String ssn) {
		this.ssn = ssn;
	}

	public String getMaritalStatus() {
		return maritalStatus;
	}

	public void setMaritalStatus(String maritalStatus) {
		this.maritalStatus = maritalStatus;
	}
	
	public Short getActive() {
		return active;
	}
	
	public void setActive(Short active) {
		this.active = active;
	}
	
	public String getContactPoint1() {
		return contactPoint1;
	}
	
	public void setContactPoint1(String contactPoint1) {
		this.contactPoint1 = contactPoint1;
	}
	
	public String getContactPoint2() {
		return contactPoint2;
	}
	
	public void setContactPoint2(String contactPoint2) {
		this.contactPoint2 = contactPoint2;
	}
	
	public String getContactPoint3() {
		return contactPoint3;
	}
	
	public void setContactPoint3(String contactPoint3) {
		this.contactPoint3 = contactPoint3;
	}
	
	
	public String getNameAsSingleString() {
		String name="";
		if (this.givenName1 != null && !this.givenName1.isEmpty())
			name = this.givenName1;
		if (this.givenName2 != null && !this.givenName2.isEmpty())
			name = name+" "+this.givenName2;
		if (this.familyName != null && !this.familyName.isEmpty()) 
			name = name+" "+this.familyName;
		
		return name;
	}
	
}
