package uw.star.rts.technique;

import static org.junit.Assert.*;
import uw.star.rts.artifact.*;
import uw.star.rts.cost.PrecisionPredictionModel;
import uw.star.rts.extraction.ArtifactFactory;
import uw.star.rts.extraction.SIRJavaFactory;
import uw.star.rts.technique.TextualDifference;
import uw.star.rts.technique.TextualDifference_Source;
import uw.star.rts.util.*;

import java.io.File;
import java.util.*;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TextualDifference_SourceTest {
	static TextualDifference tech1,tech2;
	static Program p;
	static Program pPrime;
	static TestSuite ts;
	@BeforeClass
	public static void setUp() throws Exception {
		ArtifactFactory af =new SIRJavaFactory(); 
		af.setExperimentRoot(PropertyUtil.getPropertyByName("config"+File.separator+"ARTSConfiguration.property",Constant.EXPERIMENTROOT));
		Application app = af.extract("apache-xml-security",TraceType.CODECOVERAGE_EMMA);
		p=app.getProgram(ProgramVariant.orig, 0);
		pPrime=app.getProgram(ProgramVariant.orig, 1);
		ts = app.getTestSuite();
		assertEquals("total test suite size is",18,ts.getTestCases().size());
		assertEquals("total test suite size of verion 0 is",16,ts.getTestCaseByVersion(0).size());
		assertEquals("total test suite size of verion 1 is",15,ts.getTestCaseByVersion(1).size());
		tech2 = new TextualDifference_Source();
		tech2.setApplication(app);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSelectTests() {
		List<TestCase> selectedTC = tech2.selectTests(p, pPrime,new StopWatch());
		assertEquals("size of selected test cases for v1",13,selectedTC.size());
		assertEquals("size of test cases applicable to v1",15,ts.getTestCaseByVersion(1).size());
	}
	@Test
	public void testPredictPrecision(){
		assertEquals("test predict cost", 0.62,tech2.predictPrecision(PrecisionPredictionModel.RWPredictor,p,pPrime),0.01);

		Map<PrecisionPredictionModel,Double> p1Prediction = tech2.predictPrecision(p,pPrime);
		assertEquals("test predict cost", 0.62,p1Prediction.get(PrecisionPredictionModel.RWPredictor),0.11);
		assertEquals("test predict cost", 0.63,p1Prediction.get(PrecisionPredictionModel.RWPredictor_RegressionTestsOnly),0.11);
		assertEquals("test predict cost", 0.77,p1Prediction.get(PrecisionPredictionModel.RWPredictor_multiChanges),0.11);
	}
}
