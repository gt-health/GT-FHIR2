package edu.gatech.chai.omoponfhir.omopv5.stu3.provider;

import java.util.List;
import java.util.Map;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ConceptMap;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.UriType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.IResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.stu3.mapping.OmopConceptMap;

public class ConceptMapResourceProvider implements IResourceProvider {
	private static final Logger logger = LoggerFactory.getLogger(ConceptMapResourceProvider.class);

	private WebApplicationContext myAppCtx;
	private String myDbType;
	private OmopConceptMap myMapper;
	private int preferredPageSize = 30;
	private FhirContext fhirContext;

	public ConceptMapResourceProvider() {
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
		myDbType = myAppCtx.getServletContext().getInitParameter("backendDbType");
		if (myDbType.equalsIgnoreCase("omopv5") == true) {
			myMapper = new OmopConceptMap(myAppCtx);
		} else {
			myMapper = new OmopConceptMap(myAppCtx);
		}

		String pageSizeStr = myAppCtx.getServletContext().getInitParameter("preferredPageSize");
		if (pageSizeStr != null && pageSizeStr.isEmpty() == false) {
			int pageSize = Integer.parseInt(pageSizeStr);
			if (pageSize > 0) {
				preferredPageSize = pageSize;
			}
		}
	}

	@Override
	public Class<ConceptMap> getResourceType() {
		return ConceptMap.class;
	}

	public static String getType() {
		return "ConceptMap";
	}

	public void setFhirContext(FhirContext fhirContext) {
		this.fhirContext = fhirContext;
	}
	
	/**
	 * $translate operation for concept translation.
	 * 
	 */
	@Operation(name = "$translate", idempotent = true)
	public Parameters translateOperation(RequestDetails theRequestDetails, 
			@OperationParam(name = "code") CodeType theCode, 
			@OperationParam(name = "system") UriType theSystem,
			@OperationParam(name = "version") StringType theVersion, 
			@OperationParam(name = "source") UriType theSource,
			@OperationParam(name = "coding") Coding theCoding,
			@OperationParam(name = "codeableConcept") CodeableConcept theCodeableConcept,
			@OperationParam(name = "target") UriType theTarget,
			@OperationParam(name = "targetsystem") UriType theTargetSystem,
			@OperationParam(name = "reverse") BooleanType theReverse) {

		Parameters retVal = new Parameters();

		String mappingTerminologyUrl = System.getenv("MAPPING_TERMINOLOGY_URL");
		if (mappingTerminologyUrl != null && !mappingTerminologyUrl.isEmpty()) {
			String mappingRequestUrl = theRequestDetails.getCompleteUrl();
			if (mappingRequestUrl != null && !mappingRequestUrl.isEmpty()) {
				logger.debug("$translate: RequestDetails - "+theRequestDetails.getCompleteUrl());
				
				if (!mappingTerminologyUrl.endsWith("/")) {
					mappingTerminologyUrl = mappingTerminologyUrl.concat("/");
				}

				int urlTranslateIndex = mappingRequestUrl.indexOf("$translate");
				String remoteMappingTerminologyUrl = mappingTerminologyUrl+mappingRequestUrl.substring(urlTranslateIndex);

				RestTemplate restTemplate = new RestTemplate();
				ResponseEntity<String> response = restTemplate.getForEntity(remoteMappingTerminologyUrl, String.class);
				if (response.getStatusCode().equals(HttpStatus.OK)) {
					String result = response.getBody();
					IParser fhirJsonParser = fhirContext.newJsonParser();
					Parameters parameters = fhirJsonParser.parseResource(Parameters.class, result);
					if (parameters != null && !parameters.isEmpty()) {
						return parameters;
					}
					
					// We got nothing. Just let it flow so we can use our internal one
				}
			}
		}
		
		String targetUri;
		String targetSystem;
		if (theTarget == null || theTarget.isEmpty()) {
			targetUri = "";
		} else {
			targetUri = theTarget.getValueAsString();
		}
		if (theTargetSystem == null || theTargetSystem.isEmpty()) {
			targetSystem = "";
		} else {
			targetSystem = theTargetSystem.getValueAsString();
		}

		if (theCodeableConcept != null && !theCodeableConcept.isEmpty()) {
			// If codeableconcept exists, use this.
			// If we have multiple codings, run them until we have a matching
			// translation.
			List<Coding> codings = theCodeableConcept.getCoding();
			for (Coding coding : codings) {
				String code = coding.getCode();
				String system = coding.getSystem();

				retVal = myMapper.translateConcept(code, system, targetUri, targetSystem);
				if (retVal != null && !retVal.isEmpty()) {
					return retVal;
				}
			}
		}

		if (theCoding != null && !theCoding.isEmpty()) {
			// if coding is provided, use this.
			String code = theCoding.getCode();
			String system = theCoding.getSystem();

			retVal = myMapper.translateConcept(code, system, targetUri, targetSystem);
			if (retVal != null && !retVal.isEmpty()) {
				return retVal;
			}
		}

		if (theCode != null && !theCode.isEmpty() && theSystem != null && !theSystem.isEmpty()) {
			String code = theCode.getValueAsString();
			String system = theSystem.getValueAsString();
			retVal = myMapper.translateConcept(code, system, targetUri, targetSystem);
			return retVal;
		}

		return retVal;
	}
}
