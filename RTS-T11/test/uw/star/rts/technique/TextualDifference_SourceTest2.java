package uw.star.rts.technique;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import uw.star.rts.artifact.Application;
import uw.star.rts.artifact.CodeCoverage;
import uw.star.rts.artifact.Entity;
import uw.star.rts.artifact.Program;
import uw.star.rts.artifact.ProgramVariant;
import uw.star.rts.artifact.TraceType;
import uw.star.rts.extraction.ArtifactFactory;
import uw.star.rts.extraction.SIRJavaFactory;
import uw.star.rts.util.Constant;
import uw.star.rts.util.PropertyUtil;

public class TextualDifference_SourceTest2 {

    @Test
    public void testPopulateEntityChangeFrequency(){
		ArtifactFactory aFactory =new SIRJavaFactory(); 
		aFactory.setExperimentRoot(PropertyUtil.getPropertyByName("config"+File.separator+"ARTSConfiguration.property",Constant.EXPERIMENTROOT));
		Application app = aFactory.extract("jacoco-core-snapshots-TC",TraceType.CODECOVERAGE_JACOCO);
		Program p00=app.getProgram(ProgramVariant.orig, 0);
		Program p10=app.getProgram(ProgramVariant.orig, 1);
		TextualDifference_Source tech00 = new TextualDifference_Source();
		tech00.setApplication(app);
		CodeCoverage<Entity> cc = tech00.createCoverage(p00);
		tech00.populateEntityChangeFrequency(p00);
    }

}
