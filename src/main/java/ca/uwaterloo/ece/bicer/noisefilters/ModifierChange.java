package ca.uwaterloo.ece.bicer.noisefilters;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jgit.diff.Edit;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.JavaASTParser;
import ca.uwaterloo.ece.bicer.utils.Utils;

public class ModifierChange implements Filter{
	final String name="Change a modifier";
	BIChange biChange;
	JavaASTParser biWholeCodeAST;
	JavaASTParser fixedWholeCodeAST;
	boolean isNoise=false;
	int biLineNum;
	int fixLineNum;
	
	public ModifierChange (BIChange biChange, JavaASTParser biWholeCodeAST, JavaASTParser fixedWholeCodeAST) {
		this.biChange = biChange;
		this.biWholeCodeAST = biWholeCodeAST;
		this.fixedWholeCodeAST = fixedWholeCodeAST;
		isNoise = filterOut();
	}
	
	@Override
	public boolean filterOut() {
		if(!biChange.getIsAddedLine()) return false;
		
		String biSource = biWholeCodeAST.getStringCode();
		int startPositionOfBILine = Utils.getStartPosition(biSource,biChange.getLineNumInPrevFixRev());
		
		if(startPositionOfBILine <0){
			System.err.println("Warning: line does not exist in BI source code " + ": " + biChange.getLine());
		}
		Edit edit=biChange.getEdit();
		if(edit==null) return false;

		if(edit.getType()!=Edit.Type.REPLACE){// || (edit.getEndA()-edit.getBeginA())!=1 || (edit.getEndB()-edit.getBeginB())!=1){
			return false;
		}
		biLineNum = biChange.getLineNumInPrevFixRev();
		fixLineNum = biChange.getEdit().getBeginB()+1;
		return modifierInMethodDeclaration()||modifierInVariableDeclaration();
	}

