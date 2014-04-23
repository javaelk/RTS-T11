package uw.star.rts.analysis;
import uw.star.rts.artifact.*;

import com.google.common.collect.Sets;

import uw.star.rts.extraction.ArtifactFactory;
import uw.star.rts.util.*;

import java.util.regex.*;

import org.slf4j.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;

// TODO : this should be re-implemented using http://code.google.com/p/google-diff-match-patch/ or http://code.google.com/p/java-diff-utils/


/**
 * Change Analysis (also known as impact analysis) is used to identify the entities that have 
 * been modified or could be affected by the modifications made to the system under test
 * 
 * This implementation of the ChangeAnalysis class is dependent on the format of diff result files. 
 * Diff result file is the output of source code comparison using Unix diff utility
 * The format of the diff result file is as follows -
 * 
 * diff {options} firstFile secondFile
 * n1[,n2] operation n3[,n4]
 * diff {options} firstFile secondFile
 * n1[,n2] operation n3[,n4]
 * .....
 * 
 * note [] indicates optional characters. 
 * n1[,n2] represents ranges of lines in the firstFile
 * n3[,n4] represents ranges of lines in the secondFile
 * operation is one of {a|c|d} which represents {add|change|delete}
 * When optional characters not present (i.e. n2/n4), there is no "," after n1/n3 => n1 operation n3
 *
 * examples:
 *diff --ignore-case --ignore-all-space --ignore-blank-lines --recursive /home/wliu/Dropbox/apache-xml-security/changes/beautifiedSrc/v0/xml-security/build/src/org/apache/xml/security/algorithms/Algorithm.java /home/wliu/Dropbox/apache-xml-security/changes/beautifiedSrc/v1/xml-security/build/src/org/apache/xml/security/algorithms/Algorithm.java
 *107c107,108
 *118,119c119,120
 *diff --ignore-case --ignore-all-space --ignore-blank-lines --recursive /home/wliu/Dropbox/apache-xml-security/changes/beautifiedSrc/v0/xml-security/build/src/org/apache/xml/security/algorithms/encryption/EncryptionMethod.java /home/wliu/Dropbox/apache-xml-security/changes/beautifiedSrc/v1/xml-security/build/src/org/apache/xml/security/algorithms/encryption/EncryptionMethod.java
 *440,447d439
 *552,554d542
 * 
 * @author Weining Liu
 *
 */
public class TextualDifferencingChangeAnalysis implements ChangeAnalyzer{

	Logger log;
	private Path diffResult;
	private Program p;

	//modified statements of all source files in program p
	private Map<String,Set<StatementEntity>> modifiedStatementsMap; //key: modification type {a|c|d},value :list of modified statements in p
	//modified source files in program p
	private Map<String,Set<SourceFileEntity>> modifiedSrcsMap; //key: modification type {a|c|d},value :list of modified sources in p

