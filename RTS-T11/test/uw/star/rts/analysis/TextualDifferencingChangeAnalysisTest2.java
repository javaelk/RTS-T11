package uw.star.rts.analysis;

import static org.junit.Assert.*;



import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import uw.star.rts.analysis.CodeCoverageAnalyzer;
import uw.star.rts.analysis.EmmaCodeCoverageAnalyzer;
import uw.star.rts.analysis.TextualDifferencingChangeAnalysis;
import uw.star.rts.artifact.*;
import uw.star.rts.extraction.SIRJavaFactory;

public class TextualDifferencingChangeAnalysisTest2 {

	String EXPERIMENTROOT = "C:\\Documents and Settings\\wliu\\My Documents\\personal\\Dropbox";
	String appname = "apache-xml-security";
	TextualDifferencingChangeAnalysis ca1,ca2;
	Program p0,p1,p2;
	

	@Before
	public void setUp() throws Exception {
		SIRJavaFactory sir = new SIRJavaFactory();
		sir.setExperimentRoot(EXPERIMENTROOT);
		Application testapp = sir.extract(appname);
		p0 = testapp.getProgram(ProgramVariant.orig, 0);
		p1= testapp.getProgram(ProgramVariant.orig, 1);
		p2= testapp.getProgram(ProgramVariant.orig, 2);
		ca1 = new TextualDifferencingChangeAnalysis(sir, p0,p1);
		ca2 = new TextualDifferencingChangeAnalysis(sir, p1,p2);
		CodeCoverageAnalyzer cca = new EmmaCodeCoverageAnalyzer(sir,testapp,p0,testapp.getTestSuite());
		//this will populate statementeentities in source file, required before comparing two source files for line differences.
		cca.extractEntities(EntityType.SOURCE);
		cca.extractEntities(EntityType.CLAZZ);
		cca.extractEntities(EntityType.METHOD);
		cca.extractEntities(EntityType.STATEMENT); 
		
		CodeCoverageAnalyzer cca2 = new EmmaCodeCoverageAnalyzer(sir,testapp,p1,testapp.getTestSuite());
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
		String srcfilename = "/home/wliu/Dropbox/apache-xml-security/changes/beautifiedSrc/v1/xml-security/build/src/org/apache/xml/security/utils/XPathFuncHereAPI.java";
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
		String srcfilename = "/home/wliu/Dropbox/apache-xml-security/changes/beautifiedSrc/v1/xml-security/build/src/org/apache/xml/security/utils/XPathFuncHereAPI.java";
		SourceFileEntity src = ca1.getSourceFileEntityByName(p0,srcfilename);
		assertEquals("test file name","XPathFuncHereAPI.java",src.getSourceFileName());
		assertEquals("test package name","org.apache.xml.security.utils",src.getPackageName());
		assertEquals("test src contains statement entities",29,src.getExecutableStatements().size());

	}


}
