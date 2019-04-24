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

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Device;
import org.hl7.fhir.dstu3.model.Device.DeviceUdiComponent;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.exceptions.FHIRException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.stu3.model.MyDevice;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.DeviceResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.PatientResourceProvider;
import edu.gatech.chai.omopv5.dba.service.DeviceExposureService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.DeviceExposure;

public class OmopDevice extends BaseOmopResource<Device, DeviceExposure, DeviceExposureService>
		implements IResourceMapping<Device, DeviceExposure> {

	private static final Logger logger = LoggerFactory.getLogger(OmopDevice.class);
	private static OmopDevice omopDevice = new OmopDevice();
	
//	private ConceptService conceptService;

	public OmopDevice(WebApplicationContext context) {
		super(context, DeviceExposure.class, DeviceExposureService.class, DeviceResourceProvider.getType());
		initialize(context);
	}
	
	public OmopDevice() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), DeviceExposure.class, DeviceExposureService.class, DeviceResourceProvider.getType());
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}
	
	private void initialize(WebApplicationContext context) {
//		conceptService = context.getBean(ConceptService.class);
	}

	public static OmopDevice getInstance() {
		return omopDevice;
	}
	
	@Override
	public MyDevice constructFHIR(Long fhirId, DeviceExposure entity) {
		MyDevice device = new MyDevice();
		device.setId(new IdType(fhirId));
		
		// Set patient information.
		Reference patientReference = new Reference(new IdType("Patient", entity.getFPerson().getId()));
		String singleName = entity.getFPerson().getNameAsSingleString();
		if (singleName != null && !singleName.isEmpty()) {
			patientReference.setDisplay(singleName);
		}
		device.setPatient(patientReference);
		
		// Set device type, which is DeviceExposure concept.
		Concept entityConcept = entity.getDeviceConcept();
		String systemUri = new String();
		try {
			systemUri = OmopCodeableConceptMapping.fhirUriforOmopVocabulary(entityConcept.getVocabulary().getId());
		} catch (FHIRException e) {
			e.printStackTrace();
		}
		
		String code = entityConcept.getConceptCode();
		String dispaly = entityConcept.getName();
		
		Coding typeCoding = new Coding();
		typeCoding.setSystem(systemUri);
		typeCoding.setCode(code);
		typeCoding.setDisplay(dispaly);
		
		CodeableConcept typeCodeableConcept = new CodeableConcept();
		typeCodeableConcept.addCoding(typeCoding);
		
		// if deviceSourceValue is not empty, then add it here. 
		String deviceSourceValue = entity.getDeviceSourceValue();
		if (deviceSourceValue != null) {
			String[] sources = deviceSourceValue.split(":");
			Coding extraCoding = new Coding();
			if (sources.length != 3) {
				// just put this in the text field
				extraCoding.setDisplay(deviceSourceValue);
			} else {
				// First one is system name. See if this is FHIR URI
				if (sources[0].startsWith("http://") || sources[0].startsWith("urn:oid")) {
					if (!systemUri.equals(sources[0]) || !code.equals(sources[1])) {
						extraCoding.setSystem(sources[0]);
						extraCoding.setCode(sources[1]);
						extraCoding.setDisplay(sources[2]);
					}
				} else {
					// See if we can map from our static list.
					String fhirCodingSystem = "None";
					try {
						fhirCodingSystem = OmopCodeableConceptMapping.fhirUriforOmopVocabulary(sources[0]);
					} catch (FHIRException e) {
						e.printStackTrace();
						fhirCodingSystem = "None";
					}
					if ("None".equals(fhirCodingSystem)) {
						extraCoding.setSystem(sources[0]);
						extraCoding.setCode(sources[1]);
						extraCoding.setDisplay(sources[2]);
						extraCoding.setUserSelected(true);
					} else {
						if (!systemUri.equals(fhirCodingSystem) || !code.equals(sources[1])) {
							extraCoding.setSystem(fhirCodingSystem);
							extraCoding.setCode(sources[1]);
							extraCoding.setDisplay(sources[2]);
						}
					}
				}
			}
			
			if (!extraCoding.isEmpty()) {
				typeCodeableConcept.addCoding(extraCoding);
			}
		}
		
		// set device type concept
		device.setType(typeCodeableConcept);
		
		// set udi.deviceidentifier if udi is available.
		String udi = entity.getUniqueDeviceId();
		
		if (udi != null && !udi.isEmpty()) {
			DeviceUdiComponent deviceUdiComponent = new DeviceUdiComponent();
			deviceUdiComponent.setDeviceIdentifier(udi);
			device.setUdi(deviceUdiComponent);
		}
		
		return device;
	}

	@Override
	public Long toDbase(Device fhirResource, IdType fhirId) throws FHIRException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
		if (or) paramWrapper.setUpperRelationship("or");
        else paramWrapper.setUpperRelationship("and");

		switch (parameter) {
		case Device.SP_RES_ID:
			String encounterId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(encounterId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Device.SP_TYPE:
			String system = ((TokenParam) value).getSystem();
			String code = ((TokenParam) value).getValue();
			String omopVocabulary = null;
			if (system != null && !system.isEmpty()) {
				try {
					omopVocabulary = OmopCodeableConceptMapping.omopVocabularyforFhirUri(system);
				} catch (FHIRException e) {
					e.printStackTrace();
					break;
				}
			} else {
				omopVocabulary = "None";
			}

			if (system == null || system.isEmpty()) {
				if (code == null || code.isEmpty()) {
					// nothing to do
					break;
				} else {
					// no system but code.
					paramWrapper.setParameterType("String");
					paramWrapper.setParameters(Arrays.asList("deviceConcept.conceptCode"));
					paramWrapper.setOperators(Arrays.asList("like"));
					paramWrapper.setValues(Arrays.asList(code));
					paramWrapper.setRelationship("or");
					mapList.add(paramWrapper);
				}
			} else {
				if (code == null || code.isEmpty()) {
					// yes system but no code.
					paramWrapper.setParameterType("String");
					paramWrapper.setParameters(Arrays.asList("deviceConcept.vocabulary.id"));
					paramWrapper.setOperators(Arrays.asList("like"));
					paramWrapper.setValues(Arrays.asList(omopVocabulary));
					paramWrapper.setRelationship("or");
					mapList.add(paramWrapper);
				} else {
					// We have both system and code.
					paramWrapper.setParameterType("String");
					paramWrapper.setParameters(
							Arrays.asList("deviceConcept.vocabulary.id", "deviceConcept.conceptCode"));
					paramWrapper.setOperators(Arrays.asList("like", "like"));
					paramWrapper.setValues(Arrays.asList(omopVocabulary, code));
					paramWrapper.setRelationship("and");
					mapList.add(paramWrapper);
				}
			}

			break;
		case "Patient:" + Patient.SP_RES_ID:
			addParamlistForPatientIDName(parameter, (String)value, paramWrapper, mapList);
//			String pId = (String) value;
//			paramWrapper.setParameterType("Long");
//			paramWrapper.setParameters(Arrays.asList("fPerson.id"));
//			paramWrapper.setOperators(Arrays.asList("="));
//			paramWrapper.setValues(Arrays.asList(pId));
//			paramWrapper.setRelationship("or");
//			mapList.add(paramWrapper);
			break;
		case "Patient:" + Patient.SP_NAME:
			addParamlistForPatientIDName(parameter, (String)value, paramWrapper, mapList);
//			String patientName = ((String) value).replace("\"", "");
//			paramWrapper.setParameterType("String");
//			paramWrapper.setParameters(Arrays.asList("fPerson.familyName", "fPerson.givenName1", "fPerson.givenName2",
//					"fPerson.prefixName", "fPerson.suffixName"));
//			paramWrapper.setOperators(Arrays.asList("like", "like", "like", "like", "like"));
//			paramWrapper.setValues(Arrays.asList("%" + patientName + "%"));
//			paramWrapper.setRelationship("or");
//			mapList.add(paramWrapper);
			break;
		default:
			mapList = null;
		}
		
		return mapList;
	}

	@Override
	public DeviceExposure constructOmop(Long omopId, Device fhirResource) {
		// TODO Auto-generated method stub
		return null;
	}
}
