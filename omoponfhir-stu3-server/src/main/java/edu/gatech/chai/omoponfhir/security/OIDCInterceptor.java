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

package edu.gatech.chai.omoponfhir.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
//import ca.uhn.fhir.rest.method.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;

/**
 * @author MC142
 *
 */

public class OIDCInterceptor extends InterceptorAdapter {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(OIDCInterceptor.class);

	private String authType;
	private String introspectUrl;	
	private String clientId;
	private String clientSecret;
//	private String localByPass;
	private String readOnly;

	public OIDCInterceptor() {
		authType = "None";  // Default allows anonymous access
	}

	@Override
	public boolean incomingRequestPostProcessed(RequestDetails theRequestDetails, HttpServletRequest theRequest,
			HttpServletResponse theResponse) throws AuthenticationException {

		ourLog.debug("[OAuth] Request from " + theRequest.getRemoteAddr());
		
		// check environment variables now as they may have been updated.
		String readOnlyEnv = System.getenv("FHIR_READONLY");
		if (readOnlyEnv != null && !readOnlyEnv.isEmpty()) {
			setReadOnly(readOnlyEnv);
		}
		
		String authTypeEnv = System.getenv("AUTH_TYPE");
		if (authTypeEnv != null && !authTypeEnv.isEmpty()) {
			setAuthType(authTypeEnv);
		} else {
			setAuthType("None");
		}
		
//		String localByPassEnv = System.getenv("LOCAL_BYPASS");
//		if (localByPassEnv != null && !localByPassEnv.isEmpty()) {
//			setLocalByPass(localByPassEnv);
//		}
		
		if (readOnly.equalsIgnoreCase("True")) {
			if (theRequest.getMethod().equalsIgnoreCase("GET")) {
				return true;
			} else {
				RequestTypeEnum[] allowedMethod = new RequestTypeEnum[] {RequestTypeEnum.GET};
				throw new MethodNotAllowedException("Server Running in Read Only", allowedMethod);
//				return false;
			}
		}
		
		if (theRequestDetails.getRestOperationType() == RestOperationTypeEnum.METADATA) {
			ourLog.debug("This is METADATA request.");

			// Enumeration<String> headerNames = theRequest.getHeaderNames();
			// while (headerNames.hasMoreElements()) {
			// String headerName = headerNames.nextElement();
			// System.out.println(headerName);
			// Enumeration<String> headers = theRequest.getHeaders(headerName);
			// while (headers.hasMoreElements()) {
			// String headerValue = headers.nextElement();
			// System.out.println(" "+headerValue);
			// }
			// }

			// StringBuilder buffer = new StringBuilder();
			// BufferedReader reader;
			// try {
			// reader = theRequest.getReader();
			// String line;
			// while ((line=reader.readLine())!=null) {
			// buffer.append(line);
			// }
			//
			// System.out.println("METADATA request getbody:
			// "+buffer.toString());
			//
			// } catch (IOException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			//

			return true;
		}

//		// Quick Hack for request from localhost overlay site.
//		if (localByPass.equalsIgnoreCase("True")) {
//			ourLog.debug("remoteAddress:"+theRequest.getRemoteAddr()+", localAddress:"+theRequest.getLocalAddr());
//			if (theRequest.getRemoteAddr().equalsIgnoreCase("127.0.0.1")
//					|| theRequest.getRemoteAddr().equalsIgnoreCase("0:0:0:0:0:0:0:1")) {
//				return true;
//			}
//
//			if (theRequest.getLocalAddr().equalsIgnoreCase(theRequest.getRemoteAddr())) {
//				return true;
//			}
//			
//			// When this is deployed in docker container and/or with some proxies,
//			// we may set manual server address, which caused all traffics forwarded by proxy.
//			// If the match above fails, we may still be coming from local
//			String[] remoteAddrs = theRequest.getRemoteAddr().split("\\.");
//			String[] localAddrs = theRequest.getLocalAddr().split("\\.");
//			if (remoteAddrs.length == 4 && localAddrs.length == 4) {
//				ourLog.debug("remoteAddrs[0]="+remoteAddrs[0]+", remoteAddrs[1]="+remoteAddrs[1]+" , localAddrs[0]="+localAddrs[0]+", localAddrs[1]="+localAddrs[1]);
//				if (remoteAddrs[0].equals(localAddrs[0]) && remoteAddrs[1].equals(localAddrs[1])) {
//					return true;
//				}
//			}
//		}

		if (authType.equalsIgnoreCase("None")) {
			ourLog.debug("[OAuth] OAuth is disabled. Request from " + theRequest.getRemoteAddr() + "is approved");
			return true;
		} 
		
		if (authType.startsWith("Basic ")) {
			String[] basicCredential = authType.substring(6).split(":");
			if (basicCredential.length != 2) {
				
				AuthenticationException ex = new AuthenticationException("Basic Authorization Setup Incorrectly");
				ex.addAuthenticateHeaderForRealm("OmopOnFhir");
				throw ex;
			}
			String username = basicCredential[0];
			String password = basicCredential[1];

			String authHeader = theRequest.getHeader("Authorization");
			if (authHeader == null || authHeader.isEmpty() || authHeader.length() < 6) {
				AuthenticationException ex = new AuthenticationException("No or Invalid Basic Authorization Header");
				ex.addAuthenticateHeaderForRealm("OmopOnFhir");
				throw ex;
			}
			
			// Check if basic auth.
			String prefix = authHeader.substring(0, 6);
			if (!"basic ".equalsIgnoreCase(prefix)) {
				AuthenticationException ex = new AuthenticationException("Invalid Basic Authorization Header");
				ex.addAuthenticateHeaderForRealm("OmopOnFhir");
				throw ex;
			}
			
			String base64 = authHeader.substring(6);

			String base64decoded = new String(Base64.decodeBase64(base64));
			String[] parts = base64decoded.split(":");

			if (username.equals(parts[0]) && password.equals(parts[1])) {
				ourLog.debug("[Basic Auth] Auth is granted with " + username + " and "+ password);
				return true;
			}
			
			AuthenticationException ex = new AuthenticationException("Incorrect Username and Password");
			ex.addAuthenticateHeaderForRealm("OmopOnFhir");
			throw ex;
		} else {
			// checking Auth
			ourLog.debug("IntrospectURL:" + getIntrospectUrl() + " clientID:" + getClientId() + " clientSecret:"
					+ getClientSecret());
			Authorization myAuth = new Authorization(getIntrospectUrl(), getClientId(), getClientSecret());

			String err_msg = myAuth.introspectToken(theRequest);
			if (err_msg.isEmpty() == false) {
				throw new AuthenticationException(err_msg);
			}

			// Now we have a valid access token. Now, check Token type
			if (myAuth.checkBearer() == false) {
				throw new AuthenticationException("Not Token Bearer");
			}

			// Check scope.
			return myAuth.allowRequest(theRequestDetails);				
		}
		
		// for test.
		// String resourceName = theRequestDetails.getResourceName();
		// String resourceOperationType =
		// theRequestDetails.getResourceOperationType().name();
		// System.out.println ("resource:"+resourceName+",
		// resourceOperationType:"+resourceOperationType);

	}

	public String getAuthType() {
		return authType;
	}
	
	public void setAuthType(String authType) {
		this.authType = authType;
	}
	
	public String getIntrospectUrl() {
		return introspectUrl;
	}

	public void setIntrospectUrl(String introspectURL) {
		this.introspectUrl = introspectURL;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

//	public String getLocalByPass() {
//		return localByPass;
//	}
//
//	public void setLocalByPass(String localByPass) {
//		this.localByPass = localByPass;
//	}
//
	public String getReadOnly() {
		return readOnly;
	}
	
	public void setReadOnly(String readOnly) {
		this.readOnly = readOnly;
	}
}
