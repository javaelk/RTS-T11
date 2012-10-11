package uw.star.rts.technique;

import static org.junit.Assert.*;
import uw.star.rts.artifact.*;
import uw.star.rts.extraction.ArtifactFactory;
import uw.star.rts.extraction.SIRJavaFactory;
import uw.star.rts.technique.TextualDifference;
import uw.star.rts.technique.TextualDifference_Source;
import uw.star.rts.util.*;

import java.util.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TextualDifference_SourceTest {
	TextualDifference tech1,tech2;
	Program p;
	Program pPrime;
	TestSuite ts;
	@Before
	public void setUp() throws Exception {
		ArtifactFactory af =new SIRJavaFactory(); 
		Application app = af.extract("apache-xml-security");
		p=app.getProgram(ProgramVariant.orig, 0);
		pPrime=app.getProgram(ProgramVariant.orig, 1);
		ts = app.getTestSuite();
		assertEquals("total test suite size is",15,ts.getTestCases().size());
		assertEquals("total test suite size of verion 0 is",13,ts.getTestCaseByVersion(0).size());
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
	public void testPredictCost(){
		assertEquals("test predict cost", 0.77,tech2.predictPrecision(),0.01);
		
	}


}
