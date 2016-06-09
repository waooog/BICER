package ca.uwaterloo.ece.bicer.utils;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

public class JavaASTParser {
	
	public static ArrayList<String> praseJavaFile(String source){
		final ArrayList<String> list = new ArrayList<String>();
		
		ASTParser parser = ASTParser.newParser(AST.JLS2);

        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        char[] content = source.toCharArray();
        parser.setSource(content);
        parser.setUnitName("temp.java");
        Map<String, String> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_7);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM,
            JavaCore.VERSION_1_7);
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_7);
        String[] sources = {};
        String[] classPaths = {};
        parser.setEnvironment(classPaths, sources, null, true);
        parser.setResolveBindings(false);
        parser.setCompilerOptions(options);
        parser.setStatementsRecovery(true);

        try {
            final CompilationUnit unit = (CompilationUnit) parser.createAST(null);
            final AST ast = unit.getAST();

            // Process the main body
            try {
                unit.accept(new ASTVisitor() {
                    public boolean visit(CatchClause node) {
                        list.add("CatchClause");
                        return true;
                    }
                    public boolean visit(ClassInstanceCreation node) {
                        //list.add("ClassInstanceCreation");
                        list.add(node.getName().toString());
                        return true;
                    }

                    public boolean visit(DoStatement node) {
                        list.add("DoStatement");
                        
                        return true;
                    }
                    public boolean visit(EnumConstantDeclaration node) {
                        list.add(node.getName().toString());
                        return true;
                    }
                    public boolean visit(EnumDeclaration node) {
                        list.add("EnumDeclaration");
                        list.add(node.getName().toString());
                        return true;
                    }
                    public boolean visit(ForStatement node) {
                        list.add("ForStatement");
                        
                        return true;
                    }
                    public boolean visit(IfStatement node) {
                        list.add("IfStatement");
                        return true;
                    }
                    
                    public boolean visit(MethodDeclaration node) {
                        list.add("METHOD:" + node.getName().toString() + node.parameters().toString() + ":" + node.getStartPosition() + ":" + node.getLength());
                        return true;
                    }
                    
                    public boolean visit(AssertStatement node) {
                        list.add("AssertStatement");
                        return true;
                    } 
                    public boolean visit(ContinueStatement node) {
                        list.add("ContinueStatement");
                        return true;
                    }
                  
                    
                    public boolean visit(MethodInvocation node) {
                        list.add(node.getName().toString());
                        return true;
                    }
                    public boolean visit(SwitchCase node) {
                        list.add("SwitchCase");
                        return true;
                    }
                    public boolean visit(SynchronizedStatement node) {
                        list.add("SynchronizedStatement");
                        return true;
                    }
                    public boolean visit(ThisExpression node) {
                        list.add("ThisExpression");
                        return true;
                    }
                    public boolean visit(ThrowStatement node) {
                        list.add("ThrowStatement");
                        return true;
                    }
                    public boolean visit(TryStatement node) {
                        list.add("TryStatement");
                        return true;
                    }
                    public boolean visit(TypeDeclaration node) {
                        list.add(node.getName().toString());
                        return true;
                    }
                    public boolean visit(WhileStatement node) {
                   
                        list.add("WhileStatement");
                        return true;
                    }
                    
                    //            @Override public boolean visit(final ExpressionStatement node) {
                    //                Log.info("ExpressionStatement");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final AnnotationTypeDeclaration node) {
                    //                Log.info("AnnotationTypeDeclaration");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final AnnotationTypeMemberDeclaration node) {
                    //                Log.info("AnnotationTypeMemberDeclaration");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final AnonymousClassDeclaration node) {
                    //                Log.info("AnonymousClassDeclaration");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final ArrayAccess node) {
                    //                Log.info("ArrayAccess");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final ArrayCreation node) {
                    //                Log.info("ArrayCreation");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final ArrayInitializer node) {
                    //                Log.info("ArrayInitializer");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final ArrayType node) {
                    //                Log.info("ArrayType");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final Assignment node) {
                    //                Log.info("Assignment");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            //            @Override public boolean visit(final Block node) {
                    //            //                Log.info("Block");
                    //            //                Log.info(node);
                    //            //                return super.visit(node);
                    //            //            }
                    //
                    //            @Override public boolean visit(final BlockComment node) {
                    //                Log.info("BlockComment");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final BooleanLiteral node) {
                    //                Log.info("BooleanLiteral");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final CastExpression node) {
                    //                Log.info("CastExpression");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final CharacterLiteral node) {
                    //                Log.info("CharacterLiteral");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    
                    //
                    //            //            @Override public boolean visit(final CompilationUnit node) {
                    //            //                Log.info("CompilationUnit");
                    //            //                Log.info(node);
                    //            //                return super.visit(node);
                    //            //            }
                    //
                    //            @Override public boolean visit(final ConditionalExpression node) {
                    //                Log.info("ConditionalExpression");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final ConstructorInvocation node) {
                    //                Log.info("ConstructorInvocation");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final CreationReference node) {
                    //                Log.info("CreationReference");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final Dimension node) {
                    //                Log.info("Dimension");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final EmptyStatement node) {
                    //                Log.info("EmptyStatement");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final EnumConstantDeclaration node) {
                    //                Log.info("EnumConstantDeclaration");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final EnumDeclaration node) {
                    //                Log.info("EnumDeclaration");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final ExpressionMethodReference node) {
                    //                Log.info("ExpressionMethodReference");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final FieldAccess node) {
                    //                Log.info("FieldAccess");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final FieldDeclaration node) {
                    //                Log.info("FieldDeclaration");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final ImportDeclaration node) {
                    //                Log.info("ImportDeclaration");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final InfixExpression node) {
                    //                Log.info("InfixExpression");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final Initializer node) {
                    //                Log.info("Initializer");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final InstanceofExpression node) {
                    //                Log.info("InstanceofExpression");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final IntersectionType node) {
                    //                Log.info("IntersectionType");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final Javadoc node) {
                    //                Log.info("Javadoc");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final LabeledStatement node) {
                    //                Log.info("LabeledStatement");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final LambdaExpression node) {
                    //                Log.info("LambdaExpression");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final LineComment node) {
                    //                Log.info("LineComment");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final MarkerAnnotation node) {
                    //                Log.info("MarkerAnnotation");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final MemberRef node) {
                    //                Log.info("MemberRef");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final MemberValuePair node) {
                    //                Log.info("MemberValuePair");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            //            @Override public boolean visit(final MethodDeclaration node) {
                    //            //                Log.info("MethodDeclaration");
                    //            //                Log.info(node);
                    //            //                return super.visit(node);
                    //            //            }
                    //            @Override public boolean visit(final MethodRef node) {
                    //                Log.info("MethodRef");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final MethodRefParameter node) {
                    //                Log.info("MethodRefParameter");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            //            @Override public boolean visit(final Modifier node) {
                    //            //                Log.info("Modifier");
                    //            //                Log.info(node);
                    //            //                return super.visit(node);
                    //            //            }
                    //            @Override public boolean visit(final NameQualifiedType node) {
                    //                Log.info("NameQualifiedType");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final NormalAnnotation node) {
                    //                Log.info("NormalAnnotation");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final NullLiteral node) {
                    //                Log.info("NullLiteral");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final NumberLiteral node) {
                    //                Log.info("NumberLiteral");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final PackageDeclaration node) {
                    //                Log.info("PackageDeclaration");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final ParameterizedType node) {
                    //                Log.info("ParameterizedType");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final ParenthesizedExpression node) {
                    //                Log.info("ParenthesizedExpression");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final PostfixExpression node) {
                    //                Log.info("PostfixExpression");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final PrefixExpression node) {
                    //                Log.info("PrefixExpression");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final PrimitiveType node) {
                    //                Log.info("PrimitiveType");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final QualifiedName node) {
                    //                Log.info("QualifiedName");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //
                    //            @Override public boolean visit(final QualifiedType node) {
                    //                Log.info("QualifiedType");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final SimpleName node) {
                    //                Log.info("SimpleName");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            //            @Override public boolean visit(final SimpleType node) {
                    //            //                Log.info("SimpleType");
                    //            //                Log.info(node);
                    //            //                return super.visit(node);
                    //            //            }
                    //            @Override public boolean visit(final SingleMemberAnnotation node) {
                    //                Log.info("SingleMemberAnnotation");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final SingleVariableDeclaration node) {
                    //                Log.info("SingleVariableDeclaration");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final StringLiteral node) {
                    //                Log.info("StringLiteral");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final SuperConstructorInvocation node) {
                    //                Log.info("SuperConstructorInvocation");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final SuperFieldAccess node) {
                    //                Log.info("SuperFieldAccess");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final SuperMethodInvocation node) {
                    //                Log.info("SuperMethodInvocation");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final SuperMethodReference node) {
                    //                Log.info("SuperMethodReference");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final TagElement node) {
                    //                Log.info("TagElement");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final TextElement node) {
                    //                Log.info("TextElement");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            //            @Override public boolean visit(final ThisExpression node) {
                    //            //                Log.info("ThisExpression");
                    //            //                Log.info(node);
                    //            //                return super.visit(node);
                    //            //            }
                    //            //            @Override public boolean visit(final TypeDeclaration node) {
                    //            //                Log.info("TypeDeclaration");
                    //            //                Log.info(node);
                    //            //                return super.visit(node);
                    //            //            }
                    //            @Override public boolean visit(final TypeDeclarationStatement node) {
                    //                Log.info("TypeDeclarationStatement");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final TypeLiteral node) {
                    //                Log.info("TypeLiteral");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final TypeMethodReference node) {
                    //                Log.info("TypeMethodReference");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final TypeParameter node) {
                    //                Log.info("UnionType");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final UnionType node) {
                    //                Log.info("UnionType");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final VariableDeclarationExpression node) {
                    //                Log.info("VariableDeclarationExpression");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final VariableDeclarationFragment node) {
                    //                Log.info("VariableDeclarationFragment");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final VariableDeclarationStatement node) {
                    //                Log.info("VariableDeclarationStatement");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                    //            @Override public boolean visit(final WildcardType node) {
                    //                Log.info("WildcardType");
                    //                Log.info(node);
                    //                return super.visit(node);
                    //            }
                });
            } catch (Exception e) {
                System.out.println("Problem : " + e.toString());
                e.printStackTrace();
                System.exit(0);
            }
            return list;

        } catch (Exception e) {
            System.out.println("\nError while executing compilation unit : " + e.toString());
            return null;
        }
        
	}
}