	/**
	 * Analyze changed entities in program p, 
	 * Analysis is based on diff result file generated in repository factory class(e.g. SIRJavaFactory) 
	 * This Class is only dependent on the format of the diff result file but has not knowledge of what is compared
	 * It's  ChangeAnalysis script's responsibility to ensure all files are compared (all codekinds except binary files)
	 * TODO: ideally, change analysis should be performed independent of ArtifactFactory, i.e. analyzing changes without needing to know where artifacts were extracted. 
	 * this class should trigger a diff util and analyze the results. e.g. use @see http://www.apidiff.com/en/about/  
	 * @param p
	 * @param pPrime
	 * @param diffResult - a text file contains all changes between p and pPrime.
	 */
	public TextualDifferencingChangeAnalysis(ArtifactFactory af, Program p, Program pPrime){
		log = LoggerFactory.getLogger(TextualDifferencingChangeAnalysis.class.getName());
		this.p=p;
		this.diffResult= af.getChangeResultFile(p, pPrime);
		modifiedStatementsMap = new HashMap<String,Set<StatementEntity>>();
		modifiedStatementsMap.put("a",new HashSet<StatementEntity>());
		modifiedStatementsMap.put("d",new HashSet<StatementEntity>());
		modifiedStatementsMap.put("c",new HashSet<StatementEntity>());
		modifiedSrcsMap = new HashMap<String,Set<SourceFileEntity>>();
		modifiedSrcsMap.put("c", new HashSet<SourceFileEntity>());
		modifiedSrcsMap.put("a", new HashSet<SourceFileEntity>());
		modifiedSrcsMap.put("d", new HashSet<SourceFileEntity>());
	}
	/**
	 * Analyse changes between two given programs in the constructor 
	 */
	public void analyzeChange(){
		Charset charset = Charset.forName("US-ASCII");
		try(BufferedReader reader = Files.newBufferedReader(diffResult, charset)){
			String line = null;
			SourceFileEntity sf =null;
			while((line=reader.readLine())!=null){
				String firstFile="",secondFile="",oper="";
				//line starts with diff - diff line
				if(line.matches("^diff.*")){ //diff at the beginning of line and with any chars follow it
					//log.debug("diff line : " + line );
					//split by space, find firstFile and secondFile
					for(String s: line.split(" ")){
						if(!(s.equals("diff")||s.startsWith("-"))){//NOT diff or options
							if(firstFile.isEmpty()){
								firstFile = s;
							}else if(secondFile.isEmpty()){
								secondFile =s;
							}else{
								log.error("this String should not be here " + s);
							}
						}
					}
					log.debug("firstFile : " + firstFile+ ","+ " 2nd "+secondFile);
					//find sourceFile object based on file path
					sf = getSourceFileEntityByName(p,firstFile);
					if(sf!=null) 
						modifiedSrcsMap.get("c").add(sf);
				}else if(line.matches("\\d.*[acd]\\d+.*")){//any digit (1 or more) , any char (0 or more), a/c/d, any digit (1 or more)
					//line starts with a number - operation line
					//log.debug("operation line" + line);
					//find operation, n1,n2,n3,n4
					Matcher m = Pattern.compile("[acd]").matcher(line);
					if(sf!=null&&m.find()){
						//TODO: it's possible sf==null. e.g. Emma does not report interface class ,so interface classes are not in
						//sourcefile entities, but if an interface changed, it will be in diff result file
						// this is currently not a problem as there is no test case should cover an interface anyways! 
						oper = line.substring(m.start(),m.end());
						int[] n1n2= convertToLines(line.substring(0, m.start()));
						//int[] n3n4 = convertToLines(line.substring(m.end())); don't really need this info.
						//log.debug(oper+","+Arrays.toString(n1n2));

						//update modified statements map
						Set<StatementEntity> stms = Sets.newHashSet();
						for(int i=0;i<n1n2.length;i++){//iterate list of statement numbers
							if(n1n2[i]==0&&oper.endsWith("a")) continue; //could add at line 0
							//@BUG: here I assume source->statement linkage already exist. It's only true if extractEntities(Statement is called)
							StatementEntity s =sf.getStatementByLineNumber(n1n2[i]);// need an O(1) operation here, otherwise performance would be horrible
							//@BUG, it's fine if a stm is modified but it's not executable, we don't need to add it to modifiedStm list, but we still
							//need to add the sourcefile to modified source list. 
							if(s!=null) stms.add(s);  //s could be null as a modified statement may not be executable statement and in source file we only trace executable statements.

						}
						//now stms contains a list of statements modified and executable
						modifiedStatementsMap.get(oper).addAll(stms);
					}
				}//This is not mentioned in the paper, only in v0 - deleted source, only in v1 - new source
				else if(line.matches("^Only in(.*)")){ //match Only in...
					//if a file only exist in old but not new, then every executable line in that file is considered deleted and should all be added to modified statements list
					sf = null ; //reset sf
					Matcher m = Pattern.compile("^Only in(.*)").matcher(line);
					if(m.find()){
						String fileOnlyin = m.group(1);
						//reformat and find the path to only in file
						Path fileOnlyinPath = Paths.get(fileOnlyin.trim().replaceAll(": ","/"));
						if(p.getCodeFiles(CodeKind.SOURCE).contains(fileOnlyinPath)){ //only consider this if a file exist in p but not in pPrime.
							SourceFileEntity src = getSourceFileEntityByName(p,fileOnlyinPath.toString());
							if(src!=null){
								modifiedStatementsMap.get("d").addAll(src.getExecutableStatements());
								modifiedSrcsMap.get("d").add(src);
							}
						}
					}
				}
				else{	    		//error
					log.error("can not match line " + line);
				}

			}

		}catch(IOException e){
			log.error("error in reading file " + diffResult);
			e.printStackTrace();
		}
	}

