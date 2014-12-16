package aurora.sqlje.ast;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.*;

import aurora.sqlje.core.annotation.Autonomous;

/**
 * 将带有@Autonomous标记的方法进行修改
 * 
 * @author jessen
 *
 */
public class AutonomousWrapper {
	private TypeDeclaration typeDec;
	private AST ast;

	public AutonomousWrapper(TypeDeclaration typeDec) {
		super();
		this.typeDec = typeDec;
		ast = typeDec.getAST();
	}

	public void autoDelegate() {
		ArrayList<MethodDeclaration> list = new ArrayList<MethodDeclaration>();
		findAutonomousMethod(list);
		for (MethodDeclaration md : list) {
			// MethodDeclaration md_ = cloneMethodDeclare(md);
			Block b_ = createWrappedBody(md);
			// md_.setBody(b_);
			// StructuralPropertyDescriptor spd = md.getLocationInParent();
			// List list_ = (List) md.getParent().getStructuralProperty(spd);
			// int idx = list_.indexOf(md);
			// list_.remove(idx);
			// list_.add(idx, md_);
		}
	}

	@SuppressWarnings("unchecked")
	private MethodDeclaration cloneMethodDeclare(MethodDeclaration md) {
		MethodDeclaration md_ = ast.newMethodDeclaration();
		md_.setName((SimpleName) ASTNode.copySubtree(ast, md.getName()));
		md_.modifiers().addAll(ASTNode.copySubtrees(ast, md.modifiers()));
		md_.parameters().addAll(ASTNode.copySubtrees(ast, md.parameters()));
		md_.thrownExceptions().addAll(
				ASTNode.copySubtrees(ast, md.thrownExceptions()));
		md_.typeParameters().addAll(
				ASTNode.copySubtrees(ast, md.typeParameters()));
		md_.setReturnType2((Type) ASTNode.copySubtree(ast, md.getReturnType2()));
		return md_;
	}

	@SuppressWarnings("unchecked")
	private Block createWrappedBody(MethodDeclaration md) {
		Block body = md.getBody();
		List<Statement> md_stmt_list = body.statements();
		List<ASTNode> original_statements = ASTNode.copySubtrees(ast,
				md_stmt_list);
		// body = ast.newBlock();
		// body.setSourceRange(startPosition, length);
		// md.setBody(body);
		// md_stmt_list = body.statements();
		md_stmt_list.clear();
		MethodInvocation mi_createconnection = ASTNodeUtil.newMethodInvocation(
				ast, ASTNodeUtil.newMethodInvocation(ast, null,
						AstTransform.GET_SQLCALL_STACK), "createConnection");
		String conn_name = "_$autonomous_connection_";
		Statement create_connection = ASTNodeUtil
				.newVariableDeclarationStatement(ast, Connection.class
						.getSimpleName(), ASTNodeUtil
						.newVariableDeclarationFragment(ast, conn_name,
								mi_createconnection));
		md_stmt_list.add(create_connection);

		String exception_State = "_$autonomous_exception_";
		Statement __exception_define = ASTNodeUtil
				.newVariableDeclarationStatement(ast, Exception.class
						.getSimpleName(), ASTNodeUtil
						.newVariableDeclarationFragment(ast, exception_State,
								null));
		md_stmt_list.add(__exception_define);

		// try-catch-finally
		TryStatement trys = ast.newTryStatement();
		Block tryBody = trys.getBody();

		CatchClause cc = ast.newCatchClause();
		trys.catchClauses().add(cc);
		SingleVariableDeclaration svd_exception = ast
				.newSingleVariableDeclaration();
		svd_exception.setType(ASTNodeUtil.newSimpleType(ast,
				Exception.class.getSimpleName()));
		svd_exception.setName(ast.newSimpleName("e"));
		cc.setException(svd_exception);
		Block ccBlock = cc.getBody();
		if (ccBlock == null) {
			ccBlock = ast.newBlock();
			cc.setBody(ccBlock);
		}
		Assignment assin = ast.newAssignment();
		assin.setLeftHandSide(ast.newSimpleName(exception_State));
		assin.setRightHandSide(ast.newSimpleName("e"));
		ccBlock.statements().add(ast.newExpressionStatement(assin));
		ThrowStatement throwStatement = ast.newThrowStatement();
		throwStatement.setExpression(ast.newSimpleName("e"));
		ccBlock.statements().add(throwStatement);

		Block finallyBlock = trys.getFinally();
		if (finallyBlock == null) {
			finallyBlock = ast.newBlock();
			trys.setFinally(finallyBlock);
		}
		tryBody.statements().addAll(original_statements);
		// tryBody.accept(new ClearSourceRangeVisitor());

		/*
		 * if(_$autonomous_exception_ == null) commit(); else rollback();
		 */
		IfStatement ifStmt = ast.newIfStatement();
		InfixExpression eqExp = ast.newInfixExpression();
		eqExp.setLeftOperand(ast.newSimpleName(exception_State));
		eqExp.setOperator(InfixExpression.Operator.EQUALS);
		eqExp.setRightOperand(ast.newNullLiteral());
		ifStmt.setExpression(eqExp);
		ifStmt.setThenStatement(ast.newExpressionStatement(ASTNodeUtil
				.newMethodInvocation(ast, ast.newSimpleName(conn_name),
						"commit")));// commmit;
		ifStmt.setElseStatement(ast.newExpressionStatement(ASTNodeUtil
				.newMethodInvocation(ast, ast.newSimpleName(conn_name),
						"rollback")));// rollback;

		finallyBlock.statements().add(ifStmt);

		/*
		 * free(_$autonomous_connection_)
		 */
		MethodInvocation mi_freeconnection = ASTNodeUtil.newMethodInvocation(
				ast, ASTNodeUtil.newMethodInvocation(ast, null,
						AstTransform.GET_SQLCALL_STACK), "free", ast
						.newSimpleName(conn_name));

		finallyBlock.statements().add(
				ast.newExpressionStatement(mi_freeconnection));
		md_stmt_list.add(trys);
		// md.setSourceRange(md.getStartPosition(), 0);
		return body;
	}

