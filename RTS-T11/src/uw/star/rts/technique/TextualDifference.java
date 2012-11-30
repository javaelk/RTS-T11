package uw.star.rts.technique;
import java.util.*;

import uw.star.rts.analysis.*;
import uw.star.rts.artifact.*;
import uw.star.rts.cost.CostFactor;
import uw.star.rts.cost.PrecisionPredictionModel;
import uw.star.rts.cost.RWPrecisionPredictor;
import uw.star.rts.cost.RWPrecisionPredictor2;
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
	 * Cost should be predicted based on first version of the program. 
	 * Test Suite should only contains test applicable to the first version
	 * @return
	 */
	@Override
	public double predictPrecision(PrecisionPredictionModel pm){
		Program p = testapp.getProgram(ProgramVariant.orig, 0);
		createCoverageCost.start(CostFactor.CoverageAnalysisCost);
		CodeCoverage cc = createCoverage(p);
		createCoverageCost.stop(CostFactor.CoverageAnalysisCost);
		
		switch(pm){
		case RWPredictor:
			return RWPrecisionPredictor.getPredicatedPercetageOfTestCaseSelected(cc, testSuite.getTestCaseByVersion(0));
		case RWPredictorRegression:
			return RWPrecisionPredictor2.getPredicatedPercetageOfTestCaseSelected(cc, testSuite.getRegressionTestCasesByVersion(0));	
		default:
        	//log.error("unknow Precision Prediction Model : " + pm);     	
		}
		return Double.MIN_VALUE;
	}
	
	//use actual create coverage cost to predicate total analysis cost, as for textual differencing techniques, majority of the analysis cost is on coverage analysis.
	
	public long predictAnalysisCost(){
		return createCoverageCost.getElapsedTime(CostFactor.CoverageAnalysisCost);
	}

   abstract CodeCoverage createCoverage(Program p);
}
