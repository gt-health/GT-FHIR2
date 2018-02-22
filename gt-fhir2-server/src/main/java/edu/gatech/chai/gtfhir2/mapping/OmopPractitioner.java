package edu.gatech.chai.gtfhir2.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Address.AddressUse;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.exceptions.FHIRException;

import edu.gatech.chai.omopv5.jpa.entity.CareSite;
import edu.gatech.chai.omopv5.jpa.entity.Concept;
import edu.gatech.chai.omopv5.jpa.entity.Location;
import edu.gatech.chai.omopv5.jpa.entity.Provider;
import edu.gatech.chai.omopv5.jpa.service.CareSiteService;
import edu.gatech.chai.omopv5.jpa.service.LocationService;
import edu.gatech.chai.omopv5.jpa.service.ParameterWrapper;
import edu.gatech.chai.omopv5.jpa.service.ProviderService;

public class OmopPractitioner implements ResourceMapping<Practitioner>{

	private CareSiteService careSiteService;
	private ProviderService providerService;
	private LocationService locationService;
	
	/**
	 * Omop on FHIR mapping - from OMOP to FHIR.
	 * 
	 * @param practitioner ID
	 * 	The Practitioner Resource ID in IdType variable type.
	 * 
	 * @return Practitioner
	 * 	Returns Practitioner Resource mapped from OMOP Provider table.
	 */
	@Override
	public Practitioner toFHIR(IdType id) {
		String practitioncerResourceName = ResourceType.Practitioner.getPath();
		Long id_long_part = id.getIdPartAsLong();
		Long omopId = IdMapping.getOMOPfromFHIR(id_long_part, practitioncerResourceName);
		
		Provider omopProvider = providerService.findById(omopId);
		if(omopProvider == null) return null;
		
		Long fhirId = IdMapping.getFHIRfromOMOP(id_long_part, Provider.RES_TYPE);
		
		return constructPractitioner(fhirId, omopProvider);
	}
	
	public Practitioner constructPractitioner(Long fhirId, Provider omopProvider) {
		Practitioner practitioner = new Practitioner(); //Assuming default active state
		practitioner.setId(new IdType(fhirId));
		
		CareSite omopCareSite = omopProvider.getCareSite();
		
		if(omopProvider.getProviderName() != null && !omopProvider.getProviderName().isEmpty()) {
			HumanName fhirName = new HumanName();
			fhirName.setText(omopProvider.getProviderName());
			List<HumanName> fhirNameList = new ArrayList<HumanName>();
			fhirNameList.add(fhirName);
			practitioner.setName(fhirNameList);
		}
		
		//TODO: Need practictioner telecom information
		//Set address
		if(omopCareSite.getLocation() != null && omopCareSite.getLocation().getId() != 0L) {
			practitioner.addAddress()
			.setUse(AddressUse.WORK)
			.addLine(omopCareSite.getLocation().getAddress1())
			.addLine(omopCareSite.getLocation().getAddress2())//WARNING check if mapping for lines are correct
			.setCity(omopCareSite.getLocation().getCity())
			.setPostalCode(omopCareSite.getLocation().getZipCode())
			.setState(omopCareSite.getLocation().getState());
		}
		//Set gender
		if (omopProvider.getGenderConcept() != null) {
			String gName = omopProvider.getGenderConcept().getName().toLowerCase(); 
			AdministrativeGender gender;
			try {
				gender = AdministrativeGender.fromCode(gName);
				practitioner.setGender(gender);
			} catch (FHIRException e) {
				e.printStackTrace();
			}
		}
		return practitioner;
	}

	@Override
	public Long toDbase(Practitioner Fhir) {
		Provider omopProvider = new Provider();
		CareSite omopCareSite = new CareSite();
		//Set name
		Iterator<HumanName> practitionerIterator = Fhir.getName().iterator();
		if(practitionerIterator.hasNext()) {
			HumanName next = practitionerIterator.next();
			omopProvider.setProviderName(next.getText());
		}
		//Set address
		List<Address> addresses = Fhir.getAddress();
		Location retLocation = null;
		if (addresses != null && addresses.size() > 0) {
			Address address = addresses.get(0);
			retLocation = searchAndUpdate(address, null);
			if (retLocation != null) {
				omopCareSite.setLocation(retLocation);
			}
		}
		//Set gender concept
		omopProvider.setGenderConcept(new Concept());
		String genderCode = Fhir.getGender().toCode();
		try {
			omopProvider.getGenderConcept().setId(OmopConceptMapping.omopForAdministrativeGenderCode(genderCode));
		} catch (FHIRException e) {
			e.printStackTrace();
		}
		if(!Fhir.getAddress().isEmpty()) {
			CareSite careSite = searchAndUpdateCareSite(Fhir.getAddress().get(0));
			careSiteService.createOrUpdate(careSite);
		}
		
		Long omopRecordId = providerService.createOrUpdate(omopProvider).getId();
		Long fhirId = IdMapping.getFHIRfromOMOP(omopRecordId, Provider.RES_TYPE);
		return null;
	}
	
	@Override
	public Long getSize() {
		return providerService.getSize();
	}

	public Long getSize(Map<String, List<ParameterWrapper>> map) {
		return providerService.getSize(map);
	}
	
	public Location searchAndUpdate (Address address, Location location) {
		if (address == null) return null;
		
		List<StringType> addressLines = address.getLine();
		if (addressLines.size() > 0) {
			String line1 = addressLines.get(0).getValue();
			String line2 = null;
			if (address.getLine().size() > 1)
				line2 = address.getLine().get(1).getValue();
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
					return new Location (line1, line2, city, state, zipCode);
				}
			}			
		}
		
		return null;
	}
	
	public CareSite searchAndUpdateCareSite(Address address) {
		Location location = searchAndUpdate(address, null);
		if(location == null) return null;
		CareSite careSite = careSiteService.searchByLocation(location);
		if(careSite != null) {
			return careSite;
		}
		else {
			careSite = new CareSite();
			careSite.setLocation(location);
			return careSite;
		}
	}
}
