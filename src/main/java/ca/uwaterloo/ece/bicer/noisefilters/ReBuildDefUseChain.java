package ca.uwaterloo.ece.bicer.noisefilters;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
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
	BitSet editFlag;
	public ReBuildDefUseChain (BIChange biChange, JavaASTParser biWholeCodeAST, JavaASTParser fixedWholeCodeAST) {
		this.biChange = biChange;
		this.biWholeCodeAST = biWholeCodeAST;
		this.fixedWholeCodeAST = fixedWholeCodeAST;
		this.wholeBiCode=biWholeCodeAST.getStringCode().split("\n");
		this.wholeFixCode=fixedWholeCodeAST.getStringCode().split("\n");
		this.editFlag= new BitSet(wholeFixCode.length);  // the flag to indicate whether this line is changed line in wholeFixCode
		this.isNoise = filterOut();
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
		if(stmt.matches("^(//|\\*|/\\*|\\*/|@).*")) return false; //JavaDoc or comments
		String initStmt=stmt;
		biLineNum = biChange.getLineNumInPrevFixRev();
		fixLineNum = biChange.getEdit().getBeginB()+1;
		List<Edit> editList=biChange.getEditListFromDiff();
		Collections.sort(editList, new Comparator<Edit>(){
			@Override
			public int compare(Edit o1, Edit o2) {
				return o1.getBeginB()-o2.getBeginB();	 
			}	
		});
		for(Edit ed:editList){
			for(int i=ed.getBeginB();i<ed.getEndB();i++){
				editFlag.set(i);
			}
		}
		List<FieldDeclaration> fixFieldDecList=fixedWholeCodeAST.getFieldDeclarations();
		// varDecMap contains all of the variable declaration in the editted code
		Map<String,VariableDeclarationFragment> varDecMap=new HashMap<String,VariableDeclarationFragment>();
		

		for(FieldDeclaration fixFieldDec:fixFieldDecList){
			List<VariableDeclarationFragment> varDecFragList=fixFieldDec.fragments();
			for(VariableDeclarationFragment vdf: varDecFragList){
				int lineId=fixedWholeCodeAST.getCompilationUnit().getLineNumber(vdf.getStartPosition())-1;
				if(editFlag.get(lineId)) varDecMap.put(vdf.getName().toString(), vdf);
			}
		}

		for(MethodDeclaration md : fixedWholeCodeAST.getMethodDeclarations()){
			int startLine=fixedWholeCodeAST.getCompilationUnit().getLineNumber(md.getStartPosition());
			int endLine=fixedWholeCodeAST.getCompilationUnit().getLineNumber(md.getStartPosition()+md.getLength());
			if(edit!=null && edit.getBeginB()+1>=startLine&&edit.getEndB()<=endLine){
				if( md.getBody()!=null){
//					for(Object st: md.getBody().statements()){
//						if(((Statement)st).getNodeType()==ASTNode.VARIABLE_DECLARATION_STATEMENT) {
//							VariableDeclarationStatement varDecStmt= (VariableDeclarationStatement) st;
//							List<VariableDeclarationFragment> varDecFragList= varDecStmt.fragments();
//							for(VariableDeclarationFragment vdf: varDecFragList){
//								int lineNum=fixedWholeCodeAST.getCompilationUnit().getLineNumber(vdf.getStartPosition())-1;
//								System.out.println(wholeFixCode[lineNum]);
//								for(Edit ed: editList){
//									if(lineNum>=ed.getBeginB()&&lineNum<ed.getEndB()) varDecMap.put(vdf.getName().toString(), vdf);
//									else if(lineNum<ed.getBeginB()) break;
//								}
//							}							
//						}
//					}
					ArrayList<VariableDeclarationFragment> vdfList =fixedWholeCodeAST.getVariableDeclarationFragments();
					for(VariableDeclarationFragment vdf:vdfList){
						int lineId=fixedWholeCodeAST.getCompilationUnit().getLineNumber(vdf.getStartPosition());
						if(lineId>=startLine&&lineId<=endLine&&editFlag.get(lineId-1)){  // the editted line is within the range of the method
							varDecMap.put(vdf.getName().toString(), vdf);
						}else if(lineId > endLine) break; // out of the range of this method
					}
					//System.out.println(md.getBody().statements().size());
					for(String varName:varDecMap.keySet()){
						VariableDeclarationFragment vdf =varDecMap.get(varName);
						String[] fixInits=new String[4];
						if(vdf.getInitializer()!=null){		
							String fixInit=vdf.getInitializer().toString();
							fixInits[0]=fixInit;
							String fixInit2=null;
							if(fixInit.matches("^\\(\\w*\\)\\s*\\S+.*")){
								fixInit2=fixInit.replaceAll("^\\(\\w*\\)\\s*",""); // remove force casting
								fixInits[1]=fixInit2;
							}
							if(fixInit.matches("^\\s*\\(.*\\)\\s*")){			// remove bracket "(" and ")"
								String temp=fixInit.replaceAll("^\\s*\\(", "");
								fixInits[2]=temp.replaceAll("\\)\\s*$", "");
							}
							if(fixInit2!=null&&fixInit2.matches("^\\s*\\(.*\\)\\s*")){			// remove bracket "(" and ")"
								String temp=fixInit2.replaceAll("^\\s*\\(", "");
								fixInits[3]=temp.replaceAll("\\)\\s*$", "");
							}
						}else continue;
						int decLine=fixedWholeCodeAST.getCompilationUnit().getLineNumber(vdf.getStartPosition());
						int decLastLine=fixedWholeCodeAST.getCompilationUnit().getLineNumber(vdf.getStartPosition()+vdf.getLength());
						//check whether the rebuild def-use chain is used in a assert statement
						for(int i=edit.getBeginA();i<edit.getEndA();i++){
							if(vdf.getInitializer()!=null){	
								if(decLine==decLastLine){
									for(int countOfFixInit=0;countOfFixInit<4;countOfFixInit++){
										boolean flag=true;
										stmt=Utils.removeLineComments(initStmt).replaceAll("\\s", "");
										if(fixInits[countOfFixInit]!=null&&stmt.equals(Utils.removeLineComments(fixInits[countOfFixInit]).replaceAll("\\s", ""))){
											// check the using of this new defined variable in this method
											for(int j=decLine;j<endLine;j++){
												///if(wholeFixCode[j].indexOf(varName)!=-1){
												if(wholeFixCode[j].matches(".*\\W"+varName+"\\W.*")){
													if(!(wholeFixCode[j].matches("^\\s*assert\\s+.*")
															||wholeFixCode[j].matches("^\\s*(//|\\*|/\\*|\\*/|@).*"))){
														flag=false;
													}
												}
											}
											if(flag) return true;
										}
									}
								}else{
									for(int countOfFixInit=0;countOfFixInit<4;countOfFixInit++){
										if(fixInits[countOfFixInit]!=null){
											String biStmt="";
											String cleanedFixedStmt=Utils.removeLineComments(fixInits[countOfFixInit]).replaceAll("\\s", "");
											for(int j=biChange.getLineNum()-1;j<wholeBiCode.length;j++){
												biStmt=biStmt+wholeBiCode[j];
												String cleanedStmt=Utils.removeLineComments(biStmt).replaceAll("\\s", "");
												if(cleanedStmt.matches(".*;$")){
													if(cleanedFixedStmt.indexOf(cleanedStmt.replaceAll(";$",""))!=-1){

														if(cleanedFixedStmt.equals(cleanedStmt.replaceAll(";$",""))){
															// check the using of this new defined variable in this method
															boolean flag=true;
															for(int k=decLine;k<endLine;k++){
																//if(wholeFixCode[k].indexOf(varName)!=-1){
																if(wholeFixCode[k].matches(".*\\W"+varName+"\\W.*")){
																	if(!(wholeFixCode[k].matches("^\\s*assert\\s+.*")
																			||wholeFixCode[k].matches("^\\s*(//|\\*|/\\*|\\*/|@).*"))){
																		flag=false;
																	}
																}
															}
															if(flag) return true;
														}else{
															continue;
														}
													}else{
														break;
													}
												}else{
													if(cleanedFixedStmt.indexOf(cleanedStmt)!=-1){
														// check the using of this new defined variable in this method
														if(cleanedFixedStmt.equals(cleanedStmt)){
															boolean flag=true;
															for(int k=decLine;k<endLine;k++){
																if(wholeFixCode[k].indexOf(varName)!=-1){
																	if(!(wholeFixCode[k].matches("^\\s*assert\\s+.*")
																			||wholeFixCode[k].matches("^\\s*(//|\\*|/\\*|\\*/|@).*"))){
																		flag=false;
																	}
																}
															}
															if(flag) return true;
														}else{
															continue;
														}
													}else{
														break;
													}												
												}
											}
										}

									}
								}

							}
						}
						
						for(int i=edit.getBeginB();i<edit.getEndB();i++){
							if(wholeFixCode[i].matches(".*\\W"+varName+"\\W.*")){
								if(vdf.getInitializer()!=null){		
									for(int countOfFixInit=0;countOfFixInit<4;countOfFixInit++){
										stmt=initStmt;
										if(fixInits[countOfFixInit]!=null&&stmt.indexOf(fixInits[countOfFixInit])!=-1){
											String fixedStmt=wholeFixCode[i];
											fixedStmt=fixedStmt.replaceAll(varName, fixInits[countOfFixInit]);
											fixedStmt=Utils.removeLineComments(fixedStmt).trim();
											stmt=Utils.removeLineComments(stmt).trim();
											stmt=stmt.replaceAll("\\s", "");
											fixedStmt=fixedStmt.replaceAll("\\s", "");

											if(fixedStmt.equals(stmt)){
												if(decLine>=startLine&&decLine<=endLine){
													decLine--;
													if(decLine+1==i) return true;
													else if(decLine>=i) break;
													else{
														for(int m=decLine+1;m<i;m++){
															if(!(wholeFixCode[m].matches("^\\s*(//|\\*|/\\*|\\*/|@).*")||wholeFixCode[m].matches("^\\s+")
																	||wholeFixCode[m].matches("^\\s*assert\\s+.*"))){
																break;
															}else if(m==i-1){
																return true;
															}
														}
														break;
													}
												}else return true;
											}
										}
									}
//									int index=stmt.indexOf(fixInit);
//									if(index!=-1){
//										String fixedStmt=wholeFixCode[i];
//										fixedStmt=fixedStmt.replaceAll(varName, fixInit);
//										fixedStmt=Utils.removeLineComments(fixedStmt).trim();
//										stmt=Utils.removeLineComments(stmt).trim();
//										stmt=stmt.replaceAll("\\s", "");
//										fixedStmt=fixedStmt.replaceAll("\\s", "");
//										if(fixedStmt.equals(stmt)){
//											int decLine=fixedWholeCodeAST.getCompilationUnit().getLineNumber(vdf.getStartPosition());
//											if(decLine>=startLine&&decLine<=endLine){
//												decLine--;
//												if(decLine+1==i) return true;
//												else if(decLine>=i) continue;
//												else{
//													for(int m=decLine+1;m<i;m++){
//														if(!(wholeFixCode[m].matches("^\\s*(//|\\*|/\\*|\\*/|@).*")||wholeFixCode[m].matches("^\\s+")
//																||wholeFixCode[m].matches("^\\s*assert\\s+.*"))){
//															break;
//														}else if(m==i-1){
//															return true;
//														}
//													}
//													continue;
//												}
//											}else return true;
//										}
//									}else if(fixInit2!=null&&stmt.indexOf(fixInit2)!=-1){
//										String fixedStmt=wholeFixCode[i];
//										fixedStmt=fixedStmt.replaceAll(varName, fixInit2);
//										fixedStmt=Utils.removeLineComments(fixedStmt).trim();
//										stmt=Utils.removeLineComments(stmt).trim();
//										stmt=stmt.replaceAll("\\s", "");
//										fixedStmt=fixedStmt.replaceAll("\\s", "");
//										if(fixedStmt.equals(stmt)){
//											int decLine=fixedWholeCodeAST.getCompilationUnit().getLineNumber(vdf.getStartPosition());
//											if(decLine>=startLine&&decLine<=endLine){
//												decLine--;
//												if(decLine+1==i) return true;
//												else if(decLine>=i) continue;
//												else{
//													for(int m=decLine+1;m<i;m++){
//														if(!(wholeFixCode[m].matches("^\\s*(//|\\*|/\\*|\\*/|@).*")||wholeFixCode[m].matches("^\\s+")
//																||wholeFixCode[m].matches("^\\s*assert\\s+.*"))){
//															break;
//														}else if(m==i-1){
//															return true;
//														}
//													}
//													continue;
//												}
//											}else return true;
//										}						
//									}
								}
							}
						}

						Edit nextEdit=null;
						for(int i=1;i<editList.size();i++){
							//if(editList.get(i-1).equals(edit)){
							if(editList.get(i-1).getBeginB()==edit.getBeginB()){
								nextEdit=editList.get(i);  // find the next edit from the editList
							}
						}
						if(nextEdit!=null){
							boolean  newLineFlag=false;
							for(int i=edit.getBeginB();i<edit.getEndB();i++){
								if(i!=(fixedWholeCodeAST.getCompilationUnit().getLineNumber(vdf.getStartPosition())-1)&&
										!(wholeFixCode[i].matches("^\\s*(//|\\*|/\\*|\\*/|@).*")||wholeFixCode[i].matches("^\\s+")
												||wholeFixCode[i].matches("^\\s*assert\\s+.*"))){
									newLineFlag=true;
									break;
								}
							}
							
							if(!newLineFlag){
								for(int i=nextEdit.getBeginB();i<nextEdit.getEndB();i++){
									if(wholeFixCode[i].matches(".*\\W"+varName+"\\W.*")){
										if(vdf.getInitializer()!=null){		
											for(int countOfFixInit=0;countOfFixInit<4;countOfFixInit++){
												if(fixInits[countOfFixInit]!=null&&stmt.indexOf(fixInits[countOfFixInit])!=-1){
													String fixedStmt=wholeFixCode[i];
													fixedStmt=fixedStmt.replaceAll(varName, fixInits[countOfFixInit]);
													fixedStmt=Utils.removeLineComments(fixedStmt).trim();
													stmt=Utils.removeLineComments(stmt).trim();
													stmt=stmt.replaceAll("\\s", "");
													fixedStmt=fixedStmt.replaceAll("\\s", "");
													if(fixedStmt.equals(stmt)){
														System.out.println("id"+countOfFixInit+":  "+wholeFixCode[i]);
														if(decLine>=startLine&&decLine<=endLine){
															decLine--;
															if(decLine+1==i) return true;
															else if(decLine>=i) break;
															else{
																for(int m=decLine+1;m<i;m++){
																	if(!(wholeFixCode[m].matches("^\\s*(//|\\*|/\\*|\\*/|@).*")||wholeFixCode[m].matches("^\\s+")
																			||wholeFixCode[m].matches("^\\s*assert\\s+.*"))){
																		break;
																	}else if(m==i-1){
																		return true;
																	}
																}
																break;
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
