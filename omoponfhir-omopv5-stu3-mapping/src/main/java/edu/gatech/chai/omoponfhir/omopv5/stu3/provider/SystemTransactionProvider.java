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
package edu.gatech.chai.omoponfhir.omopv5.stu3.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import org.hl7.fhir.dstu3.model.Bundle.HTTPVerb;
import org.hl7.fhir.dstu3.model.Composition;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.exceptions.FHIRException;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.annotation.Transaction;
import ca.uhn.fhir.rest.annotation.TransactionParam;
import edu.gatech.chai.omoponfhir.omopv5.stu3.mapping.BaseOmopResource;
import edu.gatech.chai.omoponfhir.omopv5.stu3.mapping.OmopTransaction;
import edu.gatech.chai.omoponfhir.omopv5.stu3.model.MyBundle;
import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.ThrowFHIRExceptions;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;

public class SystemTransactionProvider {

	private WebApplicationContext myAppCtx;
	private String myDbType;
	private OmopTransaction myMapper;
	private int preferredPageSize = 30;
	private Map<String, Object> supportedProvider = new HashMap<String, Object>();

	public static String getType() {
		return "Bundle";
	}

	public void addSupportedProvider(String resourceName, Object resourceMapper) {
		supportedProvider.put(resourceName, resourceMapper);
	}

	public SystemTransactionProvider() {
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
		myDbType = myAppCtx.getServletContext().getInitParameter("backendDbType");
		if (myDbType.equalsIgnoreCase("omopv5") == true) {
			myMapper = new OmopTransaction(myAppCtx);
		} else {
			myMapper = new OmopTransaction(myAppCtx);
		}

		String pageSizeStr = myAppCtx.getServletContext().getInitParameter("preferredPageSize");
		if (pageSizeStr != null && pageSizeStr.isEmpty() == false) {
			int pageSize = Integer.parseInt(pageSizeStr);
			if (pageSize > 0) {
				preferredPageSize = pageSize;
			}
		}

		// String url =
		// myAppCtx.getServletContext().getInitParameter("transactionServer");
		// if (url != null && url.isEmpty() == false) {
		// setMyTransactionServerUrl(url);
		// if (url.equals("${requestUrl}")) {
		// getTransactionServerUrlFromRequest = true;
		// } else {
		// getTransactionServerUrlFromRequest = false;
		// }
		// } else {
		// getTransactionServerUrlFromRequest = true;
		// }

	}

	// public void setMyTransactionServerUrl(String myTransactionServerUrl) {
	// this.myTransactionServerUrl = myTransactionServerUrl;
	// }

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <v extends BaseOmopResource> void undoCreate(List<Object> resourcesAdded) {
		v mapper;

		for (Object entry : resourcesAdded) {
			Pair<v, Long> entryPair = (Pair<v, Long>) entry;
			mapper = entryPair.getKey();
			Long id = entryPair.getValue();
			mapper.removeDbase(id);
		}
	}

	@SuppressWarnings("unchecked")
	private void addToList(Map<HTTPVerb, Object> transactionEntries, MyBundle theBundle, HTTPVerb verb) {
		List<Resource> postList = (List<Resource>) transactionEntries.get(HTTPVerb.POST);
		List<Resource> putList = (List<Resource>) transactionEntries.get(HTTPVerb.PUT);
		List<String> deleteList = (List<String>) transactionEntries.get(HTTPVerb.DELETE);
		List<ParameterWrapper> getList = (List<ParameterWrapper>) transactionEntries.get(HTTPVerb.GET);

		List<BundleEntryComponent> entries = theBundle.getEntry();

		BundleEntryComponent entry = null;

		int sizeOfEntries = entries.size();
		for (int i = 1; i < sizeOfEntries; i++) {
			entry = entries.get(i);
			if (verb != null || (entry.getRequest() != null && !entry.getRequest().isEmpty())) {
				if (verb == HTTPVerb.POST || entry.getRequest().getMethod() == HTTPVerb.POST) {
					postList.add(entry.getResource());
				} else if (verb == HTTPVerb.PUT || entry.getRequest().getMethod() == HTTPVerb.PUT) {
					// This is to update. Get URL
					String urlString = entry.getRequest().getUrl();
					IdType idType = new IdType(urlString);
					// We must be able to get Id as Long as OMOP only handles Long Id.
					entry.getResource().setId(idType);
					putList.add(entry.getResource());
				} else {
					ThrowFHIRExceptions.unprocessableEntityException("We support POST and PUT for Messages");
				}
			}
		}
	}

