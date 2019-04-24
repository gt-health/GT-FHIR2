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
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
//import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import edu.gatech.chai.omoponfhir.security.Authorization;
import edu.gatech.chai.smart.jpa.entity.SmartLaunchContext;
import edu.gatech.chai.smart.jpa.entity.SmartLaunchContextParam;
import edu.gatech.chai.smart.jpa.service.SmartOnFhirLaunchContextService;

/**
 * Servlet implementation class SmartServices
@WebServlet(description = "SMART on FHIR internal services", urlPatterns = { "/_services/smart/Launch" })
 */

public class SmartServices extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private WebApplicationContext myAppCtx;
	
	private String url;
	private String client_id;
	private String client_secret;
	private SmartOnFhirLaunchContextService mySmartOnFhirContextService;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SmartServices() {
        super();
        
    	myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
    	mySmartOnFhirContextService = myAppCtx.getBean(SmartOnFhirLaunchContextService.class);
    }

    public void init() throws ServletException {
		// According to SMART on FHIR folks in Harvard. They want to support both
		// basic AUTH and bearer AUTH for the internal communication for this.
    	url = System.getenv("SMART_INTROSPECTURL");
    	client_id = System.getenv("SMART_CLIENTID");
    	client_secret = System.getenv("SMART_CLIENTSECRET");
    	
    	if (url == null)
    		url = getServletConfig().getInitParameter("introspectUrl");
    	if (client_id == null)
    		client_id = getServletConfig().getInitParameter("client_id");
    	if (client_secret == null) 
    		client_secret = getServletConfig().getInitParameter("client_secret");
    	    	
//        bookDB = (BookDBAO)getServletContext().
//            getAttribute("bookDB");
//        if (bookDB == null) throw new
//            UnavailableException("Couldn’t get database.");
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		String launchId = new String(request.getPathInfo().substring(1));

		if (launchId == null || launchId.isEmpty()) {
			System.out.println("LaunchID is invalid");
			return;
		}
		
		System.out.println("Get LaunchID = "+launchId);

		Authorization smartAuth = new Authorization(url, client_id, client_secret);
		if (smartAuth.asBasicAuth(request) == true || smartAuth.asBearerAuth(request) == true) {
			SmartLaunchContext smartLaunchContext = (SmartLaunchContext) mySmartOnFhirContextService.getContext(Long.valueOf(launchId));
			if (smartLaunchContext != null) {
				JSONObject jsonResp = smartLaunchContext.getJSONObject();
				
				if (jsonResp == null) {
					System.out.println("Launch ID "+launchId+" does not exist.");
					return;
				}
				
				String respString = jsonResp.toString();
				response.setContentType("application/json; charset=UTF-8;");
				response.getWriter().append(respString);
				System.out.println(respString);
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// We have received a request to create Launch context
		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();
				
//		String url = getServletConfig().getInitParameter("introspectUrl");
//		String client_id = getServletConfig().getInitParameter("client_id");
//		String client_secret = getServletConfig().getInitParameter("client_secret");
		Authorization smartAuth = new Authorization(url, client_id, client_secret);
		if (smartAuth.asBasicAuth(request) == true || smartAuth.asBearerAuth(request) == true) {
			if (smartAuth.assertScope("smart/orchestrate_launch") == false) {
				// This is internal communication for SMART orchestration. Other request is not
				// allowed.
				return;
			}
			
			String line;
			while ((line=reader.readLine())!=null) {
				buffer.append(line);
			}
			
			System.out.println("LaunchContext: "+buffer.toString());

			// Convert the body content to JSON and create launch context in database.
			//
			JSONObject servReq = new JSONObject(buffer.toString());

			String launchContextClientId = null;
			if (!servReq.isNull("client_id")) {
				launchContextClientId = servReq.getString("client_id");
			}
			
			String launchContextCreatedBy = smartAuth.getClientId();
			String launchContextUsername = smartAuth.getUserId();
			java.util.Date date= new java.util.Date();
			Timestamp ts = new Timestamp(date.getTime());

			SmartLaunchContext smartLaunchContext = new SmartLaunchContext();
			if (launchContextClientId != null) 
				smartLaunchContext.setClientId(launchContextClientId);
			smartLaunchContext.setCreatedBy(launchContextCreatedBy);
			smartLaunchContext.setCreatedAt(ts);
			smartLaunchContext.setUsername(launchContextUsername);

			List<SmartLaunchContextParam> smartLaunchContextParams = new ArrayList<SmartLaunchContextParam>();
			smartLaunchContext.setSmartLaunchContextParams(smartLaunchContextParams);
			JSONObject paramsJSON = servReq.getJSONObject("parameters");
			Iterator<?> paramsIter = paramsJSON.keys();
//			String jsonParams="";
			while (paramsIter.hasNext()) {
				String key = (String) paramsIter.next();
				String val = paramsJSON.getString(key);
				System.out.println ("Parameters: key: "+key+" val:"+val);
				
				SmartLaunchContextParam smartLaunchContextParam = new SmartLaunchContextParam();
				smartLaunchContextParam.setParamName(key);
				smartLaunchContextParam.setParamValue(val);
				smartLaunchContext.addSmartLaunchContextParam(smartLaunchContextParam);
//				if (key.equalsIgnoreCase("patient")) {
//					// We supposed to have only patient.
//					jsonParams="\"patient\":\""+val+"\","
//							+"\"smart_style_url\":"+"\"https://fhir.smarthealthit.org/stylesheets/smart_v1.json\""+","
//							+"\"need_patient_banner\":true";
//				}
			}
			mySmartOnFhirContextService.setContext(smartLaunchContext);
			
			/* Response with JSON object with the following information.
			 *	{
			 *	  "created_by": “[CLIENTID]",
			 *	  "username": “[USERNAME]",
			 *	  "launch_id": “[LAUNCHID]",
			 *	  "created_at": “[ISO8601TIMESTAMP]",
			 *	  "parameters": {
			 *	    "patient": “[PATIENTID]",
			 *	    "smart_style_url": "https://fhir.smarthealthit.org/stylesheets/smart_v1.json",
			 *	    "need_patient_banner": true
			 *	  }
			 *	}
		 	 */
			
			response.setContentType("application/json; charset=UTF-8;");
//			TimeZone tz = TimeZone.getTimeZone("UTC");
//		    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
//		    df.setTimeZone(tz);
//		    date = new Date(ts.getTime());
//		    String createdAt = df.format(date);
//
			PrintWriter out = response.getWriter();
//			String jsonData = "{\"created_by\":\""+launchContextClientId+"\","
//					+"\"username\":\""+launchContextUsername+"\","
//					+"\"launch_id\":\""+smartLaunchContext.getLaunchId()+"\","
//					+"\"created_at\":\""+createdAt+"\"";
//			
//			// Parameters
//			if (jsonParams.isEmpty() == false) {
//				jsonData = jsonData.concat(",\"parameters\": {"+jsonParams+"}");
//			}
//			
//			jsonData = jsonData.concat("}");
			
			String jsonData = smartLaunchContext.getJSONObject().toString();
		    out.println(jsonData) ; 
		    out.close();
		    
		    System.out.println("SMARTonFHIR service response: "+jsonData);
		}

//		Enumeration<String> headerNames = request.getHeaderNames();
//		while (headerNames.hasMoreElements()) {
//			String headerName = headerNames.nextElement();
//			System.out.println(headerName);
//			Enumeration<String> headers = request.getHeaders(headerName);
//			while (headers.hasMoreElements()) {
//				String headerValue = headers.nextElement();
//				System.out.println("  "+headerValue);
//			}
//		}
		
	}
}
