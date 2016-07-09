package ca.uwaterloo.ece.bicer.noisefilters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jgit.diff.Edit;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.JavaASTParser;
import ca.uwaterloo.ece.bicer.utils.Utils;

public class ReBuildDefUseChain implements Filter{
	final String name="Rebuild Def-use chain";
	BIChange biChange;
	JavaASTParser biWholeCodeAST;
	JavaASTParser fixedWholeCodeAST;
	boolean isNoise=false;
	int biLineNum;
	int fixLineNum;
	String[] wholeFixCode;
	String[] wholeBiCode;
	public ReBuildDefUseChain (BIChange biChange, JavaASTParser biWholeCodeAST, JavaASTParser fixedWholeCodeAST) {
		this.biChange = biChange;
		this.biWholeCodeAST = biWholeCodeAST;
		this.fixedWholeCodeAST = fixedWholeCodeAST;
		this.wholeFixCode=fixedWholeCodeAST.getStringCode().split("\n");
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
		List<FieldDeclaration> fixFieldDecList=fixedWholeCodeAST.getFieldDeclarations();
		// varDecMap contains all of the variable declaration in the editted code
		Map<String,VariableDeclarationFragment> varDecMap=new HashMap<String,VariableDeclarationFragment>();
		
		for(FieldDeclaration fixFieldDec:fixFieldDecList){
			List<VariableDeclarationFragment> varDecFragList=fixFieldDec.fragments();
			for(VariableDeclarationFragment vdf: varDecFragList){
				int lineNum=fixedWholeCodeAST.getCompilationUnit().getLineNumber(vdf.getStartPosition())-1;
				for(Edit ed: editList){
					if(lineNum>=ed.getBeginB()&&lineNum<ed.getEndB()) varDecMap.put(vdf.getName().toString(), vdf);
					else if(lineNum<ed.getBeginB()) break; // the variable declaration is not editted
				}	
			}
		}
		
		for(MethodDeclaration md : fixedWholeCodeAST.getMethodDeclarations()){
			int startLine=fixedWholeCodeAST.getCompilationUnit().getLineNumber(md.getStartPosition());
			int endLine=fixedWholeCodeAST.getCompilationUnit().getLineNumber(md.getStartPosition()+md.getLength());
			if(edit!=null && edit.getBeginB()+1>=startLine&&edit.getEndB()<=endLine){
				if( md.getBody()!=null){
					for(Object st: md.getBody().statements()){
						if(((Statement)st).getNodeType()==ASTNode.VARIABLE_DECLARATION_STATEMENT) {
							VariableDeclarationStatement varDecStmt= (VariableDeclarationStatement) st;
							List<VariableDeclarationFragment> varDecFragList= varDecStmt.fragments();
							for(VariableDeclarationFragment vdf: varDecFragList){
								int lineNum=fixedWholeCodeAST.getCompilationUnit().getLineNumber(vdf.getStartPosition())-1;
								for(Edit ed: editList){
									if(lineNum>=ed.getBeginB()&&lineNum<ed.getEndB()) varDecMap.put(vdf.getName().toString(), vdf);
									else if(lineNum<ed.getBeginB()) break;
								}	

							}							
						}
					}
					
					for(String varName:varDecMap.keySet()){
						VariableDeclarationFragment vdf =varDecMap.get(varName);
						for(int i=edit.getBeginB();i<edit.getEndB();i++){
							if(wholeFixCode[i].indexOf(varName)!=-1){
								if(vdf.getInitializer()!=null){							
									String fixInit=vdf.getInitializer().toString();
									String fixInit2=null;
									if(fixInit.matches("^\\(\\w*\\)\\s*\\S+"))
										fixInit2=fixInit.replaceAll("^\\(\\w*\\)\\s*",""); // remove force casting
									int index=stmt.indexOf(fixInit);
									if(index!=-1){
										String fixedStmt=wholeFixCode[i];
										fixedStmt=fixedStmt.replaceAll(varName, fixInit);
										fixedStmt=Utils.removeLineComments(fixedStmt).trim();
										stmt=Utils.removeLineComments(stmt).trim();
										stmt=stmt.replaceAll("\\s", "");
										fixedStmt=fixedStmt.replaceAll("\\s", "");
										if(fixedStmt.equals(stmt)){
											int decLine=fixedWholeCodeAST.getCompilationUnit().getLineNumber(vdf.getStartPosition());
											if(decLine>=startLine&&decLine<=endLine){
												decLine--;
												if(decLine+1==i) return true;
												else if(decLine>=i) continue;
												else{
													for(int m=decLine+1;m<i;m++){
														if(!(wholeFixCode[m].matches("^\\s*(//|\\*|/\\*|\\*/|@).*")||wholeFixCode[m].matches("^\\s+"))){
															break;
														}else if(m==i-1){
															return true;
														}
													}
													continue;
												}
											}else return true;
										}
									}else if(fixInit2!=null&&stmt.indexOf(fixInit2)!=-1){
										String fixedStmt=wholeFixCode[i];
										fixedStmt=fixedStmt.replaceAll(varName, fixInit2);
										fixedStmt=Utils.removeLineComments(fixedStmt).trim();
										stmt=Utils.removeLineComments(stmt).trim();
										stmt=stmt.replaceAll("\\s", "");
										fixedStmt=fixedStmt.replaceAll("\\s", "");
										if(fixedStmt.equals(stmt)){
											int decLine=fixedWholeCodeAST.getCompilationUnit().getLineNumber(vdf.getStartPosition());
											if(decLine>=startLine&&decLine<=endLine){
												decLine--;
												if(decLine+1==i) return true;
												else if(decLine>=i) continue;
												else{
													for(int m=decLine+1;m<i;m++){
														if(!(wholeFixCode[m].matches("^\\s*(//|\\*|/\\*|\\*/|@).*")||wholeFixCode[m].matches("^\\s+"))){
															break;
														}else if(m==i-1){
															return true;
														}
													}
													continue;
												}
											}else return true;
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
