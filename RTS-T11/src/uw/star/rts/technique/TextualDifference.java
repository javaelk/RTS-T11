package uw.star.rts.technique;
import java.util.*;

import org.apache.commons.collections.CollectionUtils;

import uw.star.rts.analysis.*;
import uw.star.rts.artifact.*;
import uw.star.rts.cost.CostFactor;
import uw.star.rts.cost.PrecisionPredictionModel;
import uw.star.rts.cost.RWPrecisionPredictor;
import uw.star.rts.cost.RWPrecisionPredictor2;
import uw.star.rts.cost.RWPrecisionPredictor_multiChanges;
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
			return RWPrecisionPredictor.predictSelectionRate(cc, testSuite.getTestCaseByVersion(p.getVersionNo()));
		case RWPredictorRegression:
			return RWPrecisionPredictor2.predictSelectionRate(cc, testSuite.getRegressionTestCasesByVersion(p.getVersionNo()));	
		
		case RWPrecisionPredictor_multiChanges:
			//this prediction model would need to know number of changed covered entities (within covered entities)
			List<TestCase> regressionTests = testapp.getTestSuite().getRegressionTestCasesByVersion(p.getVersionNo());
	        List<Entity> regressionTestCoveredEntities = cc.getCoveredEntities(regressionTests);
			return RWPrecisionPredictor_multiChanges.predictSelectionRate(regressionTestCoveredEntities.size(), getModifiedCoveredEntities(regressionTestCoveredEntities,p,pPrime).size());
		default:
        	//log.error("unknown Precision Prediction Model : " + pm);     	
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

		Map<PrecisionPredictionModel,Double> results = new HashMap<>();

		for(PrecisionPredictionModel pm: PrecisionPredictionModel.values()){        
			switch(pm){
			case RWPredictor:
				results.put(PrecisionPredictionModel.RWPredictor,RWPrecisionPredictor.predictSelectionRate(cc, testSuite.getTestCaseByVersion(p.getVersionNo())));
				break;

			case RWPredictorRegression:
				results.put(PrecisionPredictionModel.RWPredictorRegression,RWPrecisionPredictor2.predictSelectionRate(cc, testSuite.getRegressionTestCasesByVersion(p.getVersionNo())));
				break;

			case RWPrecisionPredictor_multiChanges:
				//this prediction model would need to know number of changed covered entities (within covered entities)
				List<TestCase> regressionTests = testapp.getTestSuite().getRegressionTestCasesByVersion(p.getVersionNo());
				List<Entity> regressionTestCoveredEntities = cc.getCoveredEntities(regressionTests);
				results.put(PrecisionPredictionModel.RWPrecisionPredictor_multiChanges,RWPrecisionPredictor_multiChanges.predictSelectionRate(regressionTestCoveredEntities.size(), getModifiedCoveredEntities(regressionTestCoveredEntities,p,pPrime).size()));
				break;

			default:
				//log.error("unknown Precision Prediction Model : " + pm);     	
			}
		}
		return results;
	}
	//use actual create coverage cost to predicate total analysis cost, as for textual differencing techniques, majority of the analysis cost is on coverage analysis.
	
	public long predictAnalysisCost(){
		return createCoverageCost.getElapsedTime(CostFactor.CoverageAnalysisCost);
	}

   abstract CodeCoverage<Entity> createCoverage(Program p);
   abstract Collection<Entity> getModifiedCoveredEntities(List<Entity> coveredEntities,Program p, Program pPrime);
}
