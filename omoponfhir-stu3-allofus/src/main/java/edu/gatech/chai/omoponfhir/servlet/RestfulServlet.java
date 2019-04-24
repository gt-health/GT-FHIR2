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
package edu.gatech.chai.omoponfhir.servlet;

import java.util.*;

import edu.gatech.chai.omoponfhir.security.OIDCInterceptor;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.*;
import edu.gatech.chai.omoponfhir.stu3.security.SMARTonFHIRConformanceStatement;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.narrative.INarrativeGenerator;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.FifoMemoryPagingProvider;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.IServerAddressStrategy;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;

/**
 * This servlet is the actual FHIR server itself
 */
public class RestfulServlet extends RestfulServer {

	private static final long serialVersionUID = 1L;
	private WebApplicationContext myAppCtx;

	/**
	 * Constructor
	 */
	public RestfulServlet() {
		super(FhirContext.forDstu3());
	}

	/**
	 * This method is called automatically when the servlet is initializing.
	 */
	@Override
	public void initialize() {
		// Set server name
		setServerName("GT-FHIR2 for OMOPv5");
		
		
		// If we have system environment variable to hardcode the base URL, do it now.
		String serverBaseUrl = System.getenv("SERVERBASE_URL");
		if (serverBaseUrl != null && !serverBaseUrl.isEmpty() && !serverBaseUrl.trim().equalsIgnoreCase("")) {
			serverBaseUrl = serverBaseUrl.trim();
			if (!serverBaseUrl.startsWith("http://") && !serverBaseUrl.startsWith("https://")) {
				serverBaseUrl = "https://"+serverBaseUrl;
			}
			
			if (serverBaseUrl.endsWith("/")) {
				serverBaseUrl = serverBaseUrl.substring(0, serverBaseUrl.length()-1);
			}

			IServerAddressStrategy serverAddressStrategy = new HardcodedServerAddressStrategy(serverBaseUrl);
			setServerAddressStrategy(serverAddressStrategy);
		}

		/*
		 * Set non resource provider.
		 */
		List<Object> plainProviders = new ArrayList<Object>();
		SystemTransactionProvider systemTransactionProvider = new SystemTransactionProvider();
		ServerOperations serverOperations = new ServerOperations();

		/*
		 * Define resource providers 
		 */
		List<IResourceProvider> providers = new ArrayList<IResourceProvider>();

		ConditionResourceProvider conditionResourceProvider = new ConditionResourceProvider();
		providers.add(conditionResourceProvider);
		
		EncounterResourceProvider encounterResourceProvider = new EncounterResourceProvider(); 
		providers.add(encounterResourceProvider);
		
		MedicationResourceProvider medicationResourceProvider = new MedicationResourceProvider(); 
		providers.add(medicationResourceProvider);
		
		MedicationStatementResourceProvider medicationStatementResourceProvider = new MedicationStatementResourceProvider();
		providers.add(medicationStatementResourceProvider);
		
		MedicationRequestResourceProvider medicationRequestResourceProvider = new MedicationRequestResourceProvider();
		providers.add(medicationRequestResourceProvider);
		
		ObservationResourceProvider observationResourceProvider = new ObservationResourceProvider();
		providers.add(observationResourceProvider);
		
		OrganizationResourceProvider organizationResourceProvider = new OrganizationResourceProvider();
		providers.add(organizationResourceProvider);
		
		PractitionerResourceProvider practitionerResourceProvider = new PractitionerResourceProvider();
		providers.add(practitionerResourceProvider);
		
		PatientResourceProvider patientResourceProvider = new PatientResourceProvider();
		providers.add(patientResourceProvider);
		
		ProcedureResourceProvider procedureResourceProvider = new ProcedureResourceProvider();
		providers.add(procedureResourceProvider);
		
		DeviceResourceProvider deviceResourceProvider = new DeviceResourceProvider();
		providers.add(deviceResourceProvider);

		DeviceUseStatementResourceProvider deviceUseStatementResourceProvider = new DeviceUseStatementResourceProvider();
		providers.add(deviceUseStatementResourceProvider);
		
		DocumentReferenceResourceProvider documentReferenceResourceProvider = new DocumentReferenceResourceProvider();
		providers.add(documentReferenceResourceProvider);		
		
		setResourceProviders(providers);

		/*
		 * add system transaction provider to the plain provider.
		 */
		plainProviders.add(systemTransactionProvider);
		plainProviders.add(serverOperations);
		
		setPlainProviders(plainProviders);
		
		/*
		 * Set conformance provider
		 */
		SMARTonFHIRConformanceStatement capbilityProvider = new SMARTonFHIRConformanceStatement(this);
		capbilityProvider.setPublisher("Georgia Tech - I3L");
		capbilityProvider.setAuthServerUrl(getServletConfig().getInitParameter("authServerUrl"));
		setServerConformanceProvider(capbilityProvider);
		
		/*
		 * Add page provider. Use memory based on for now.
		 */
		FifoMemoryPagingProvider pp = new FifoMemoryPagingProvider(5);
		pp.setDefaultPageSize(50);
		pp.setMaximumPageSize(100000);
		setPagingProvider(pp);

		/*
		 * Use a narrative generator. This is a completely optional step, but
		 * can be useful as it causes HAPI to generate narratives for resources
		 * which don't otherwise have one.
		 */
		INarrativeGenerator narrativeGen = new DefaultThymeleafNarrativeGenerator();
		getFhirContext().setNarrativeGenerator(narrativeGen);

		/*
		 * Enable CORS
		 */
		CorsConfiguration config = new CorsConfiguration();
		CorsInterceptor corsInterceptor = new CorsInterceptor(config);
		config.addAllowedHeader("Accept");
		config.addAllowedHeader("Content-Type");
		config.addAllowedOrigin("*");
		config.addExposedHeader("Location");
		config.addExposedHeader("Content-Location");
		config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		registerInterceptor(corsInterceptor);

		/*
		 * This server interceptor causes the server to return nicely formatter
		 * and coloured responses instead of plain JSON/XML if the request is
		 * coming from a browser window. It is optional, but can be nice for
		 * testing.
		 */
		registerInterceptor(new ResponseHighlighterInterceptor());

		/*
		 * OpenID check interceptor to support SMART on FHIR
		 */
		OIDCInterceptor oIDCInterceptor = new OIDCInterceptor();
		oIDCInterceptor.setIntrospectUrl(getServletConfig().getInitParameter("introspectUrl"));
		oIDCInterceptor.setEnableOAuth(getServletConfig().getInitParameter("enableOAuth"));
		oIDCInterceptor.setClientId(getServletConfig().getInitParameter("clientId"));
		oIDCInterceptor.setClientSecret(getServletConfig().getInitParameter("clientSecret"));
		oIDCInterceptor.setLocalByPass(getServletConfig().getInitParameter("localByPass"));
		oIDCInterceptor.setReadOnly(getServletConfig().getInitParameter("readOnly"));
		registerInterceptor(oIDCInterceptor);
		
		/*
		 * Tells the server to return pretty-printed responses by default
		 */
		setDefaultPrettyPrint(true);

		/*
		 * Set response encoding.
		 */
		setDefaultResponseEncoding(EncodingEnum.JSON);
		
	}

}
