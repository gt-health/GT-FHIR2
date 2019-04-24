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
package edu.gatech.chai.omoponfhir.omopv5.stu3.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Address.AddressUse;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.stu3.model.MyOrganization;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.OrganizationResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.AddressUtil;
import edu.gatech.chai.omopv5.dba.service.CareSiteService;
import edu.gatech.chai.omopv5.dba.service.LocationService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.model.entity.CareSite;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.Location;

public class OmopOrganization extends BaseOmopResource<Organization, CareSite, CareSiteService> implements IResourceMapping<Organization, CareSite> {
	
	private static OmopOrganization omopOrganization = new OmopOrganization();
	private LocationService locationService;

	public OmopOrganization(WebApplicationContext context) {
		super(context, CareSite.class, CareSiteService.class, OrganizationResourceProvider.getType());
		
	}

	public OmopOrganization() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), CareSite.class, CareSiteService.class, OrganizationResourceProvider.getType());
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}

	private void initialize(WebApplicationContext context) {		
		// Get bean for other service(s) for mapping.
		locationService = context.getBean(LocationService.class);
	}
	
	public static OmopOrganization getInstance() {
		return omopOrganization;
	}
	
	@Override
	public MyOrganization constructFHIR(Long fhirId, CareSite careSite) {
		MyOrganization organization = new MyOrganization();

		organization.setId(new IdType(fhirId));

		if (careSite.getCareSiteName() != null && careSite.getCareSiteName() != "") {
			organization.setName(careSite.getCareSiteName());
		}

		if (careSite.getPlaceOfServiceConcept() != null) {
			String codeString = careSite.getPlaceOfServiceConcept().getConceptCode();
			String systemUriString = careSite.getPlaceOfServiceConcept().getVocabulary().getVocabularyReference();
			String displayString = careSite.getPlaceOfServiceConcept().getName();

			CodeableConcept typeCodeableConcept = new CodeableConcept()
					.addCoding(new Coding(systemUriString, codeString, displayString));
			organization.addType(typeCodeableConcept);
		}

		if (careSite.getLocation() != null) {
			// WARNING check if mapping for lines are correct
			organization.addAddress().setUse(AddressUse.HOME).addLine(careSite.getLocation().getAddress1())
					.addLine(careSite.getLocation().getAddress2())
					.setCity(careSite.getLocation().getCity()).setPostalCode(careSite.getLocation().getZipCode())
					.setState(careSite.getLocation().getState());
			// .setPeriod(period);
		}

		// TODO: Static Extensions for sample. Remove this later.
		// Populate the first, primitive extension
		organization.setBillingCode(new CodeType("00102-1"));

		// The second extension is repeatable and takes a block type
		MyOrganization.EmergencyContact contact = new MyOrganization.EmergencyContact();
		contact.setActive(new BooleanType(true));
		contact.setContact(new ContactPoint());
		organization.getEmergencyContact().add(contact);

		return organization;
	}

	@Override
	public Long toDbase(Organization organization, IdType fhirId) throws FHIRException {
		// If fhirId is null, then it's CREATE.
		// If fhirId is not null, then it's UPDATE.

		Long omopId = null;
		if (fhirId != null) {
			omopId = IdMapping.getOMOPfromFHIR(fhirId.getIdPartAsLong(), OrganizationResourceProvider.getType());
		} else {
			// See if we have this already. If so, we throw error.
			// Get the identifier to store the source information.
			// If we found a matching one, replace this with the careSite.
			List<Identifier> identifiers = organization.getIdentifier();
			CareSite existingCareSite = null;
			String careSiteSourceValue = null;
			for (Identifier identifier: identifiers) {
				if (identifier.getValue().isEmpty() == false) {
					careSiteSourceValue = identifier.getValue();
					
					existingCareSite = getMyOmopService().searchByColumnString("careSiteSourceValue", careSiteSourceValue).get(0);
					if (existingCareSite != null) {
						omopId = existingCareSite.getId();
						break;
					}
				}
			}
		}

		CareSite careSite = constructOmop(omopId, organization);
		
		Long omopRecordId = null;
		if (careSite.getId() != null) {
			omopRecordId = getMyOmopService().update(careSite).getId();	
		} else {
			omopRecordId = getMyOmopService().create(careSite).getId();
		}
		
		Long fhirRecordId = IdMapping.getFHIRfromOMOP(omopRecordId, OrganizationResourceProvider.getType());
		return fhirRecordId;
	}

