package ca.uwaterloo.ece.bicer.noisefilters;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jgit.diff.Edit;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.JavaASTParser;
import ca.uwaterloo.ece.bicer.utils.Utils;

public class RemoveDefUseChain implements Filter{
	final String name="Remove Def-use chain";
	BIChange biChange;
	JavaASTParser biWholeCodeAST;
	JavaASTParser fixedWholeCodeAST;
	boolean isNoise=false;
	int biLineNum;
	int fixLineNum;
	String[] wholeFixCode;
	String[] wholeBiCode;
	
	public RemoveDefUseChain (BIChange biChange, JavaASTParser biWholeCodeAST, JavaASTParser fixedWholeCodeAST) {
		this.biChange = biChange;
		this.biWholeCodeAST = biWholeCodeAST;
		this.fixedWholeCodeAST = fixedWholeCodeAST;
		this.wholeFixCode=fixedWholeCodeAST.getStringCode().split("\n");
		this.wholeBiCode=biWholeCodeAST.getStringCode().split("\n");
		isNoise = filterOut();
	}
	
	@Override
	public boolean filterOut() {
		// TODO Auto-generated method stub
		if(!biChange.getIsAddedLine()) return false;
		
		String biSource = biWholeCodeAST.getStringCode();
		int startPositionOfBILine = Utils.getStartPosition(biSource,biChange.getLineNumInPrevFixRev());
		
		if(startPositionOfBILine <0){
			System.err.println("Warning: line does not exist in BI source code " + ": " + biChange.getLine());
		}
		Edit edit=biChange.getEdit();
		if(edit==null) return false;
		String stmt=biChange.getLine();
		stmt=stmt.trim();
		if(stmt.matches("(//|\\*|/\\*|\\*/|@).*")) return false; //JavaDoc or comments
		biLineNum = biChange.getLineNumInPrevFixRev();
		fixLineNum = biChange.getEdit().getBeginB()+1;
		List<Edit> editList=biChange.getEditListFromDiff();
		Collections.sort(editList, new Comparator<Edit>(){
			@Override
			public int compare(Edit o1, Edit o2) {
				return o1.getBeginB()-o2.getBeginB();	 
			}	
		});
		List<FieldDeclaration> biFieldDecList=biWholeCodeAST.getFieldDeclarations();
		// varDecMap contains all of the variable declaration in the editted code
		Map<String,VariableDeclarationFragment> varDecMap=new HashMap<String,VariableDeclarationFragment>();
		
		for(FieldDeclaration biFieldDec:biFieldDecList){
			List<VariableDeclarationFragment> varDecFragList=biFieldDec.fragments();
			for(VariableDeclarationFragment vdf: varDecFragList){
				int lineNum=biWholeCodeAST.getCompilationUnit().getLineNumber(vdf.getStartPosition())-1;
				for(Edit ed: editList){
					if(lineNum>=ed.getBeginA()&&lineNum<ed.getEndA()) varDecMap.put(vdf.getName().toString(), vdf);
					else if(lineNum<ed.getBeginA()) break; // the variable declaration is not editted
				}	
			}
		}
		
		for(MethodDeclaration md : biWholeCodeAST.getMethodDeclarations()){
			int startLine=biWholeCodeAST.getCompilationUnit().getLineNumber(md.getStartPosition());
			int endLine=biWholeCodeAST.getCompilationUnit().getLineNumber(md.getStartPosition()+md.getLength());
			if(biLineNum>=startLine&&biLineNum<=endLine){
				if( md.getBody()!=null){
					for(Object st: md.getBody().statements()){
						//ADD assignemnt statmentss  ,
						if(((Statement)st).getNodeType()==ASTNode.VARIABLE_DECLARATION_STATEMENT) {
							VariableDeclarationStatement varDecStmt= (VariableDeclarationStatement) st;
							List<VariableDeclarationFragment> varDecFragList= varDecStmt.fragments();
							for(VariableDeclarationFragment vdf: varDecFragList){
								int lineNum=biWholeCodeAST.getCompilationUnit().getLineNumber(vdf.getStartPosition())-1;
								for(Edit ed: editList){
									if(lineNum>=ed.getBeginA()&&lineNum<ed.getEndA()) varDecMap.put(vdf.getName().toString(), vdf);
									else if(lineNum<ed.getBeginA()) break;
								}	
							}
						}
					}
					boolean isVariableDeclaration=false;
					for(String vdfName:varDecMap.keySet()){
						if(biWholeCodeAST.getCompilationUnit().getLineNumber(varDecMap.get(vdfName).getStartPosition())==biLineNum){
							// CASE1: the bi line is the variable declaration
							if(varDecMap.get(vdfName).getInitializer()!=null){
								String initializer=varDecMap.get(vdfName).getInitializer().toString();
								String initializer2=null;
								if(initializer.matches("^\\(\\w*\\)\\s*\\S+"))
									initializer2=initializer.replaceAll("^\\(\\w*\\)\\s*",""); // remove force casting
								isVariableDeclaration=true;
								for(Edit ed:editList){
									if(ed!=null && (ed.getBeginA()>=startLine-1&&ed.getEndA()<=endLine)){
										//the edit is located within the method
										for(int i=ed.getBeginA();i<ed.getEndA();i++){
											stmt=wholeBiCode[i];
											if(stmt.indexOf(vdfName)!=-1){ // contain the variable name
												for(int j=ed.getBeginB();j<ed.getEndB();j++){
													String fixedStmt=wholeFixCode[j];
													if(fixedStmt.indexOf(initializer)!=-1){ // fixedStmt contain initializer
														fixedStmt=Utils.removeLineComments(fixedStmt).trim();
														fixedStmt=fixedStmt.replaceAll("\\s", "");	
														stmt=stmt.replaceAll(vdfName, initializer);
														stmt=Utils.removeLineComments(stmt).trim();
														stmt=stmt.replaceAll("\\s", "");
														if(stmt.equals(fixedStmt)) return true;
													}else if(initializer2!=null&&fixedStmt.indexOf(initializer2)!=-1){//????????add dealing with force casting?????????
														fixedStmt=Utils.removeLineComments(fixedStmt).trim();
														fixedStmt=fixedStmt.replaceAll("\\s", "");	
														stmt=stmt.replaceAll(vdfName, initializer2);
														stmt=Utils.removeLineComments(stmt).trim();
														stmt=stmt.replaceAll("\\s", "");
														if(stmt.equals(fixedStmt)) return true;											
													}
												}
											}

										}
									}
								}								
							}

						}
							
					}
					//CASE 2: the bi line is the variable using statement
					if(!isVariableDeclaration){ 
						stmt=biChange.getLine();
						for(String vdfName:varDecMap.keySet()){
							if(varDecMap.get(vdfName).getInitializer()!=null){
								String initializer=varDecMap.get(vdfName).getInitializer().toString();
								String initializer2=null;
								if(initializer.matches("^\\(\\w*\\)\\s*\\S+"))
									initializer2=initializer.replaceAll("^\\(\\w*\\)\\s*",""); // remove force casting
								if(stmt.indexOf(vdfName)!=-1){ // check whether biChange line contain variable
									for(int i=edit.getBeginB();i<edit.getEndB();i++){
										String fixedStmt=wholeFixCode[i];
										if(fixedStmt.indexOf(initializer)!=-1){ // fixedStmt contain initializer
											fixedStmt=Utils.removeLineComments(fixedStmt).trim();
											fixedStmt=fixedStmt.replaceAll("\\s", "");	
											stmt=stmt.replaceAll(vdfName, initializer);
											stmt=Utils.removeLineComments(stmt).trim();
											stmt=stmt.replaceAll("\\s", "");
											if(stmt.equals(fixedStmt)) return true;
										}else if(initializer2!=null&&fixedStmt.indexOf(initializer2)!=-1){//????????add dealing with force casting?????????
											fixedStmt=Utils.removeLineComments(fixedStmt).trim();
											fixedStmt=fixedStmt.replaceAll("\\s", "");	
											stmt=stmt.replaceAll(vdfName, initializer2);
											stmt=Utils.removeLineComments(stmt).trim();
											stmt=stmt.replaceAll("\\s", "");
											if(stmt.equals(fixedStmt)) return true;											
										}
									}
								}							
							}

						}
					}
					
				}
			}
				

		}


		return false;
	}

	@Override
	public boolean isNoise() {
		// TODO Auto-generated method stub
		return isNoise;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}

}
