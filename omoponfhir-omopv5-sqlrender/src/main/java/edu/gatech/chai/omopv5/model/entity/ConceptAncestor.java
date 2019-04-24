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

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ConceptAncestor extends BaseEntity implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static String tableName = "concept_ancestor";

	@JsonProperty("ancestor_concept_id")
	private Concept ancestorConcept;

	@JsonProperty("descendant_concept_id")
	private Concept descendantConcept;

	@JsonProperty("min_levels_of_separation")
	private Integer minLevelsOfSeparation;
	
	@JsonProperty("max_levels_of_separation")
	private Integer maxLevelsOfSeparation;
	
	public Concept getAncestorConcept() {
		return this.ancestorConcept;
	}
	
	public Concept getDescendantConcept() {
		return this.descendantConcept;
	}
	
	public Integer getMinLevelsOfSeparation() {
		return this.minLevelsOfSeparation;
	}
	
	public Integer getMaxLevelsOfSeparation() {
		return this.maxLevelsOfSeparation;
	}
	
	@Override
	public Long getIdAsLong() {
		// TODO Auto-generated method stub
		return null;
	}

}