	/**
	 */
	@Transaction
	public Bundle transaction(@TransactionParam MyBundle theBundle, HttpServletRequest theRequest) {
		validateResource(theBundle);

		Bundle retVal = new Bundle();
		List<Resource> postList = new ArrayList<Resource>();
		List<Resource> putList = new ArrayList<Resource>();
		List<String> deleteList = new ArrayList<String>();
		List<ParameterWrapper> getList = new ArrayList<ParameterWrapper>();

		Map<HTTPVerb, Object> transactionEntries = new HashMap<HTTPVerb, Object>();
		transactionEntries.put(HTTPVerb.POST, postList);
		transactionEntries.put(HTTPVerb.PUT, putList);
		transactionEntries.put(HTTPVerb.DELETE, deleteList);
		transactionEntries.put(HTTPVerb.GET, getList);

//		Resource resource = null;
//		BundleEntryComponent entry = null;

		try {
			Resource resource;
			switch (theBundle.getType()) {
			case DOCUMENT:
				// https://www.hl7.org/fhir/documents.html#bundle
				// Ignore the fact that the bundle is a document and process all of the
				// resources that it contains as individual resources. Clients SHOULD not expect
				// that a server that receives a document submitted using this method will be
				// able to reassemble the document exactly. (Even if the server can reassemble
				// the document (see below), the result cannot be expected to be in the same
				// order, etc. Thus a document signature will very likely be invalid.)
				
				List<BundleEntryComponent> entries = theBundle.getEntry();
				int index = 0;
				for (BundleEntryComponent entry: entries) {
					resource = entry.getResource();
					
					// First entry is Composition
					if (index == 0) {
						if (resource.getResourceType() == ResourceType.Composition) {
							// First check the patient 
							Composition composition = (Composition) resource;
							
						} else {
							// First entry must be Composition resource.
							ThrowFHIRExceptions
									.unprocessableEntityException("First entry in "
											+ "Bundle document type should be Composition");
						}
					} else {
						// 
					}
					
					index++;
				}
				
				
//				entry = theBundle.getEntryFirstRep();
//				resource = entry.getResource();
//				if (resource.getResourceType() == ResourceType.Composition) {
//					Composition composition = (Composition) resource;
//					// Find out from composition if we can proceed.
//					// For now, we do not care what type of this document is.
//					// We just parse all the entries and do what we need to do.
//
//					addToList(transactionEntries, theBundle, HTTPVerb.POST);
//
//					List<BundleEntryComponent> responseTransaction = myMapper.executeRequests(transactionEntries);
//					if (responseTransaction != null && responseTransaction.size() > 0) {
//						retVal.setEntry(responseTransaction);
//						retVal.setType(BundleType.TRANSACTIONRESPONSE);
//					} else {
//						ThrowFHIRExceptions.unprocessableEntityException(
//								"Faied process the bundle, " + theBundle.getType().toString());
//					}
//				} else {
//					// First entry must be Composition resource.
//					ThrowFHIRExceptions
//							.unprocessableEntityException("First entry in Bundle document type should be Composition");
//				}
			case TRANSACTION:
				System.out.println("We are at the transaction");
				for (BundleEntryComponent nextEntry : theBundle.getEntry()) {
					resource = nextEntry.getResource();
					BundleEntryRequestComponent request = nextEntry.getRequest();

					// We require a transaction to have a request so that we can
					// handle the transaction. Without it, we have nothing to
					// do.
					if (request == null)
						continue;

					if (!request.isEmpty()) {
						// First check the Resource to see if we can support
						// this. resourceName =
						// resource.getResourceType().toString();

						// Now we have a request that we support. Add this into
						// the entry to process.
						HTTPVerb method = request.getMethod();
						if (method == HTTPVerb.POST) {
							postList.add(resource);
						} else if (method == HTTPVerb.PUT) {
							putList.add(resource);
						} else if (method == HTTPVerb.DELETE) {
							deleteList.add(request.getUrl());
						} else if (method == HTTPVerb.GET) {
							// TODO: getList.add(new ParameterWrapper());
							// create parameter here.
						} else {
							continue;
						}
					}
				}

				break;
			case MESSAGE:
//				entry = theBundle.getEntryFirstRep();
//				resource = entry.getResource();
//				if (resource.getResourceType() == ResourceType.MessageHeader) {
//					MessageHeader messageHeader = (MessageHeader) resource;
//					// We handle observation-type.
//					// TODO: Add other types later.
//					Coding event = messageHeader.getEvent();
//					if ("R01".equals(event.getCode())) {
//						// This is lab report. they are all to be added to the server.
//						addToList(transactionEntries, theBundle, HTTPVerb.POST);
//					} else {
//						ThrowFHIRExceptions
//								.unprocessableEntityException("We currently support only HL7 v2 R01 Message");
//					}
//				} else {
//					// First entry must be message header.
//					ThrowFHIRExceptions
//							.unprocessableEntityException("First entry in Bundle message type should be MessageHeader");
//				}
				break;
			default:
				ThrowFHIRExceptions.unprocessableEntityException("Unsupported Bundle Type, "
						+ theBundle.getType().toString() + ". We support DOCUMENT, TRANSACTION, and MESSAGE");
			}

			List<BundleEntryComponent> responseTransaction = myMapper.executeRequests(transactionEntries);
			if (responseTransaction != null && responseTransaction.size() > 0) {
				retVal.setEntry(responseTransaction);
				retVal.setType(BundleType.TRANSACTIONRESPONSE);
			} else {
				ThrowFHIRExceptions
						.unprocessableEntityException("Faied process the bundle, " + theBundle.getType().toString());
			}

		} catch (FHIRException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return retVal;
	}

	// TODO: Add more validation code here.
	private void validateResource(MyBundle theBundle) {
	}

}