//	@Override
//	public Long getSize() {
//		return myOmopService.getSize(CareSite.class);
//	}
//	
//	public Long getSize(Map<String, List<ParameterWrapper>> map) {
//		return myOmopService.getSize(CareSite.class, map);
//	}
	

	@Override
	public Organization constructResource(Long fhirId, CareSite entity, List<String> includes) {
		MyOrganization myOrganization = constructFHIR(fhirId, entity);
		
		if (!includes.isEmpty()) {
			if (includes.contains("Organization:partof")) {
				Reference partOfOrganization = myOrganization.getPartOf();
				if (partOfOrganization != null && partOfOrganization.isEmpty() == false) {
					IIdType partOfOrgId = partOfOrganization.getReferenceElement();
					Long partOfOrgFhirId = partOfOrgId.getIdPartAsLong();
					Long omopId = IdMapping.getOMOPfromFHIR(partOfOrgFhirId, OrganizationResourceProvider.getType());
					CareSite partOfCareSite = getMyOmopService().findById(omopId);
					MyOrganization partOfOrgResource = constructFHIR(partOfOrgFhirId, partOfCareSite);
					
					partOfOrganization.setResource(partOfOrgResource);
				}
			}
		}

		return myOrganization;
	}

	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
        if (or) paramWrapper.setUpperRelationship("or");
        else paramWrapper.setUpperRelationship("and");

		switch (parameter) {
		case MyOrganization.SP_RES_ID:
			String orgnizationId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(orgnizationId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case MyOrganization.SP_NAME:
			// This is family name, which is string. use like.
			String familyString = ((StringParam) value).getValue();
			paramWrapper.setParameterType("String");
			paramWrapper.setParameters(Arrays.asList("careSiteName"));
			paramWrapper.setOperators(Arrays.asList("like"));
			paramWrapper.setValues(Arrays.asList(familyString));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		default:
			mapList = null;
		}

		return mapList;
	}

	@Override
	public CareSite constructOmop(Long omopId, Organization fhirResource) {
		String careSiteSourceValue = null;
		MyOrganization myOrganization = (MyOrganization) fhirResource;
		Location location = null;
		
		CareSite careSite = null;
		if (omopId != null) {
			careSite  = getMyOmopService().findById(omopId);
			if (careSite == null) {
				try {
					throw new FHIRException(myOrganization.getId() + " does not exist");
				} catch (FHIRException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			location = careSite.getLocation();
		} else {
			careSite = new CareSite();
		}
		
		Identifier identifier = myOrganization.getIdentifierFirstRep();
		if (!identifier.getValue().isEmpty()) {
			careSiteSourceValue = identifier.getValue();
			careSite.setCareSiteSourceValue(careSiteSourceValue);
		}
		
		Location existingLocation = AddressUtil.searchAndUpdate(locationService, myOrganization.getAddressFirstRep(), location);
		if (existingLocation != null) {
			careSite.setLocation(existingLocation);
		}

		// Organization.name to CareSiteName
		careSite.setCareSiteName(myOrganization.getName());

		// Organzation.type to Place of Service Concept
		List<CodeableConcept> orgTypes = myOrganization.getType();
		for (CodeableConcept orgType: orgTypes) {
			List<Coding> typeCodings = orgType.getCoding();
			if (typeCodings.size() > 0) {
				String typeCode = typeCodings.get(0).getCode();
				Long placeOfServiceId;
				try {
					placeOfServiceId = OmopConceptMapping.omopForOrganizationTypeCode(typeCode);
					Concept placeOfServiceConcept = new Concept();
					placeOfServiceConcept.setId(placeOfServiceId);
					careSite.setPlaceOfServiceConcept(placeOfServiceConcept);
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}
		}

		// Address to Location ID
		List<Address> addresses = myOrganization.getAddress();
		for (Address address: addresses) {
			// We can only store one address.
			Location retLocation = AddressUtil.searchAndUpdate(locationService, address, careSite.getLocation());
			if (retLocation != null) {
				careSite.setLocation(retLocation);
				break;
			}
		}

		return careSite;
	}
}
