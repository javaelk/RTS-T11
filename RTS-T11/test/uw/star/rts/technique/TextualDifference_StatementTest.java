package uw.star.rts.technique;

import static org.junit.Assert.*;
import uw.star.rts.artifact.*;
import uw.star.rts.cost.PrecisionPredictionModel;
import uw.star.rts.extraction.ArtifactFactory;
import uw.star.rts.extraction.SIRJavaFactory;
import uw.star.rts.technique.TextualDifference;
import uw.star.rts.technique.TextualDifference_Statement;
import uw.star.rts.util.*;

import java.io.File;
import java.util.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TextualDifference_StatementTest {
	TextualDifference tech1,tech2;
	Program p;
	Program pPrime;
	TestSuite ts;
	@Before
	public void setUp() throws Exception {
		ArtifactFactory af =new SIRJavaFactory(); 
		af.setExperimentRoot(PropertyUtil.getPropertyByName("config"+File.separator+"ARTSConfiguration.property",Constant.EXPERIMENTROOT));
		Application app = af.extract("apache-xml-security");
		p=app.getProgram(ProgramVariant.orig, 0);
		pPrime=app.getProgram(ProgramVariant.orig, 1);
		ts = app.getTestSuite();
		tech1 = new TextualDifference_Statement();
		tech1.setApplication(app);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSelectTests() {
		List<TestCase> selectedTC = tech1.selectTests(p, pPrime,new StopWatch());
		assertEquals("size of selected test cases for v1",13,selectedTC.size());
		assertEquals("size of test cases applicable to v1",15,ts.getTestCaseByVersion(1).size());
	}
	@Test
	public void testPredictCost(){
		assertEquals("test predict cost", 0.32,tech1.predictPrecision(PrecisionPredictionModel.RWPredictor),0.11);
		assertEquals("test predict cost", 0.32,tech1.predictPrecision(PrecisionPredictionModel.RWPredictorRegression),0.11);
		assertEquals("test predict cost", 0.32,tech1.predictPrecision(PrecisionPredictionModel.RWPrecisionPredictor_multiChanges),0.11);
	}

}