	@Override
	public boolean isNoise() {
		// TODO Auto-generated method stub
		return isNoise;
	}
	public boolean modifierInMethodDeclaration(){
		String stmt=biChange.getLine();
		stmt=stmt.trim();
		if(stmt.matches("(//|\\*|/\\*|\\*/|@).*")) return false; //JavaDoc or comments
		ArrayList<MethodDeclaration> lstBIMethodDeclaration = biWholeCodeAST.getMethodDeclarations();
		ArrayList<MethodDeclaration> lstFixMethodDeclaration = fixedWholeCodeAST.getMethodDeclarations();
		int originalModifier=0, changedModifier=0;
		ArrayList<Object> biLineNodes = new ArrayList<Object>(), fixLineNodes = new ArrayList<Object>();
		String[] wholeBiCode=biWholeCodeAST.getStringCode().split("\n");
		String[] wholeFixCode= fixedWholeCodeAST.getStringCode().split("\n");
		int lastLineOfMethod, firstLineOfMethod;
		for(MethodDeclaration methodDecNode:lstBIMethodDeclaration){
			lastLineOfMethod =biWholeCodeAST.getCompilationUnit().getLineNumber(methodDecNode.getStartPosition()+methodDecNode.getLength());
			firstLineOfMethod =biWholeCodeAST.getCompilationUnit().getLineNumber(methodDecNode.getStartPosition());
			while(firstLineOfMethod<=lastLineOfMethod){
				if(wholeBiCode[firstLineOfMethod-1].matches("\\s*(//|\\*|/\\*|\\*/|@).*")) {
					firstLineOfMethod++;
				}else break;
			}

			if(biLineNum==firstLineOfMethod){
				biLineNodes.add(methodDecNode.getName());
				originalModifier =methodDecNode.getModifiers();
				if(methodDecNode.getReturnType2() != null)
					biLineNodes.add(methodDecNode.getReturnType2());
				biLineNodes.add(methodDecNode.parameters());
				break;
			}
		}
		
		if(biLineNodes.size()!=0){ 
			for(MethodDeclaration methodDecNode:lstFixMethodDeclaration){
				if(methodDecNode.getName().toString().equals(biLineNodes.get(0).toString())){
					fixLineNodes.add(methodDecNode.getName());
					changedModifier = methodDecNode.getModifiers();
					if(methodDecNode.getReturnType2() != null)
						fixLineNodes.add(methodDecNode.getReturnType2());
					fixLineNodes.add(methodDecNode.parameters());
					
					if(biLineNodes.size()==fixLineNodes.size()){	
						int i=0;
						for(;i<biLineNodes.size();i++){
							if(!biLineNodes.get(i).toString().equals(fixLineNodes.get(i).toString()))
								break;
						}
						if(i==biLineNodes.size()){
							//System.out.println(biChange.getLine());
							lastLineOfMethod =fixedWholeCodeAST.getCompilationUnit().getLineNumber(methodDecNode.getStartPosition()+methodDecNode.getLength());
							firstLineOfMethod =fixedWholeCodeAST.getCompilationUnit().getLineNumber(methodDecNode.getStartPosition());
							while(firstLineOfMethod<=lastLineOfMethod){
								if(wholeFixCode[firstLineOfMethod-1].matches("\\s*(//|\\*|/\\*|\\*/|@).*")) {
									firstLineOfMethod++;
								}else break;
							}
							List<Edit> editls= biChange.getEditListFromDiff();
							for(Edit ed: editls){
								if(ed!=null&&((firstLineOfMethod-1)>=ed.getBeginB()&&(firstLineOfMethod-1)<ed.getEndB())){	
									if(Modifier.isPublic(originalModifier)){
										if(!Modifier.isPublic(changedModifier)) return true;
									}else if(Modifier.isProtected(originalModifier)){
										if(Modifier.isPrivate(changedModifier)) return true;
										else if(!Modifier.isProtected(changedModifier)&&!Modifier.isPublic(changedModifier)) return true;
									}else if(!Modifier.isPrivate(originalModifier)){// no modifier
										if(Modifier.isPrivate(changedModifier)) return true;
									}
									break;
								}
							}// end of for edit

						}// end of if(i==biLineNodes.size())
					
					}//end of if(biLineNodes.size()==fixLineNodes.size())
					fixLineNodes.clear();
				}	
			}
		}
		return false;
						
	}
	public boolean modifierInVariableDeclaration(){	
		List<FieldDeclaration> biFieldDecList=biWholeCodeAST.getFieldDeclarations();
		List<FieldDeclaration> fixFieldDecList=fixedWholeCodeAST.getFieldDeclarations();
		int originalModifier=0, changedModifier=0;
		ArrayList<Object> biLineNodes = new ArrayList<Object>(), fixLineNodes = new ArrayList<Object>();
		for(FieldDeclaration biFieldDec:biFieldDecList){
			List<VariableDeclarationFragment> varDecFragList=biFieldDec.fragments();
			for(VariableDeclarationFragment varDecFrag: varDecFragList){
				if(biLineNum==biWholeCodeAST.getCompilationUnit().getLineNumber(varDecFrag.getStartPosition())){
					originalModifier=biFieldDec.getModifiers();
					biLineNodes.add(biFieldDec.getType());
					Expression exp = varDecFrag.getInitializer();
					if(exp!=null) biLineNodes.add(exp);
					biLineNodes.add(varDecFrag.getName());	
					break;
				}
			}
			if(biLineNodes.size()!=0) break;
		}
		
		if(biLineNodes.size()!=0){
			for(FieldDeclaration fixFieldDec:fixFieldDecList){
				if(fixFieldDec.getType().toString().equals(biLineNodes.get(0).toString())){
					List<VariableDeclarationFragment> varDecFragList=fixFieldDec.fragments();
					for(VariableDeclarationFragment varDecFrag: varDecFragList){
						changedModifier=fixFieldDec.getModifiers();
						fixLineNodes.add(fixFieldDec.getType());	
						Expression exp = varDecFrag.getInitializer();
						if(exp!=null) fixLineNodes.add(exp);
						fixLineNodes.add(varDecFrag.getName());
						
						if(biLineNodes.size()==fixLineNodes.size()){	
							int i=0;
							for(i=0;i<biLineNodes.size();i++){
								if(!biLineNodes.get(i).toString().equals(fixLineNodes.get(i).toString()))
									break;
							}
							if(i==biLineNodes.size()){
								int fixFieldDecLineNum=fixedWholeCodeAST.getCompilationUnit().getLineNumber(varDecFrag.getStartPosition());
								for(Edit ed: biChange.getEditListFromDiff()){
									if(ed!=null){	
										// whether this line is a edit line
										if((fixFieldDecLineNum-1)>=ed.getBeginB()&&(fixFieldDecLineNum-1)<ed.getEndB()){
											if(Modifier.isPublic(originalModifier)){
												if(!Modifier.isPublic(changedModifier)) return true;
											}else if(Modifier.isProtected(originalModifier)){
												if(Modifier.isPrivate(changedModifier)) return true;
												else if(!Modifier.isProtected(changedModifier)&&!Modifier.isPublic(changedModifier)) return true;
											}else if(!Modifier.isPrivate(originalModifier)){// no modifier
												if(Modifier.isPrivate(changedModifier)) return true;
											}	
											break;
										}
									}
								}// end of for edit
							}//end of if(i==biLineNodes.size())
							
						}//end of if(biLineNodes.size()==fixLineNodes.size())
						fixLineNodes.clear();					
					}//end of for(VariableDeclarationFragment varDecFrag: varDecFragList)					
				}

				
			}			
		}
		return false;
	}
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}

}
