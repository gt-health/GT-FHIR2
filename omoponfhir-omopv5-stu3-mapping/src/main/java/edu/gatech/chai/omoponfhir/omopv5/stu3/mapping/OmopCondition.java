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

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.CodeableConceptUtil;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.ConditionResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.EncounterResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.PatientResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.PractitionerResourceProvider;
import edu.gatech.chai.omopv5.dba.service.*;
import edu.gatech.chai.omopv5.model.entity.*;

import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class OmopCondition extends BaseOmopResource<Condition, ConditionOccurrence, ConditionOccurrenceService> implements IResourceMapping<Condition, ConditionOccurrence> {

    private static final Logger logger = LoggerFactory.getLogger(OmopCondition.class);

    private static OmopCondition omopCondition = new OmopCondition();

    private ConditionOccurrenceService conditionOccurrenceService;
    private FPersonService fPersonService;
    private ProviderService providerService;
    private ConceptService conceptService;
    private VisitOccurrenceService visitOccurrenceService;

    public OmopCondition(WebApplicationContext context) {
        super(context, ConditionOccurrence.class, ConditionOccurrenceService.class, ConditionResourceProvider.getType());
        initialize(context);
    }

    public OmopCondition() {
        super(ContextLoaderListener.getCurrentWebApplicationContext(), ConditionOccurrence.class, ConditionOccurrenceService.class, ConditionResourceProvider.getType());
        initialize(ContextLoaderListener.getCurrentWebApplicationContext());
    }

    private void initialize(WebApplicationContext context) {
        // Get bean for other services that we need for mapping.
        conditionOccurrenceService = context.getBean(ConditionOccurrenceService.class);
        fPersonService = context.getBean(FPersonService.class);
        providerService = context.getBean(ProviderService.class);
        conceptService = context.getBean(ConceptService.class);
        visitOccurrenceService = context.getBean(VisitOccurrenceService.class);
    }

    public static OmopCondition getInstance() {
        return OmopCondition.omopCondition;
    }

    @Override
    public Condition constructFHIR(Long fhirId, ConditionOccurrence conditionOccurrence){
        Condition condition = new Condition();
        condition.setId(new IdType(fhirId));

        addPersonToCondition(conditionOccurrence, condition);
        addCodeToCondition(conditionOccurrence, condition);
        addStartAndEndDateToCondition(conditionOccurrence, condition);
        addTypeToCondition(conditionOccurrence, condition);
        addAsserterToCondition(conditionOccurrence, condition);
        addContextToCondition(conditionOccurrence, condition);

        //TODO: Need to map the following
        //??Condition.abatement.abatementString, but we are using abatementDateTime for the end date and Abatement[x] has a 0..1 cardinality.
        String stopReason = conditionOccurrence.getStopReason();
        //??
        String sourceValue = conditionOccurrence.getConditionSourceValue();
        //??
        Concept sourceConceptId = conditionOccurrence.getSourceConceptId();

        return condition;
    }

    @Override
    public Long toDbase(Condition fhirResource, IdType fhirId) throws FHIRException {
        Long retval;
    	Long omopId = null;
    	
    	if (fhirId != null) {
    		omopId = IdMapping.getOMOPfromFHIR(fhirId.getIdPartAsLong(), ConditionResourceProvider.getType());
    	}
    	
    	ConditionOccurrence conditionOccurrence = constructOmop(omopId, fhirResource);
    	
        //TODO: Do you need to call other services to update links resources.

        if (conditionOccurrence.getId() != null) {
            retval = conditionOccurrenceService.update(conditionOccurrence).getId();
        } else {
            retval = conditionOccurrenceService.create(conditionOccurrence).getId();
        }

        return IdMapping.getFHIRfromOMOP(retval, getMyFhirResourceType());
    }

    public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
        List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
        ParameterWrapper paramWrapper = new ParameterWrapper();
        if (or) paramWrapper.setUpperRelationship("or");
        else paramWrapper.setUpperRelationship("and");
        
        switch (parameter) {
            case Condition.SP_ABATEMENT_AGE:
                //not supporting
                break;
            case Condition.SP_ABATEMENT_BOOLEAN:
                //not supporting
                break;
            case Condition.SP_ABATEMENT_DATE:
                //Condition.abatementDate -> Omop ConditionOccurrence.conditionEndDate
                putDateInParamWrapper(paramWrapper, value, "conditionEndDate");
                mapList.add(paramWrapper);
                break;
            case Condition.SP_ABATEMENT_STRING:
                //not supporting
                break;
            case Condition.SP_ASSERTED_DATE:
                //Condition.assertedDate -> Omop ConditionOccurrence.conditionStartDate
                putDateInParamWrapper(paramWrapper, value, "conditionStartDate");
                mapList.add(paramWrapper);
                break;
            case Condition.SP_ASSERTER:
                //Condition.asserter -> Omop Provider
                ReferenceParam patientReference = ((ReferenceParam) value);
                String patientId = String.valueOf(patientReference.getIdPartAsLong());

                paramWrapper.setParameterType("Long");
                paramWrapper.setParameters(Arrays.asList("provider.id"));
                paramWrapper.setOperators(Arrays.asList("="));
                paramWrapper.setValues(Arrays.asList(patientId));
                paramWrapper.setRelationship("or");
                mapList.add(paramWrapper);
                break;
            case Condition.SP_BODY_SITE:
                //not supporting
                break;
            case Condition.SP_CATEGORY:
                //Condition.category
                putConditionInParamWrapper(paramWrapper, value);
                mapList.add(paramWrapper);
                break;
            case Condition.SP_CLINICAL_STATUS:
                break;
            case Condition.SP_CODE:
    			String system = ((TokenParam) value).getSystem();
    			String code = ((TokenParam) value).getValue();
//    			System.out.println("\n\n\n\n\nSystem:"+system+"\n\ncode:"+code+"\n\n\n\n\n");
    			if ((system == null || system.isEmpty()) && (code == null || code.isEmpty()))
    				break;
    			
    			String omopVocabulary = "None";
    			if (system != null && !system.isEmpty()) {
    				try {
    					omopVocabulary = OmopCodeableConceptMapping.omopVocabularyforFhirUri(system);
    				} catch (FHIRException e) {
    					e.printStackTrace();
    				}
    			} 

    			paramWrapper.setParameterType("String");
    			if ("None".equals(omopVocabulary) && code != null && !code.isEmpty()) {
    				paramWrapper.setParameters(Arrays.asList("conceptId.conceptCode"));
    				paramWrapper.setOperators(Arrays.asList("="));
    				paramWrapper.setValues(Arrays.asList(code));
    			} else if (!"None".equals(omopVocabulary) && (code == null || code.isEmpty())) {
    				paramWrapper.setParameters(Arrays.asList("conceptId.vocabulary.id"));
    				paramWrapper.setOperators(Arrays.asList("="));
    				paramWrapper.setValues(Arrays.asList(omopVocabulary));				
    			} else {
    				paramWrapper.setParameters(Arrays.asList("conceptId.vocabulary.id", "conceptId.conceptCode"));
    				paramWrapper.setOperators(Arrays.asList("=","="));
    				paramWrapper.setValues(Arrays.asList(omopVocabulary, code));
    			}
    			paramWrapper.setRelationship("and");
    			mapList.add(paramWrapper);
            	
//                //Condition.code -> Omop Concept
//                putConditionInParamWrapper(paramWrapper, value);
//                mapList.add(paramWrapper);

                break;
            case Condition.SP_CONTEXT:
                //Condition.context -> Omop VisitOccurrence
                ReferenceParam visitReference = (ReferenceParam)value;
                String visitId = String.valueOf(visitReference.getIdPartAsLong());
                paramWrapper.setParameterType("Long");
                paramWrapper.setParameters(Arrays.asList("visitOccurrence.id"));
                paramWrapper.setOperators(Arrays.asList("="));
                paramWrapper.setValues(Arrays.asList(visitId));
                paramWrapper.setRelationship("or");
                mapList.add(paramWrapper);
                break;
            case Condition.SP_ENCOUNTER:
                //not supporting
                break;
            case Condition.SP_EVIDENCE:
                //not supporting
                break;
            case Condition.SP_EVIDENCE_DETAIL:
                //not supporting
                break;
            case Condition.SP_IDENTIFIER:
                //not supporting
                break;
            case Condition.SP_ONSET_AGE:
                //not supporting
                break;
            case Condition.SP_ONSET_DATE:
                //not supporting
                break;
            case Condition.SP_ONSET_INFO:
                //not supporting
                break;
            case Condition.SP_PATIENT:
    		case Condition.SP_SUBJECT:
    			ReferenceParam subjectReference = ((ReferenceParam) value);
    			Long fhirPatientId = subjectReference.getIdPartAsLong();
    			Long omopPersonId = IdMapping.getOMOPfromFHIR(fhirPatientId, PatientResourceProvider.getType());

    			String omopPersonIdString = String.valueOf(omopPersonId);
    			
    			paramWrapper.setParameterType("Long");
    			paramWrapper.setParameters(Arrays.asList("fPerson.id"));
    			paramWrapper.setOperators(Arrays.asList("="));
    			paramWrapper.setValues(Arrays.asList(omopPersonIdString));
    			paramWrapper.setRelationship("or");
    			mapList.add(paramWrapper);
    			break;
    		case Procedure.SP_RES_ID:
    			String conditionId = ((TokenParam) value).getValue();
    			paramWrapper.setParameterType("Long");
    			paramWrapper.setParameters(Arrays.asList("id"));
    			paramWrapper.setOperators(Arrays.asList("="));
    			paramWrapper.setValues(Arrays.asList(conditionId));
    			paramWrapper.setRelationship("or");
    			mapList.add(paramWrapper);
    			break;
            case Condition.SP_SEVERITY:
                //not supporting
                break;
            case Condition.SP_STAGE:
                //not supporting
                break;
            case Condition.SP_VERIFICATION_STATUS:
                //not supporting
                break;
            default:
                mapList = null;
        }

        return mapList;
    }

    /*======================================================================*/
    /* PRIVATE METHODS */
    /*======================================================================*/

    private void putConditionInParamWrapper(ParameterWrapper paramWrapper, Object value){
        String system = ((TokenParam) value).getSystem();
        String code = ((TokenParam) value).getValue();

        paramWrapper.setParameterType("String");
        paramWrapper.setParameters(Arrays.asList("concept.vocabularyId", "concept.conceptCode"));
        paramWrapper.setParameters(Arrays.asList("like", "like"));
        paramWrapper.setValues(Arrays.asList(system, code));
        paramWrapper.setRelationship("and");
    }

    private void putDateInParamWrapper(ParameterWrapper paramWrapper, Object value, String omopTableColumn){
        DateParam dateParam = (DateParam)value;
        ParamPrefixEnum apiOperator = dateParam.getPrefix();
        String sqlOperator = null;
        if (apiOperator.equals(ParamPrefixEnum.GREATERTHAN)) {
            sqlOperator = ">";
        } else if (apiOperator.equals(ParamPrefixEnum.GREATERTHAN_OR_EQUALS)) {
            sqlOperator = ">=";
        } else if (apiOperator.equals(ParamPrefixEnum.LESSTHAN)) {
            sqlOperator = "<";
        } else if (apiOperator.equals(ParamPrefixEnum.LESSTHAN_OR_EQUALS)) {
            sqlOperator = "<=";
        } else if (apiOperator.equals(ParamPrefixEnum.NOT_EQUAL)) {
            sqlOperator = "!=";
        } else {
            sqlOperator = "=";
        }
        Date effectiveDate = dateParam.getValue();

        paramWrapper.setParameterType("Date");
        paramWrapper.setParameters(Arrays.asList(omopTableColumn));
        paramWrapper.setOperators(Arrays.asList(sqlOperator));
        paramWrapper.setValues(Arrays.asList(String.valueOf(effectiveDate.getTime())));
        paramWrapper.setRelationship("or");
    }

    private CodeableConcept retrieveCodeableConcept(Concept concept){
        CodeableConcept conditionCodeableConcept = null;
        try {
            conditionCodeableConcept = CodeableConceptUtil.createFromConcept(concept);
        }
        catch(FHIRException fe){
            logger.error("Could not generate CodeableConcept from Concept.", fe);
        }
        return conditionCodeableConcept;
    }

    private void addPersonToCondition(ConditionOccurrence conditionOccurrence, Condition condition){
        //Condition.subject
        FPerson fPerson = conditionOccurrence.getFPerson();
        //set the person
        Reference subjectRef = new Reference(new IdType(PatientResourceProvider.getType(), fPerson.getId()));
        subjectRef.setDisplay(fPerson.getNameAsSingleString());
        condition.setSubject(subjectRef);
    }

    private void addCodeToCondition(ConditionOccurrence conditionOccurrence, Condition condition){
        //Condition.code SNOMED-CT
        Concept conceptId = conditionOccurrence.getConceptId();
        CodeableConcept conditionCodeableConcept = retrieveCodeableConcept(conceptId);
        if(conditionCodeableConcept != null ) {
            condition.setCode(conditionCodeableConcept);
        }
    }

    private void addStartAndEndDateToCondition(ConditionOccurrence conditionOccurrence, Condition condition){
        //Condition.onsetDateTime
        Date startDate = conditionOccurrence.getStartDate();
        if( startDate != null ){
            DateTimeType onsetDateTime = new DateTimeType(startDate);
            condition.setOnset(onsetDateTime);
        }
        //Condition.abatementDateTime
        Date endDate = conditionOccurrence.getEndDate();
        if( endDate != null ){
            DateTimeType abatementDateTime = new DateTimeType(endDate);
            condition.setAbatement(abatementDateTime);
        }
    }

    private void addTypeToCondition(ConditionOccurrence conditionOccurrence, Condition condition){
        //Condition.category
        Concept typeConceptId = conditionOccurrence.getTypeConceptId();
        CodeableConcept typeCodeableConcept = retrieveCodeableConcept(typeConceptId);
        if(typeCodeableConcept != null){
            List<CodeableConcept> typeList = new ArrayList<CodeableConcept>();
            typeList.add(typeCodeableConcept);
            condition.setCategory(typeList);
        }
    }

    private void addAsserterToCondition(ConditionOccurrence conditionOccurrence, Condition condition){
        //Condition.asserter
        Provider provider = conditionOccurrence.getProvider();
        if( provider != null ) {
            Reference providerRef = new Reference(new IdType(PractitionerResourceProvider.getType(), provider.getId()));
            providerRef.setDisplay(provider.getProviderName());
            condition.setAsserter(providerRef);
        }
    }

    private void addContextToCondition(ConditionOccurrence conditionOccurrence, Condition condition){
        //Condition.context
        VisitOccurrence visitOccurrence = conditionOccurrence.getVisitOccurrence();
        if( visitOccurrence != null ) {
            Reference visitRef = new Reference(new IdType(EncounterResourceProvider.getType(), visitOccurrence.getId()));
            condition.setContext(visitRef);
        }
    }

	@Override
	public ConditionOccurrence constructOmop(Long omopId, Condition fhirResource) {
        //things to update Condition_Occurrence, Concept, FPerson, Provider, VisitOccurrence
        ConditionOccurrence conditionOccurrence;
        FPerson fPerson;
        Provider provider;
        VisitOccurrence visitOccurrence;

        //check for an existing condition
        if (omopId != null) {
            conditionOccurrence = conditionOccurrenceService.findById(omopId);
        } else {
        	conditionOccurrence = new ConditionOccurrence();
        }

        //get the Subject
        if( fhirResource.getSubject() != null ){
            Long subjectId = fhirResource.getSubject().getReferenceElement().getIdPartAsLong();
            Long subjectFhirId = IdMapping.getOMOPfromFHIR(subjectId, PatientResourceProvider.getType());
            fPerson = fPersonService.findById(subjectFhirId);
            conditionOccurrence.setFPerson(fPerson);
        }
        else{
            //throw an error
            try {
				throw new FHIRException("FHIR Resource does not contain a Subject.");
			} catch (FHIRException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        //get the Provider
        if( fhirResource.getAsserter() != null ){
            Long providerId = fhirResource.getAsserter().getReferenceElement().getIdPartAsLong();
            Long providerOmopId = IdMapping.getOMOPfromFHIR(providerId, PractitionerResourceProvider.getType());
            provider = providerService.findById(providerOmopId);
            conditionOccurrence.setProvider(provider);
        }
        else {
            //else create provider
            try {
				throw new FHIRException("FHIR Resource does not contain a Provider.");
			} catch (FHIRException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        //get the concept code
        if( fhirResource.getCode() != null ){
            List<Coding> codes = fhirResource.getCode().getCoding();
            Concept omopConcept;
            //there is only one so get the first
            try {
				omopConcept = CodeableConceptUtil.getOmopConceptWithFhirConcept(conceptService, codes.get(0));
	            //set the concept
	            conditionOccurrence.setConceptId(omopConcept);
			} catch (FHIRException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        else{
            //is there a generic condition concept to use?
            try {
				throw new FHIRException("FHIR Resource does not contain a Condition Code.");
			} catch (FHIRException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        //get the start and end date. We are expecting both to be of type DateTimeType
        if( fhirResource.getOnset() != null &&
            fhirResource.getOnset() instanceof DateTimeType ){
            conditionOccurrence.setStartDate(((DateTimeType)fhirResource.getOnset()).toCalendar().getTime());
        }
        else{
            //create a start date
            try {
				throw new FHIRException("FHIR Resource does not contain a start date.");
			} catch (FHIRException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        if( fhirResource.getAbatement() != null &&
            fhirResource.getAbatement() instanceof DateTimeType ){
            conditionOccurrence.setEndDate(((DateTimeType)fhirResource.getAbatement()).toCalendar().getTime());
        }
        else{
            //leave alone, end date not required
        }

        //set the conditions
        if( fhirResource.getCategory() != null ) {
            List<CodeableConcept> categories = fhirResource.getCategory();
            Concept omopTypeConcept;
            //there is only one so get the first
            try {
				omopTypeConcept = CodeableConceptUtil.getOmopConceptWithFhirConcept(conceptService, categories.get(0).getCodingFirstRep());
	            conditionOccurrence.setTypeConceptId(omopTypeConcept);
			} catch (FHIRException e) {
				e.printStackTrace();
			}
        }
        else{
            //is there a generic condition type concept to use?
            try {
				throw new FHIRException("FHIR Resource does not contain a Condition category.");
			} catch (FHIRException e) {
				e.printStackTrace();
			}
        }

        //set the context
        if( fhirResource.getContext() != null ){
            Long visitId = fhirResource.getContext().getReferenceElement().getIdPartAsLong();
            Long visitFhirId = IdMapping.getOMOPfromFHIR(visitId, EncounterResourceProvider.getType());
            visitOccurrence = visitOccurrenceService.findById(visitFhirId);
            conditionOccurrence.setVisitOccurrence(visitOccurrence);
        }
        
		return conditionOccurrence;
	}
}