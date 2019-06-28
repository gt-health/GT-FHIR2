package edu.gatech.chai.omoponfhir.omopv5.stu3.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ConceptMap;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import edu.gatech.chai.omoponfhir.omopv5.stu3.provider.ConceptMapResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.CodeableConceptUtil;
import edu.gatech.chai.omopv5.dba.service.ConceptRelationshipService;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.ConceptRelationship;

public class OmopConceptMap extends BaseOmopResource<ConceptMap, ConceptRelationship, ConceptRelationshipService>
		implements IResourceMapping<ConceptMap, ConceptRelationship> {

	private static final Logger logger = LoggerFactory.getLogger(OmopConceptMap.class);
	private static OmopConceptMap omopConceptMap = new OmopConceptMap();
	private ConceptService conceptService;
	
	public OmopConceptMap(WebApplicationContext context) {
		super(context, ConceptRelationship.class, ConceptRelationshipService.class, ConceptMapResourceProvider.getType());
		initialize(context);
	}

	public OmopConceptMap() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), ConceptRelationship.class, ConceptRelationshipService.class, ConceptMapResourceProvider.getType());
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}
	
	private void initialize(WebApplicationContext context) {
		conceptService = context.getBean(ConceptService.class);
	}
	
	@Override
	public Long toDbase(ConceptMap fhirResource, IdType fhirId) throws FHIRException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConceptMap constructFHIR(Long fhirId, ConceptRelationship entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConceptRelationship constructOmop(Long omopId, ConceptMap fhirResource) {
		// TODO Auto-generated method stub
		return null;
	}

	public Parameters translateConcept(String code, String system, String targetUri, String targetSystem) {
		Parameters retVal = new Parameters();
		
		// Using the system/code and targetSystem, map the system/code.
		String omopSrcVocab = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(system);
		String omopTargetVocab = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(targetSystem);

		if ("None".equals(omopSrcVocab) || "None".equals(omopTargetVocab)) {
			logger.error("$translate: trying to translate not-known coding system ("+system+"|"+code+" to "+targetSystem);
			return retVal;
		}
		
		String relationshipId = omopSrcVocab+" % "+omopTargetVocab+" eq";
		logger.debug("$translate requested for "+relationshipId);
		
		// Find concept_id for source coding.
		Concept omopSrcConcept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, omopSrcVocab, code);
		if (omopSrcConcept == null) {
			logger.error("$translate: could not find concept for "+system+"|"+code);
			return retVal;
		}
		
		logger.debug("$translate: attempting translate from concept_id_1:"+omopSrcConcept.getId()+" to "+targetSystem);
		List<ParameterWrapper> params = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramConceptId1 = new ParameterWrapper(
				"Long",
				Arrays.asList("id.concept1"),
				Arrays.asList("="),
				Arrays.asList(String.valueOf(omopSrcConcept.getId())),
				"or"
				);
		params.add(paramConceptId1);
		
		ParameterWrapper paramRelationshipId = new ParameterWrapper(
				"String",
				Arrays.asList("id.relationshipId"),
				Arrays.asList("like"),
				Arrays.asList(relationshipId),
				"or"
				);
		params.add(paramRelationshipId);
		
		List<ConceptRelationship> conceptRealationships = getMyOmopService().searchWithParams(0, 0, params, null);
		if (conceptRealationships.isEmpty()) {
			logger.info("$translate: mapping information is not found ("+system+"|"+code+" to "+targetSystem+")");
			return retVal;
		}
		
		ParametersParameterComponent parameter = retVal.addParameter();
		parameter.setName("result");
		parameter.setValue(new BooleanType(true));
		
		parameter = retVal.addParameter();
		parameter.setName("match");
		
		ParametersParameterComponent partParameter = parameter.addPart();
		partParameter.setName("equivalence");
		partParameter.setValue(new CodeType("equivalent"));
		
		for (ConceptRelationship conceptRealationship: conceptRealationships) {
			// We found the mapping. Populate this information in Parameters resource.
			// concept_id_2 is the target concept.
			partParameter = parameter.addPart();
			partParameter.setName("concept");
			
			Long targetConceptId = conceptRealationship.getId().getConcept2();
			Concept targetConcept = conceptService.findById(targetConceptId);
			
			logger.debug("$translate: target concept obtained with vocabulary_id="+targetConcept.getVocabulary().getId());
			Coding targetCoding = CodeableConceptUtil.getCodingFromOmopConcept(targetConcept, getFhirOmopVocabularyMap());
			partParameter.setValue(targetCoding);
		}
		
		return retVal;
	}
}
