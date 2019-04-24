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
package edu.gatech.chai.omoponfhir.omopv5.stu3.utilities;

import java.util.List;

import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.StringType;

import edu.gatech.chai.omopv5.dba.service.LocationService;
import edu.gatech.chai.omopv5.model.entity.Location;

public class AddressUtil {
	/**
	 * 
	 * @param locationService : Class to JPA service
	 * @param address : FHIR address data
	 * @param location : Location entity class in OMOP
	 * @return : Location class found. Null if not found
	 */
	public static Location searchAndUpdate(LocationService locationService, Address address, Location location) {
		if (address == null)
			return null;

		List<StringType> addressLines = address.getLine();
		String line1 = null;
		String line2 = null;
		if (addressLines.size() > 0) {
			line1 = addressLines.get(0).getValue();
			if (address.getLine().size() > 1) {
				line2 = address.getLine().get(1).getValue();
			}
		}
		String zipCode = address.getPostalCode();
		String city = address.getCity();
		String state = address.getState();

		Location existingLocation = locationService.searchByAddress(line1, line2, city, state, zipCode);
		if (existingLocation != null) {
			return existingLocation;
		} else {
			// We will return new Location. But, if Location is provided,
			// then we update the parameters here.
			if (location != null) {
				location.setAddress1(line1);
				if (line2 != null)
					location.setAddress2(line2);
				location.setZipCode(zipCode);
				location.setCity(city);
				location.setState(state);
			} else {
				Location newLocation = locationService.create(new Location(line1, line2, city, state, zipCode));
				return newLocation;
			}
		}

		return null;
	}
}
