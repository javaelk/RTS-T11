package uw.star.rts.technique;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

import uw.star.rts.analysis.ChangeAnalyzer;
import uw.star.rts.analysis.CodeCoverageAnalyzer;
import uw.star.rts.analysis.EmmaCodeCoverageAnalyzer;
import uw.star.rts.analysis.TextualDifferencingChangeAnalysis;
import uw.star.rts.artifact.Application;
import uw.star.rts.artifact.CodeCoverage;
import uw.star.rts.artifact.Entity;
import uw.star.rts.artifact.EntityType;
import uw.star.rts.artifact.Program;
import uw.star.rts.artifact.ProgramVariant;
import uw.star.rts.artifact.SourceFileEntity;
import uw.star.rts.artifact.StatementEntity;
import uw.star.rts.artifact.TestCase;
import uw.star.rts.cost.CostFactor;
import uw.star.rts.extraction.ArtifactFactory;
import uw.star.rts.util.*;
public class TextualDifference_Statement extends TextualDifference {
	
	    public TextualDifference_Statement(){
	    	super();
	    	this.setImplmentationName("uw.star.rts.technique.TextualDifference_Statement");
	    }
	    
	//TODO: verify algorithm steps with reference paper
		public List<TestCase> selectTests(Program p,Program pPrime,StopWatch sw){
			

			//1)construct test case - source statement trace
			//this trace contains test case as row, statements as columns
			sw.start(CostFactor.CoverageAnalysisCost);
			CodeCoverage stmTraces = createCoverage(p);
			sw.stop(CostFactor.CoverageAnalysisCost);
			
			//2)compare the source files of the old and new versions of the program to identify the modified program statments
			//analyzer should be able to call ArtifactFactory to fetch result files from disk

			//TODO: currently, CodeCoverageAnalyzer has to be called first. Because extraction of CodeEntities relies on 
			// Emma xml/html reports. This can be fixed if a seperate parse is used to get statement enties, method entities etc.
			sw.start(CostFactor.ChangeAnalysisCost);
			ChangeAnalyzer ca = new TextualDifferencingChangeAnalysis(af,p,pPrime); //p and pPrime are always of same variant type
			ca.analyzeChange(); 
			List<StatementEntity> modifiedStms = ca.getModifiedStatements();
			sw.stop(CostFactor.ChangeAnalysisCost);

			//3)all test case that executed modified source statement are selected
			sw.start(CostFactor.ApplyTechniqueCost);
			Set<TestCase> selectedTests = new HashSet<>();
			for(StatementEntity stm : modifiedStms)
				selectedTests.addAll(stmTraces.getLinkedEntitiesByColumn(stm)); 
			
			//4) only select tests that are still applicable to pPrime
			List<TestCase> results = new ArrayList<>();
			for(TestCase tc: selectedTests)
				if(tc.isApplicabletoVersion(p.getVersionNo())&&tc.isApplicabletoVersion(pPrime.getVersionNo()))
					results.add(tc);
			sw.stop(CostFactor.ApplyTechniqueCost);
			
			return results;
		}
		/**
		 * Helper method as this will called twice. once for predicting cost, once for actual selecting tests
		 */
		CodeCoverage createCoverage(Program p){
			CodeCoverageAnalyzer cca = new EmmaCodeCoverageAnalyzer(af,testapp,p,testSuite);
			//if(!p.containsType(EntityType.STATEMENT)) - for some reason this line would cause a null pointer exception
				cca.extractEntities(EntityType.STATEMENT);
			CodeCoverage stmTraces =  cca.createCodeCoverage(EntityType.STATEMENT);
			return stmTraces;
		}
		
		protected Collection<Entity> getModifiedCoveredEntities(List<Entity> coveredEntities,Program p, Program pPrime){
			if(!p.containsType(EntityType.STATEMENT)){
				CodeCoverageAnalyzer cca1 = new EmmaCodeCoverageAnalyzer(testapp.getRepository(),testapp,p,testapp.getTestSuite());
				cca1.extractEntities(EntityType.STATEMENT);
			}
			if(!pPrime.containsType(EntityType.STATEMENT)){
				CodeCoverageAnalyzer cca2 = new EmmaCodeCoverageAnalyzer(testapp.getRepository(),testapp,pPrime,testapp.getTestSuite());
				cca2.extractEntities(EntityType.STATEMENT);
			}
			
			// find all modified SourceFile entities
			ChangeAnalyzer ca = new TextualDifferencingChangeAnalysis(af,p,pPrime); //p and pPrime are always of same variant type
			ca.analyzeChange(); 
			List<StatementEntity> modified = ca.getModifiedStatements();
			//intersection of the two is the covered entities that are modified
			return  CollectionUtils.intersection(coveredEntities, modified);
		}
}