	private void findAutonomousMethod(final ArrayList<MethodDeclaration> list) {
		typeDec.accept(new ASTVisitor() {
			public boolean visit(MethodDeclaration md) {
				List<IExtendedModifier> modifiers = md.modifiers();
				for (IExtendedModifier em : modifiers) {
					if (isAutonomousAnnotation(em)) {
						list.add(md);
						break;
					}
				}
				return super.visit(md);
			}
		});
	}

	private boolean isAutonomousAnnotation(IExtendedModifier em) {
		if (em instanceof MarkerAnnotation) {
			MarkerAnnotation ma = (MarkerAnnotation) em;
			// Method is marked as Autonomous
			if (ma.getTypeName().toString()
					.equals(Autonomous.class.getSimpleName())) {
				return true;
			}
		}
		return false;
	}

	class ClearSourceRangeVisitor extends ASTVisitor {

		@Override
		public boolean visit(AnnotationTypeDeclaration node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(AnnotationTypeMemberDeclaration node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(AnonymousClassDeclaration node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(ArrayAccess node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(ArrayCreation node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(ArrayInitializer node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(ArrayType node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(AssertStatement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(Assignment node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(Block node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(BlockComment node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(BooleanLiteral node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(BreakStatement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(CastExpression node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(CatchClause node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(CharacterLiteral node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(ClassInstanceCreation node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(CompilationUnit node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(ConditionalExpression node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(ConstructorInvocation node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(ContinueStatement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(CreationReference node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(Dimension node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(DoStatement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(EmptyStatement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(EnhancedForStatement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(EnumConstantDeclaration node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(EnumDeclaration node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(ExpressionMethodReference node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(ExpressionStatement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(FieldAccess node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(FieldDeclaration node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(ForStatement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(IfStatement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(ImportDeclaration node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(InfixExpression node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(Initializer node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(InstanceofExpression node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(IntersectionType node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(Javadoc node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(LabeledStatement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(LambdaExpression node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(LineComment node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(MarkerAnnotation node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(MemberRef node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(MemberValuePair node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(MethodRef node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(MethodRefParameter node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(MethodInvocation node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(Modifier node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(NameQualifiedType node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(NormalAnnotation node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(NullLiteral node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(NumberLiteral node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(PackageDeclaration node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(ParameterizedType node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(ParenthesizedExpression node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(PostfixExpression node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(PrefixExpression node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(PrimitiveType node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(QualifiedName node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(QualifiedType node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(ReturnStatement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(SimpleName node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(SimpleType node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(SingleMemberAnnotation node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(SingleVariableDeclaration node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(StringLiteral node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(SuperConstructorInvocation node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(SuperFieldAccess node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(SuperMethodInvocation node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(SuperMethodReference node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(SwitchCase node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(SwitchStatement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(SynchronizedStatement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(TagElement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(TextElement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(ThisExpression node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(ThrowStatement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(TryStatement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(TypeDeclarationStatement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(TypeLiteral node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(TypeMethodReference node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(TypeParameter node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(UnionType node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(VariableDeclarationExpression node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(VariableDeclarationStatement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(VariableDeclarationFragment node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(WhileStatement node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

		@Override
		public boolean visit(WildcardType node) {
			node.setSourceRange(-1, 0);
			return super.visit(node);
		}

	}
}