package uw.star.rts.technique;

import java.util.ArrayList;

import uw.star.rts.analysis.*;
import uw.star.rts.artifact.*;
import uw.star.rts.cost.CostFactor;
import uw.star.rts.extraction.ArtifactFactory;
import uw.star.rts.util.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;


public class TextualDifference_Source extends TextualDifference {

	public TextualDifference_Source(){
		super();
		this.setImplmentationName("uw.star.rts.technique.TextualDifference_Source");
	}
	
	//TODO: verify algorithm steps with reference paper.It's not clear whether coverage matrix is re-calculated for each version for reused. Should implement both options.
	@Override	
	public List<TestCase> selectTests(Program p,Program pPrime,StopWatch stopwatch){
 
			//1)construct test case - source trace
			//this trace contains test case as row, source file as columns
			stopwatch.start(CostFactor.CoverageAnalysisCost);
		    CodeCoverage entityTrace = createCoverage(p);
		    stopwatch.stop(CostFactor.CoverageAnalysisCost);
		    
			//2)compare the source files of the old and new versions of the program to identify the modified program statements
			//analyzer should be able to call ArtifactFactory to fetch result files from disk

			//TODO: currently, CodeCoverageAnalyzer has to be called first. Because extraction of CodeEntities relies on 
			// Emma xml/html reports. This can be fixed if a separate parse is used to get statement entities, method entities etc.
            stopwatch.start(CostFactor.ChangeAnalysisCost);
			ChangeAnalyzer ca = new TextualDifferencingChangeAnalysis(af,p,pPrime); //p and pPrime are always of same variant type
			ca.analyzeChange(); 
			List<SourceFileEntity> modified = ca.getModifiedSourceFiles();
			stopwatch.stop(CostFactor.ChangeAnalysisCost);

			//3)all test case that executed modified source statement are selected
			stopwatch.start(CostFactor.ApplyTechniqueCost);
			Set<TestCase> selectedTests = new HashSet<>();
			for(SourceFileEntity stm : modified)
				selectedTests.addAll(entityTrace.getLinkedEntitiesByColumn(stm)); //row contains ALL test cases for the version p. 

			//4) only select tests that were exist in P and are still applicable to pPrime
			List<TestCase> results = new ArrayList<>();
			for(TestCase tc: selectedTests)
				if(tc.isApplicabletoVersion(p.getVersionNo())&&tc.isApplicabletoVersion(pPrime.getVersionNo())) 
					results.add(tc);
			stopwatch.stop(CostFactor.ApplyTechniqueCost);
			return results;
		}
		/**
		 * Helper method as this will called twice. once for predicting cost, once for actual selecting tests
		 */
		@Override
		CodeCoverage createCoverage(Program p){
			CodeCoverageAnalyzer cca = new JacocoCodeCoverageAnalyzer(af,testapp,p,testSuite);
			CodeCoverage stmTraces =  cca.createCodeCoverage(EntityType.SOURCE);
			return stmTraces;
		}
	
		protected Collection<Entity> getModifiedCoveredEntities(List<Entity> coveredEntities,Program p,Program pPrime){
			CodeCoverageAnalyzer cca1 = new JacocoCodeCoverageAnalyzer(testapp.getRepository(),testapp,p,testapp.getTestSuite());
			if(!p.containsType(EntityType.STATEMENT))
				cca1.extractEntities(EntityType.STATEMENT);
			if(!p.containsType(EntityType.SOURCE))
				cca1.extractEntities(EntityType.SOURCE);
			
			CodeCoverageAnalyzer cca2 = new JacocoCodeCoverageAnalyzer(testapp.getRepository(),testapp,pPrime,testapp.getTestSuite());
			if(!pPrime.containsType(EntityType.STATEMENT))
				cca2.extractEntities(EntityType.STATEMENT);
			if(!pPrime.containsType(EntityType.SOURCE))
				cca2.extractEntities(EntityType.SOURCE);
			
			// find all modified SourceFile entities
			ChangeAnalyzer ca = new TextualDifferencingChangeAnalysis(af,p,pPrime); //p and pPrime are always of same variant type
			ca.analyzeChange(); 
			List<SourceFileEntity> modified = ca.getModifiedSourceFiles();
			//intersection of the two is the covered entities that are modified
			return  CollectionUtils.intersection(coveredEntities, modified);
		}
}
