package uw.star.rts.technique;

import java.util.ArrayList;

import uw.star.rts.analysis.*;
import uw.star.rts.artifact.*;
import uw.star.rts.cost.CostFactor;
import uw.star.rts.extraction.ArtifactFactory;
import uw.star.rts.util.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class TextualDifference_Source extends TextualDifference {

	public TextualDifference_Source(){
		super();
		this.setImplmentationName("uw.star.rts.technique.TextualDifference_Source");
	}
	
	//TODO: verify algorithm steps with reference paper
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
				selectedTests.addAll(entityTrace.getLinkedEntitiesByColumn(stm)); 

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
			CodeCoverageAnalyzer cca = new EmmaCodeCoverageAnalyzer(af,testapp,p,testSuite);
			cca.extractEntities(EntityType.STATEMENT);//fix bug:always need to extract statement to establish source->statement links, need this for change analysis later 
			cca.extractEntities(EntityType.SOURCE);
			CodeCoverage stmTraces =  cca.createCodeCoverage(EntityType.SOURCE);
			return stmTraces;
		}
	
}
