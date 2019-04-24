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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(
		generator = ObjectIdGenerators.PropertyGenerator.class,
		property = "person_id")
public class Person extends BaseEntity {
	@JsonProperty("person_id")
	private Long id;
	
	@JsonProperty("gender_concpet_id")
	private Concept genderConcept;
	
	@JsonProperty("year_of_birth")
	private Integer yearOfBirth;
	
	@JsonProperty("month_of_birth")
	private Integer monthOfBirth;
	
	@JsonProperty("day_of_birth")
	private Integer dayOfBirth;
	
	@JsonProperty("time_of_birth")
	private String timeOfBirth;
	
	@JsonProperty("race_concept_id")
	private Concept raceConcept;
	
	@JsonProperty("ethnicity_concept_id")
	private Concept ethnicityConcept;
	
	@JsonProperty("location_id")
	private Location location;
	
	@JsonProperty("provider_id")
	private Provider provider;

	@JsonProperty("care_site_id")
	private CareSite careSite;
	
	@JsonProperty("person_source_value")
	private String personSourceValue;
	
	@JsonProperty("gender_source_value")
	private String genderSourceValue;
	
	@JsonProperty("gender_source_concept_id")
	private Concept genderSourceConcept;
	
	@JsonProperty("race_source_value")
	private String raceSourceValue;
	
	@JsonProperty("race_source_concept_id")
	private Concept raceSourceConcept;

	@JsonProperty("ethnicity_source_value")
	private String ethnicitySourceValue;
	
	@JsonProperty("ethnicity_source_concept_id")
	private Concept ethnicitySourceConcept;

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
