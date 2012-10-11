package uw.star.rts.analysis;

import java.util.List;

import uw.star.rts.artifact.*;

/**
 * Change Analysis (also known as impact analysis) is used to identify the entities that have 
 * been modified or could be affected by the modifications made to the system under test
 * @author Weining Liu
 */
public interface ChangeAnalyzer {
//TODO: add interface methods 
	public void analyzeChange();
	
	/*
	 * TODO: user java generic to provide a cleaner interface, for now , one method for each type
	 * public <T extends Entity> List<T> getModifiedCodeEntities(List<T> emptypList);
	 */
	
	public List<StatementEntity> getModifiedStatements();
	public List<SourceFileEntity> getModifiedSourceFiles();
/*	TODO: //for cost predicton
	public void getEstimatedCost();
	//for evaluation
    public void getActualCost();*/
		
}
