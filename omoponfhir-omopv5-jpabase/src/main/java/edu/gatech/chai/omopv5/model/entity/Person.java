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
package edu.gatech.chai.omopv5.model.entity;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(
		name="person",
		indexes = { 
				@Index(name = "idx_person_location_id", columnList = "location_id"), 
				@Index(name = "idx_person_care_site_id", columnList = "care_site_id") 
				}
		)
@Inheritance(strategy=InheritanceType.JOINED)
public class Person extends BaseEntity {

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="person_id_seq")
	@SequenceGenerator(name="person_id_seq", sequenceName="person_id_seq", allocationSize=1)
	@Column(name="person_id", nullable = false)
	@Access(AccessType.PROPERTY)
	private Long id;
	
	@ManyToOne
	@JoinColumn(name="gender_concept_id", nullable= false)
	private Concept genderConcept;
	
	@Column(name="year_of_birth", nullable=false)
	private Integer yearOfBirth;
	
	@Column(name="month_of_birth")
	private Integer monthOfBirth;
	
	@Column(name="day_of_birth")
	private Integer dayOfBirth;
	
	@Column(name="time_of_birth")
	private String timeOfBirth;
	
	@ManyToOne
	@JoinColumn(name="race_concept_id")
	private Concept raceConcept;
	
	@ManyToOne
	@JoinColumn(name="ethnicity_concept_id")
	private Concept ethnicityConcept;
	
	@ManyToOne
	@JoinColumn(name="location_id")
	private Location location;
	
	@ManyToOne
	@JoinColumn(name="provider_id")
	private Provider provider;

	@ManyToOne
	@JoinColumn(name="care_site_id")
	private CareSite careSite;
	
	@Column(name="person_source_value")
	private String personSourceValue;
	
	@Column(name="gender_source_value")
	private String genderSourceValue;
	
	@ManyToOne
	@JoinColumn(name="gender_source_concept_id")
	private Concept genderSourceConcept;
	
	@Column(name="race_source_value")
	private String raceSourceValue;
	
	@ManyToOne
	@JoinColumn(name="race_source_concept_id")
	private Concept raceSourceConcept;

	@Column(name="ethnicity_source_value")
	private String ethnicitySourceValue;
	
	@ManyToOne
	@JoinColumn(name="ethnicity_source_concept_id")
	private Concept ethnicitySourceConcept;

//	@OneToMany(orphanRemoval=true, mappedBy="person")
//	private Set<ConditionOccurrence> conditions;
	
	//private Death death;

	public Person() {
		super();
		this.genderConcept = new Concept();
		this.genderConcept.setId(0L);
		this.raceConcept = new Concept();
		this.raceConcept.setId(0L);
		this.setYearOfBirth(0);
		this.ethnicityConcept = new Concept();
		this.ethnicityConcept.setId(0L);
	}

	public Person(Long id, Concept genderConcept, Integer yearOfBirth, Integer monthOfBirth,
			Integer dayOfBirth, String timeOfBirth, Concept raceConcept, Concept ethnicityConcept, 
			Location location, Provider provider, CareSite careSite, String personSourceValue,
			String genderSourceValue, Concept genderSourceConcept, String raceSourceValue,
			Concept raceSourceConcept, String ethnicitySourceValue, Concept ethnicitySourceConcept) {
		super();
		this.id = id;
		this.genderConcept = genderConcept;
		this.yearOfBirth = yearOfBirth;
		this.monthOfBirth = monthOfBirth;
		this.dayOfBirth = dayOfBirth;
		this.timeOfBirth = timeOfBirth;
		this.raceConcept = raceConcept;
		this.ethnicityConcept = ethnicityConcept;
		this.location = location;
		this.provider = provider;
		this.careSite = careSite;
		this.personSourceValue = personSourceValue;
		this.genderSourceValue = genderSourceValue;
		this.genderSourceConcept = genderSourceConcept;
		this.raceSourceValue = raceSourceValue;
		this.raceSourceConcept = raceSourceConcept;
		this.ethnicitySourceValue = ethnicitySourceValue;
		this.ethnicitySourceConcept = ethnicitySourceConcept;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Concept getGenderConcept() {
		return genderConcept;
	}

	public void setGenderConcept(Concept genderConcept) {
		this.genderConcept = genderConcept;
	}

	public Integer getYearOfBirth() {
		return yearOfBirth;
	}

	public void setYearOfBirth(Integer yearOfBirth) {
		this.yearOfBirth = yearOfBirth;
	}

	public Integer getMonthOfBirth() {
		return monthOfBirth;
	}

	public void setMonthOfBirth(Integer monthOfBirth) {
		this.monthOfBirth = monthOfBirth;
	}

	public Integer getDayOfBirth() {
		return dayOfBirth;
	}

	public void setDayOfBirth(Integer dayOfBirth) {
		this.dayOfBirth = dayOfBirth;
	}
	
	public String getTimeOfBirth() {
		return timeOfBirth;
	}
	
	public void setTimeOfBirth(String timeOfBirth) {
		this.timeOfBirth = timeOfBirth;
	}

	public Concept getRaceConcept() {
		return raceConcept;
	}

	public void setRaceConcept(Concept raceConcept) {
		this.raceConcept = raceConcept;
	}
	
	public Concept getEthnicityConcept() {
		return ethnicityConcept;
	}

	public void setEthnicityConcept(Concept ethnicityConcept) {
		this.ethnicityConcept = ethnicityConcept;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public Provider getProvider() {
		return provider;
	}

	public void setProvider(Provider provider) {
		this.provider = provider;
	}

	public CareSite getCareSite() {
		return careSite;
	}
	
	public void setCareSite(CareSite careSite) {
		this.careSite = careSite;
	}
	
	public String getPersonSourceValue() {
		return personSourceValue;
	}

	public void setPersonSourceValue(String personSourceValue) {
		this.personSourceValue = personSourceValue;
	}

	public String getGenderSourceValue() {
		return genderSourceValue;
	}

	public void setGenderSourceValue(String genderSourceValue) {
		this.genderSourceValue = genderSourceValue;
	}
	
	public Concept getGenderSourceConcept() {
		return genderSourceConcept;
	}
	
	public void setGenderSourceConcept(Concept genderSourceConcept) {
		this.genderSourceConcept = genderSourceConcept;
	}

	public String getRaceSourceValue() {
		return raceSourceValue;
	}

	public void setRaceSourceValue(String raceSourceValue) {
		this.raceSourceValue = raceSourceValue;
	}

	public Concept getRaceSourceConcept() {
		return raceSourceConcept;
	}
	
	public void setRaceSourceConcept(Concept raceSourceConcept) {
		this.raceSourceConcept = raceSourceConcept;
	}
	
	public String getEthnicitySourceValue() {
		return ethnicitySourceValue;
	}

	public void setEthnicitySourceValue(String ethnicitySourceValue) {
		this.ethnicitySourceValue = ethnicitySourceValue;
	}

	public Concept getEthnicitySourceConcept() {
		return ethnicitySourceConcept;
	}
	
	public void setEthnicitySourceConcept(Concept ethnicitySourceConcept) {
		this.ethnicitySourceConcept = ethnicitySourceConcept;
	}

	@Override
	public Long getIdAsLong() {
		return getId();
	}

}
