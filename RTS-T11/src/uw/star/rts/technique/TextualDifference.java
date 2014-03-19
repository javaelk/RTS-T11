package uw.star.rts.technique;
import java.nio.file.Paths;
import java.util.*;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multiset;

import uw.star.rts.analysis.*;
import uw.star.rts.artifact.*;
import uw.star.rts.changeHistory.ChangeHistoryParser;
import uw.star.rts.changeHistory.GitChangeHistoryParser;
import uw.star.rts.changeHistory.SvnChangeHistoryParser;
import uw.star.rts.cost.CostFactor;
import uw.star.rts.cost.PrecisionPredictionModel;
import uw.star.rts.cost.RWPredictor;
import uw.star.rts.cost.RWPredictor_RegressionTestsOnly;
import uw.star.rts.cost.RWPredictor_multiChanges2;
import uw.star.rts.cost.RWPredictor_multiChanges;
import uw.star.rts.cost.WeightedRWPrecisionPredictor;
import uw.star.rts.extraction.*;
import uw.star.rts.util.*;


/**
 * Change analysis and coverage analysis should be called from technique so that cost of applying techniques can be properly measured
 * i.e. you won't need to analyze changes if the technique doesn't need it
 * @author wliu
 * 
 *
 */
public abstract class TextualDifference extends Technique{
	ArtifactFactory af;
	TestSuite testSuite;
	StopWatch createCoverageCost;
	static Logger log = LoggerFactory.getLogger(TextualDifference.class.getName());
	
	public void setApplication(Application app){
		this.testapp=app;
		this.af= testapp.getRepository();
		this.testSuite = testapp.getTestSuite();
		createCoverageCost = new StopWatch();
	}
	/**
	 * Predict test selection rate for pPrime, based on information of p using prediction model pm.
	 * @return
	 */
	@Override
	public double predictPrecision(PrecisionPredictionModel pm,Program p,Program pPrime){
		createCoverageCost.start(CostFactor.CoverageAnalysisCost);
		CodeCoverage<Entity> cc = createCoverage(p);
		createCoverageCost.stop(CostFactor.CoverageAnalysisCost);

        
		switch(pm){
		case RWPredictor:
			return RWPredictor.predictSelectionRate(cc, testSuite.getTestCaseByVersion(p.getVersionNo()));
		case RWPredictor_RegressionTestsOnly:
			return RWPredictor_RegressionTestsOnly.predictSelectionRate(cc, testSuite.getRegressionTestCasesByVersion(p.getVersionNo()));	
		
		case RWPredictor_multiChanges:
			//this prediction model would need to know number of changed covered entities (within covered entities)
			List<TestCase> regressionTests = testapp.getTestSuite().getRegressionTestCasesByVersion(p.getVersionNo());
	        List<Entity> regressionTestCoveredEntities = cc.getCoveredEntities(regressionTests);
			return RWPredictor_multiChanges.predictSelectionRate(regressionTestCoveredEntities.size(), getModifiedCoveredEntities(regressionTestCoveredEntities,p,pPrime).size());
		
		case RWPredictor_multiChanges2:
			return RWPredictor_multiChanges2.predictSelectionRate(cc, testSuite.getRegressionTestCasesByVersion(p.getVersionNo()),getNumModifiedEntities(p,pPrime));
			
		default:
        	log.warn("Prediction Model " + pm + " is not implemented");     	
		}
		return Double.MIN_VALUE;
	}
	
