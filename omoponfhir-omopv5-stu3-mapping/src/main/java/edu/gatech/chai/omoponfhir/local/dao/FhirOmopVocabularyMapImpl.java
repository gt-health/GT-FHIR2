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
package edu.gatech.chai.omoponfhir.local.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gatech.chai.omoponfhir.local.model.FhirOmopVocabularyMapEntry;

public class FhirOmopVocabularyMapImpl implements FhirOmopVocabularyMap {
	final static Logger logger = LoggerFactory.getLogger(FhirOmopVocabularyMapImpl.class);

	@Override
	public Connection connect() {
		String url = "jdbc:sqlite::resource:omoponfhir.db";
		Connection conn = null;
		try {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection(url);
			logger.info("Connected to database");
		} catch (SQLException e) {
			logger.info(e.getMessage());
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			logger.info(e.getMessage());
			e.printStackTrace();
		}

		return conn;
	}

	@Override
	public int save(FhirOmopVocabularyMapEntry conceptMapEntry) {
		String sql = "INSERT INTO FhirOmopVocabularyMap (omop_concept_code_name, fhir_url_system_name, other_system_name) values (?,?,?)";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, conceptMapEntry.getOmopConceptCodeName());
			pstmt.setString(2, conceptMapEntry.getFhirUrlSystemName());
			pstmt.setString(3, conceptMapEntry.getOtherSystemName());

			pstmt.executeUpdate();

			logger.info("New Map entry data added (" + conceptMapEntry.getOmopConceptCodeName() + ", "
					+ conceptMapEntry.getFhirUrlSystemName() + ", " + conceptMapEntry.getOtherSystemName());
		} catch (SQLException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}

		return 0;
	}

	@Override
	public void update(FhirOmopVocabularyMapEntry conceptMapEntry) {
		String sql = "UPDATE FhirOmopVocabularyMap SET fhir_url_system_name=?, other_system_name=? where omop_concept_code_name=?";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, conceptMapEntry.getFhirUrlSystemName());
			pstmt.setString(2, conceptMapEntry.getOtherSystemName());
			pstmt.setString(3, conceptMapEntry.getOmopConceptCodeName());
			pstmt.executeUpdate();
			logger.info("Map entry data (" + conceptMapEntry.getOmopConceptCodeName() + ") updated to ("
					+ conceptMapEntry.getFhirUrlSystemName() + ", " + conceptMapEntry.getOtherSystemName() + ")");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	@Override
	public void delete(String omopConceptCodeName) {
		String sql = "DELETE FROM FhirOmopVocabularyMap where omop_concept_code_name = ?";
		
		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, omopConceptCodeName);
			pstmt.executeUpdate();
			logger.info("filter data ("+omopConceptCodeName+") deleted");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}		
	}

	@Override
	public List<FhirOmopVocabularyMapEntry> get() {
		List<FhirOmopVocabularyMapEntry> conceptMapEntryList = new ArrayList<FhirOmopVocabularyMapEntry>();
		
		String sql = "SELECT * FROM FhirOmopVocabularyMap";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				FhirOmopVocabularyMapEntry conceptMapEntry = new FhirOmopVocabularyMapEntry();
				conceptMapEntry.setOmopConceptCodeName(rs.getString("omop_concept_code_name"));
				conceptMapEntry.setFhirUrlSystemName(rs.getString("fhir_url_system_name"));
				conceptMapEntry.setOtherSystemName(rs.getString("other_system_name"));
				conceptMapEntryList.add(conceptMapEntry);
			}
			logger.info(conceptMapEntryList.size()+" Concept Map entries obtained");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}		

		return conceptMapEntryList;
	}

	@Override
	public String getOmopVocabularyFromFhirSystemName(String fhirSystemName) {
		String retv = new String();
		String sql = "SELECT * FROM FhirOmopVocabularyMap where fhir_url_system_name=? or other_system_name=?";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, fhirSystemName);
			pstmt.setString(2, fhirSystemName);
			
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				retv = rs.getString("omop_concept_code_name");
			}
			logger.debug("Omop Vocabulary,"+retv+" , found for "+fhirSystemName);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		
		return retv;
	}

	@Override
	public String getFhirSystemNameFromOmopVocabulary(String omopVocabulary) {
		String retv = new String();
		String sql = "SELECT * FROM FhirOmopVocabularyMap where omop_concept_code_name=?";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, omopVocabulary);
			
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				retv = rs.getString("omop_concept_code_name");
			}
			logger.debug("FHIR System name,"+retv+" , found for "+omopVocabulary);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		
		return retv;
	}

}
