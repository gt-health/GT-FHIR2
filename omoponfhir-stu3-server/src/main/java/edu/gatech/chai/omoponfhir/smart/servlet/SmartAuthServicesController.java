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
package edu.gatech.chai.omoponfhir.smart.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.Key;
import java.sql.Timestamp;
import java.util.Date;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
//import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import edu.gatech.chai.smart.jpa.service.SmartOnFhirLaunchContextService;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * auth/ implementation for SMART on FHIR support for authentication
 * 
 */

@Controller
public class SmartAuthServicesController {
	private static final long serialVersionUID = 1L;
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SmartAuthServicesController.class);

	private WebApplicationContext myAppCtx;

	private String client_id;
	private String client_secret;
	private SmartOnFhirLaunchContextService mySmartOnFhirContextService;
	private String jwtSecret;
	private String smartStyleUrl;
	private boolean simEhr;

	public SmartAuthServicesController() {
		super();

		client_id = System.getenv("SMART_CLIENTID");
		client_secret = System.getenv("SMART_CLIENTSECRET");

		if (client_id == null)
			client_id = "client_id";
		if (client_secret == null)
			client_secret = "client_secret";

		jwtSecret = System.getenv("JWT_SECRET");
		if (jwtSecret == null) {
			jwtSecret = "thisismysecret";
		}

		smartStyleUrl = System.getenv("SMART_STYLE_URL");
		if (smartStyleUrl == null) {
			smartStyleUrl = "http://localhost/smart-style.json";
		}

		simEhr = false;

		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
	}

	private String generateJWT(JSONObject paramMap) {
		SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

		long nowMillis = System.currentTimeMillis();
		Date now = new Date(nowMillis);
		Date expiration = new Date(nowMillis + 300000); // 5m later

		byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(jwtSecret);
		Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

		JSONObject payload = new JSONObject();
		JSONObject context = new JSONObject();
		context.put("need_patient_banner", !simEhr);
		context.put("smart_style_url", smartStyleUrl);
		if (paramMap.has("patient_id")) {
			context.put("patient", paramMap.getString("patient_id"));
		}

		payload.put("context", context);
		payload.put("client_id", paramMap.getString("client_id"));
		payload.put("scope", paramMap.getString("scope"));
		payload.put("iat", now.getTime() / 1000);
		payload.put("exp", expiration.getTime() / 1000);

		JwtBuilder jwtBuilder = Jwts.builder().setHeaderParam(Header.TYPE, Header.JWT_TYPE).setPayload(payload.toString()).signWith(signatureAlgorithm, signingKey);
		return jwtBuilder.compact();
	}

	@GetMapping(value = "/authorize")
	public String authorize(@RequestParam(name = "launch", required = false) String launchContext,
			@RequestParam(name = "response_type", required = false) String responseType,
			@RequestParam(name = "client_id", required = false) String clientId,
			@RequestParam(name = "redirect_uri", required = true) String redirectUri,
			@RequestParam(name = "scope", required = false) String scope,
			@RequestParam(name = "aud", required = false) String aud,
			@RequestParam(name = "state", required = false) String state, Model model) {

		// We MUST have redirect URL.
		if (redirectUri == null || redirectUri.isEmpty()) {
			model.addAttribute("error", "redicrect_uri cannot be null or empty.");
			return "error";
		}

		// JSONize all parameters for possible next redirection.
		JSONObject paramMap = new JSONObject();
		paramMap.put("launch", launchContext);
		paramMap.put("response_type", responseType);
		paramMap.put("client_id", clientId);
		paramMap.put("redirect_uri", redirectUri);
		paramMap.put("scope", scope);
		paramMap.put("aud", aud);
		paramMap.put("state", state);

		model.addAttribute("request_params", paramMap);

		// We should use the client Id and check the scope to decide if
		// we want to authenticate this app request.
		// But, for now, we authenticate this.
		// TODO: connect to Authenticate server to authenticate the app.
		// One idea is to use Google or Facebook.
		//
		// For now, we expect the public app. client_id is required.
		// http://www.hl7.org/fhir/smart-app-launch/#step-3-app-exchanges-authorization-code-for-access-token

		// scope check. We need to make sure if this app has these scopes registered.
		// But, for this simulation, we allow everything.
		// If scope has launch, it should have launchContext as it's launched from EHR
		// If scope has launch/patient, then we need to run patient browser
		// If scope has launch/encounter, then we need to run encounter browser
		if (scope == null || scope.isEmpty()) {
			return "redirect:" + redirectUri + "?error=" + SmartLauncherCodec.simErrors.get(2)
					+ "&error_description=Simulated Auth Invalid Scope&state=" + state;
		}

		// Check scope parameter.
		// For our authorization, we accept ALL SCOPE. We are just checking
		// scope setting.
		String[] scopes = scope.split(" ");
		boolean launcedInEhr = false;
		boolean needToChoosePatient = false;
		boolean needToChooseEncounter = false;
		for (String s : scopes) {
			if ("launch".equals(s)) {
				launcedInEhr = true;
				if (launchContext == null || launchContext.isEmpty()) {
					// this has launch in scope. But, launchContext is null or empty
					return "redirect:" + redirectUri + "?error=auth_invalid_scope"
							+ "&error_description=Simulated Error for auth_invalid_scope&state=" + state;
				}
			}
			if ("launch/patient".equals(s)) {
				needToChoosePatient = true;
				paramMap.put("need_to_choose_patient", needToChoosePatient);
			}
			if ("launch/encounter".equals(s)) {
				needToChooseEncounter = true;
				paramMap.put("need_to_choose_encounter", needToChooseEncounter);
			}
		}

		if (!"code".equals(responseType)) {
			model.addAttribute("error", "response_type MUST be code.");
			return "error";
		}

		// The launchContext, if exists, contains a context to resolve this to
		// patient, encounter, provider, etc. We however do not have EHR that can
		// store the information as this is initiated from smart-launcher.
		// The context itself has those information encoded. Decode it now.
		String code = null;
		String patientId = null;
		if (launchContext != null && !launchContext.isEmpty()) {
			String launchCode = new String(Base64.decodeBase64(launchContext));
			logger.debug("Launch Code:" + launchCode);

			// decode the code.
			JSONObject codeJson = new JSONObject(launchCode);
			JSONObject decodedCode = SmartLauncherCodec.decode(codeJson);
			if ("1".equals(decodedCode.getString("launch_ehr"))) {
				// We are launching in EHR mode.
				// Do something here if you need to do anything
			}

			if ("1".equals(decodedCode.getString("sim_ehr"))) {
				simEhr = true;

				// We are simulating EHR.
				if (decodedCode.has("auth_error")) {
					// Auth error simulation is requested.
					// Return error as requested.
					String authError = decodedCode.getString("auth_error");
					String authErrorDesc = SmartLauncherCodec.getSimErrorDesc(authError);
					return "redirect:" + redirectUri + "?error=" + authError + "&error_description=" + authErrorDesc
							+ "&state=" + state;
				}
			}

			patientId = decodedCode.getString("patient");
			paramMap.put("patient_id", patientId);
			
			code = generateJWT(paramMap);
		}

		if (needToChoosePatient) {
			// TODO: Handle patient choose
		}
		if (needToChooseEncounter) {
			// TODO: Handle Encounter choose
		}

//		java.util.Date date = new java.util.Date();
//		Timestamp ts = new Timestamp(date.getTime());

		if (code != null && !code.isEmpty()) {
			return "redirect:" + redirectUri + "?code=" + code + "&state=" + state;
		}

		return "index";
	}
}