	/**
	 * this is a performance improve version for evaluation purpose, run all prediction models at the same time to save time building coverage matrix 
	 */
	@Override
	public Map<PrecisionPredictionModel,Double> predictPrecision(Program p,Program pPrime){
		createCoverageCost.start(CostFactor.CoverageAnalysisCost);
		CodeCoverage<Entity> cc = createCoverage(p);
		createCoverageCost.stop(CostFactor.CoverageAnalysisCost);
		
		List<TestCase> regressionTests = testapp.getTestSuite().getRegressionTestCasesByVersion(p.getVersionNo());
		List<Entity> regressionTestCoveredEntities = cc.getCoveredEntities(regressionTests);
		
		Map<PrecisionPredictionModel,Double> results = new HashMap<>();

		for(PrecisionPredictionModel pm: PrecisionPredictionModel.values()){        
			switch(pm){
			case RWPredictor:
				results.put(PrecisionPredictionModel.RWPredictor,RWPredictor.predictSelectionRate(cc, testSuite.getTestCaseByVersion(p.getVersionNo())));
				break;

			case RWPredictor_RegressionTestsOnly:
				results.put(PrecisionPredictionModel.RWPredictor_RegressionTestsOnly,RWPredictor_RegressionTestsOnly.predictSelectionRate(cc, testSuite.getRegressionTestCasesByVersion(p.getVersionNo())));
				break;

			case RWPredictor_multiChanges:
				//this prediction model would need to know number of changed covered entities (within covered entities)
				int mce = getModifiedCoveredEntities(regressionTestCoveredEntities,p,pPrime).size();
				results.put(PrecisionPredictionModel.RWPredictor_multiChanges,RWPredictor_multiChanges.predictSelectionRate(regressionTestCoveredEntities.size(),mce ));
				break;
				
			case RWPredictor_multiChanges2:
				results.put(PrecisionPredictionModel.RWPredictor_multiChanges2,RWPredictor_multiChanges2.predictSelectionRate(cc, testSuite.getRegressionTestCasesByVersion(p.getVersionNo()),getNumModifiedEntities(p,pPrime)));
                break;
                
    		case WeightedRWPrecisionPredictor:
			populateEntityChangeFrequency(p);
			results.put(PrecisionPredictionModel.WeightedRWPrecisionPredictor,WeightedRWPrecisionPredictor.predictSelectionRate(cc, testSuite.getRegressionTestCasesByVersion(p.getVersionNo())));
			break;

			default:
	        	log.warn("Prediction Model " + pm + " is not implemented");        	
			}
		}
		return results;
	}
	//use actual create coverage cost to predicate total analysis cost, as for textual differencing techniques, majority of the analysis cost is on coverage analysis.
	
	public long predictAnalysisCost(){
		return createCoverageCost.getElapsedTime(CostFactor.CoverageAnalysisCost);
	}

	//this is a hack for Jacoco.core only, each entity in given program p is populated with a calculated change frequency
	 void populateEntityChangeFrequency(Program p){
		Map<SourceFileEntity,Integer> frequencyMap= new HashMap<>(); //each entity in p and it's number of changes 
		
/*		GitChangeHistoryParser parser = new GitChangeHistoryParser(Paths.get(
				"/media/data/wliu/sir/jacoco-core-snapshots-TC/changeHistory/jacoco_core.changehistory.txt")); 
		Multiset<String> ms = parser.getChangeFrequency(EntityType.SOURCE, "9e9cfaac707f36f013a10a4dd089f742a9aa149b");
*/		ChangeHistoryParser parser = new SvnChangeHistoryParser(Paths.get(
				"/media/data/wliu/sir/apache-solr-core-snapshots-TC/changeHistory/changeHistory.txt")); 
		Multiset<String> ms = parser.getChangeFrequency(EntityType.SOURCE, "r1487602"); 

		for(Entity src: p.getCodeEntities(EntityType.SOURCE)){
			//StringBuilder convertedFilePath = new StringBuilder().append("org.jacoco.core/src/");
			StringBuilder convertedFilePath = new StringBuilder().append("/lucene/dev/trunk/solr/core/src/java/");
			String srcName = src.getName().substring(0, src.getName().lastIndexOf("."));
			convertedFilePath.append(srcName.replaceAll("\\.", "/"))
			                 .append(".java");
			log.debug("trying to find number of changes of " + convertedFilePath  + "count is " + ms.count(convertedFilePath.toString()));
			frequencyMap.put((SourceFileEntity)src, ms.count(convertedFilePath.toString()));
		}
		int sum=0;
		for(SourceFileEntity sfe: frequencyMap.keySet())
		     sum += frequencyMap.get(sfe);
		
		for(SourceFileEntity sfe: frequencyMap.keySet())
			sfe.setChangeFrequency((frequencyMap.get(sfe)*1.0)/sum);
		
	}
   abstract CodeCoverage<Entity> createCoverage(Program p);
   abstract Collection<Entity> getModifiedCoveredEntities(List<Entity> coveredEntities,Program p, Program pPrime);
   abstract int getNumModifiedEntities(Program p,Program pPrime);
}
