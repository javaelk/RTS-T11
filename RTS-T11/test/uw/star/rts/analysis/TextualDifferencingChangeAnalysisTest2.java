package uw.star.rts.analysis;

import static org.junit.Assert.*;



import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import uw.star.rts.analysis.CodeCoverageAnalyzer;
import uw.star.rts.analysis.TextualDifferencingChangeAnalysis;
import uw.star.rts.artifact.*;
import uw.star.rts.extraction.SIRJavaFactory;
import uw.star.rts.util.Constant;
import uw.star.rts.util.PropertyUtil;

public class TextualDifferencingChangeAnalysisTest2 {
    //TODO: read experiment root from environment variable instead of hard code it in every test classes! 
	static String appname = "apache-xml-security-releases-TC";
	static TextualDifferencingChangeAnalysis ca1,ca2;
	static Program p0,p1,p2;
	static String EXPERIMENT_ROOT;
	

	@BeforeClass
	public static void setUp() throws Exception {
		SIRJavaFactory sir = new SIRJavaFactory();
		EXPERIMENT_ROOT = PropertyUtil.getPropertyByName("config"+File.separator+"ARTSConfiguration.property",Constant.EXPERIMENTROOT);
		sir.setExperimentRoot(EXPERIMENT_ROOT);
		Application testapp = sir.extract(appname,TraceType.CODECOVERAGE_JACOCO);
		p0 = testapp.getProgram(ProgramVariant.orig, 0);
		p1= testapp.getProgram(ProgramVariant.orig, 1);
		p2= testapp.getProgram(ProgramVariant.orig, 2);
		ca1 = new TextualDifferencingChangeAnalysis(sir, p0,p1);
		ca2 = new TextualDifferencingChangeAnalysis(sir, p1,p2);
		CodeCoverageAnalyzer cca = new JacocoCodeCoverageAnalyzer(sir,testapp,p0,testapp.getTestSuite());
		//this will populate statementeentities in source file, required before comparing two source files for line differences.
		cca.extractEntities(EntityType.SOURCE);
		cca.extractEntities(EntityType.CLAZZ);
		cca.extractEntities(EntityType.METHOD);
		cca.extractEntities(EntityType.STATEMENT); 
		
		CodeCoverageAnalyzer cca2 = new JacocoCodeCoverageAnalyzer(sir,testapp,p1,testapp.getTestSuite());
		//this will populate statementeentities in source file, required before comparing two source files for line differences.
		cca2.extractEntities(EntityType.SOURCE);
		cca2.extractEntities(EntityType.CLAZZ);
		cca2.extractEntities(EntityType.METHOD);
		cca2.extractEntities(EntityType.STATEMENT);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test 
	public void convertToLinesTest(){
		assertEquals("test n1 only",105,ca1.convertToLines("105")[0]);
		assertEquals("test n1 only",1,ca1.convertToLines("105").length);
		assertEquals("test n1 only",0,ca1.convertToLines("0")[0]);
		assertEquals("test n1,n2",110,ca1.convertToLines("110,112")[0]);
		assertEquals("test n1,n2",3,ca1.convertToLines("110,112").length);
		assertEquals("test n1,n2",112,ca1.convertToLines("110,112")[2]);
	}
	
	@Test
	public void analyzeTest1(){
		String srcfilename = EXPERIMENT_ROOT+"/apache-xml-security/versions.alt/orig/v1/xml-security/build/src/org/apache/xml/security/utils/XPathFuncHereAPI.java";
		SourceFileEntity src = ca1.getSourceFileEntityByName(p0,srcfilename);
		ca1.analyzeChange();
		List<StatementEntity> mod = ca1.getModifiedStatements();
		assertEquals("test number of modified statements", 893, mod.size());
		p0.getCodeEntities(EntityType.SOURCE);
		StatementEntity modifiedStatmentInSrc =null;
		for(StatementEntity stm: mod)
			if(stm.getSourceFileEntity().equals(src))
		assertEquals("test changed statement for source file XPathFuncHereAPI",343,modifiedStatmentInSrc.getLineNumber());
		List<SourceFileEntity> changedSrcs = ca1.getModifiedSourceFiles();
		assertEquals("test number of modified source files", 70, changedSrcs.size());
	    SourceFileEntity unChangedSrc = new SourceFileEntity(p0,"org.apache.xml.security.signature","InvalidSignatureValueException.java",Paths.get(srcfilename));
		assertFalse("org.apache.xml.security.signature.InvalidSignatureValueException.java is not modified",changedSrcs.contains(unChangedSrc));
	}
	
	@Test
	public void analyzeTest2(){
		ca2.analyzeChange();
		List<StatementEntity> mod = ca2.getModifiedStatements();
		assertEquals("test number of modified statements", 668, mod.size());
	}
	@Test
	public void parseSourceFileTest(){
		String srcfilename =EXPERIMENT_ROOT+"/apache-xml-security-releases-TC/versions.alt/orig/v1/xml-security/build/src/org/apache/xml/security/utils/XPathFuncHereAPI.java";
		SourceFileEntity src = ca1.getSourceFileEntityByName(p0,srcfilename);
		assertEquals("test file name","XPathFuncHereAPI.java",src.getSourceFileName());
		assertEquals("test package name","org.apache.xml.security.utils",src.getPackageName());
		assertEquals("test src contains statement entities",29,src.getExecutableStatements().size());

	}
	
	@Test
	public void anayzeTest3(){
		SIRJavaFactory sir2 = new SIRJavaFactory();
		EXPERIMENT_ROOT = PropertyUtil.getPropertyByName("config"+File.separator+"ARTSConfiguration.property",Constant.EXPERIMENTROOT);
		sir2.setExperimentRoot(EXPERIMENT_ROOT);
		Application testapp = sir2.extract("jacoco-core-releases-TC",TraceType.CODECOVERAGE_JACOCO);
		Program p7 = testapp.getProgram(ProgramVariant.orig, 7);
		Program p8= testapp.getProgram(ProgramVariant.orig, 8);
		
		CodeCoverageAnalyzer cca7= new JacocoCodeCoverageAnalyzer(sir2,testapp,p7,testapp.getTestSuite());
		//this will populate statementeentities in source file, required before comparing two source files for line differences.
		cca7.extractEntities(EntityType.SOURCE);
		cca7.extractEntities(EntityType.CLAZZ);
		cca7.extractEntities(EntityType.METHOD);
		cca7.extractEntities(EntityType.STATEMENT); 
		
		CodeCoverageAnalyzer cca8 = new JacocoCodeCoverageAnalyzer(sir2,testapp,p8,testapp.getTestSuite());
		//this will populate statementeentities in source file, required before comparing two source files for line differences.
		cca8.extractEntities(EntityType.SOURCE);
		cca8.extractEntities(EntityType.CLAZZ);
		cca8.extractEntities(EntityType.METHOD);
		cca8.extractEntities(EntityType.STATEMENT);
		
		
		TextualDifferencingChangeAnalysis ta = new TextualDifferencingChangeAnalysis(sir2, p7,p8);
		ta.analyzeChange();
		List<SourceFileEntity> deletedSrc = ta.getModifiedSourceFiles("d");
		boolean found = false;
		for(SourceFileEntity src: deletedSrc)
			if(src.getName().equals("org.jacoco.core.internal.instr.JumpProbe.java"))
				found = true;
		assertTrue(found);
	}


}