	/*	TODO: user java generic to provide a cleaner interface, for now , one method for each type
	 * public <T extends Entity> List<T> getModifiedCodeEntities(EntityType type){

		switch(type){
			case STATEMENT  : return getModifiedStatements();
			default:
				log.error("unknown entity type " + type);
				return null;
		}
	}*/

	//modified statements does not contain new statements in pPrime
	public List<StatementEntity> getModifiedStatements(){
		List<StatementEntity> stms = new ArrayList<StatementEntity>();
		if(modifiedStatementsMap.containsKey("d"))
			stms.addAll(modifiedStatementsMap.get("d"));	
		if(modifiedStatementsMap.containsKey("c"))
			stms.addAll(modifiedStatementsMap.get("c"));
		return stms;
	}

	/**
	 * 
	 * @param operCode {a|c|d}
	 * @return
	 */
	public List<StatementEntity> getModifiedStatements(String operCode){
		return new ArrayList<StatementEntity>(modifiedStatementsMap.get(operCode));
	}

	/**
	 * can NOT use statement -> sourcefile linkages to calculate modified source files.
	 * There are cases where modified statement is not an executable statement which will be dumped
	 * This is rather a limitation of extraction as all entities are constructed from coverage report
	 * and coverage report does not report non-executable statements
	 * modified source dose not contain new source files added in pPrime.
	 * @return a list of source files that have been modified
	 */
	public List<SourceFileEntity> getModifiedSourceFiles(){
		Set<SourceFileEntity> resultSet =  modifiedSrcsMap.get("c");
		resultSet.addAll(modifiedSrcsMap.get("d"));
		return new ArrayList<SourceFileEntity>(resultSet);
	}

	public List<SourceFileEntity> getModifiedSourceFiles(String operCode){
		return new ArrayList<SourceFileEntity>(modifiedSrcsMap.get(operCode));
	}
	//TODO: get modified methods and classes??
	/**
	 * convert a n1,n2 to a list of lines
	 * @param n1n2, n1[,n2], either one line or a range of lines
	 * @return n1 if one line only, else list of lines [n1,n2] inclusive
	 */
	public int[] convertToLines(String n1n2){
		int n1=-1,n2=-1;
		int commaIdx = n1n2.indexOf(",");
		if(commaIdx>-1){ //"," found
			n1 = Integer.parseInt(n1n2.substring(0,commaIdx));
			n2= Integer.parseInt(n1n2.substring(commaIdx+1));
			int[] result = new int[n2-n1+1];
			for(int i=0;i<=n2-n1;i++)
				result[i]=n1+i;
			return result;
		}else{
			n1 = Integer.parseInt(n1n2);
			int[] result = new int[1];
			result[0] = n1;
			return result;
		}
	}

	/**
	 * return null if the file name doesn't exist in program p, otherwise return the sourcefile entity
	 * 4 possible scenarios - given file doesn't exist
	 * given file is not in list of source code (regardless it's java file or not)
	 * given file is not a java file
	 * given file is java but doesn't have package name
	 * @param p
	 * @param fileName
	 * @return
	 */
	SourceFileEntity getSourceFileEntityByName(Program p,String fileName){
		SourceFileEntity sfe =null;
		int srcFileNameIdx = fileName.lastIndexOf("/");

		if(srcFileNameIdx>-1){
			String packageName = JavaFileParser.getJavaPackageName(fileName);
			if(packageName==null) { 
				log.warn("package name not found in" + fileName + " Is it a Java file?");
				return null;
			}
			String srcFileName = fileName.substring(srcFileNameIdx+1);
	        log.debug("package name:" + packageName + " , srcFileName : " + srcFileName);
			sfe = (packageName=="")?(SourceFileEntity)p.getEntityByName(EntityType.SOURCE, srcFileName):
					(SourceFileEntity)p.getEntityByName(EntityType.SOURCE, packageName+"."+srcFileName);
			if(sfe==null){
					log.warn("source file " + fileName+ "not found in this program " + p.getName()+" -v"+p.getVersionNo() +
							" as this is an interface class changed between versions but does not exist in source entity. Source entites are extracted from code coverage tool which does not report interface");
			}else{
				log.debug("source file name is : " + sfe.toString());
			}

		}else{
			log.error("there is no / at all :" + fileName);
		}
		return sfe;
		}
}
