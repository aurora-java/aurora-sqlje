package aurora.sqlje.ast;

import static aurora.sqlje.ast.ASTNodeUtil.createAST;
import static aurora.sqlje.ast.ASTNodeUtil.newClassInstance;
import static aurora.sqlje.ast.ASTNodeUtil.newClassInstanceWithType;
import static aurora.sqlje.ast.ASTNodeUtil.newMethodInvocation;
import static aurora.sqlje.ast.ASTNodeUtil.newQualifiedName;
import static aurora.sqlje.ast.ASTNodeUtil.newStringLiteral;
import static aurora.sqlje.ast.ASTNodeUtil.newTypeLiteral;
import static aurora.sqlje.ast.ASTNodeUtil.newVariableDeclarationFragment;
import static aurora.sqlje.ast.ASTNodeUtil.newVariableDeclarationStatement;
import static aurora.sqlje.ast.ASTNodeUtil.parseExpression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import aurora.sqlje.core.ResultSetIterator;
import aurora.sqlje.core.ResultSetUtil;
import aurora.sqlje.core.SqlFlag;
import aurora.sqlje.exception.ParserException;
import aurora.sqlje.exception.TransformException;
import aurora.sqlje.parser.Parameter;
import aurora.sqlje.parser.ParameterParser;
import aurora.sqlje.parser.ParsedSource;
import aurora.sqlje.parser.ParsedSql;
import aurora.sqlje.parser.SqlBlock;
import aurora.sqlje.parser.SqlPosition;

public class AstTransform {
	private static final String GET_DATABASE_DESCRIPTOR = "getDatabaseDescriptor";
	private static final String PUSH = "push";
	public static final String GET_SQLCALL_STACK = "getSqlCallStack";
	public static final String METHOD_GET = "get";
	public static final String METHOD_TO_STRING = "toString";
	public static final String METHOD_APPEND = "append";
	public static final String METHOD_GET_CONNECTION = "getCurrentConnection";
	public static final String UPDATE_COUNT = "UPDATE_COUNT";
	public static final String METHOD_PREPARE_CALL = "prepareCall";
	public static final String METHOD_REGISTER_OUT_PARAMETER = "registerOutParameter";
	public static final String METHOD_PREPARE_STATEMENT = "prepareStatement";
	public static final String METHOD_EXECUTE = "execute";
	public static final String METHOD_GET_UPDATE_COUNT = "getUpdateCount";
	public static final String METHOD_GET_RESULT_SET = "getResultSet";
	public static final String SQL_FLAG = "$sql";
	public static final String METHOD_LOCK = "$lock";
	public static final String METHOD_INSERT = "$insert";
	public static final int API_LEVEL = AST.JLS3;

	private ParsedSource parsedSource;

	public AstTransform(ParsedSource parsedSource) {
		super();
		this.parsedSource = parsedSource;
	}

	public String tranform() throws Exception {
		CompilationUnit result = createCompilationUnit();

		String sourceToFormat = result.toString();
		Map options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
		CodeFormatter formatter = ToolFactory.createCodeFormatter(options);
		Document doc = new Document(sourceToFormat);
		TextEdit edit = formatter.format(CodeFormatter.F_INCLUDE_COMMENTS
				| CodeFormatter.K_COMPILATION_UNIT, sourceToFormat, 0,
				sourceToFormat.length(), 0, "\n");
		if (edit != null) {
			edit.apply(doc);
			return doc.get();
		} else {
			System.out.println("Can't format source.");
		}

		return result.toString();
	}

	/**
	 * the final java source will be set into <i>doc<i>
	 * 
	 * @param doc
	 *            the document that contains the parsed original source
	 * @throws Exception
	 */
	public void transform(IDocument doc) throws Exception {
		CompilationUnit result = createCompilationUnit();

		TextEdit edits = result.rewrite(doc, null);
		edits.apply(doc);
	}

	private CompilationUnit createCompilationUnit() throws Exception {
		StringBuilder strBuffer = parsedSource.getBuffer();
		// System.out.println(strBuffer);
		char[] cs = new char[strBuffer.length()];
		strBuffer.getChars(0, cs.length, cs, 0);
		CompilationUnit result = createAST(cs, ASTParser.K_COMPILATION_UNIT);
		long t0 = System.currentTimeMillis();
		if (result.getProblems().length == 0) {
			result.recordModifications();
			new ImportOrganizer(result).organize();
			List<TypeDeclaration> types = result.types();
			for (TypeDeclaration td : types) {
				transform(td);
			}
		} else {
			// System.out.println(strBuffer);
			for (IProblem p : result.getProblems()) {

				int dx = translateToAbs(p.getSourceStart())
						- p.getSourceStart();
				p.setSourceStart(p.getSourceStart() + dx);
				p.setSourceEnd(p.getSourceEnd() + dx);
				System.out.println(p);
			}
			throw new TransformException(result.getProblems());
		}
		// System.out.println(result);
		// System.out.println(System.currentTimeMillis() - t0);
		return result;
	}

	private int translateToAbs(int idx) {
		int size = parsedSource.getSqljBlockSize();
		int trx = 0;
		SqlBlock lastSqljBlock = null;
		for (int i = 0; i < size; i++) {
			SqlBlock b = parsedSource.getSqlById(i);
			if (lastSqljBlock == null) {
				trx = b.getStartIdx();
				if (trx > idx)
					return idx;
			} else {
				trx += lastSqljBlock.getReplaceLength();
				trx += (b.getStartIdx() - lastSqljBlock.getBodyEndIdx() - 1);
				if (trx > idx)
					return b.getStartIdx() - (trx - idx);
			}
			lastSqljBlock = b;
		}
		if (lastSqljBlock == null)
			return idx;
		trx += lastSqljBlock.getReplaceLength();
		return lastSqljBlock.getBodyEndIdx() + (idx - trx) + 1;
	}

	public void compile2Class() throws Exception {
		CompilationUnit unit = createCompilationUnit();
		org.eclipse.jdt.internal.compiler.Compiler compiler = new org.eclipse.jdt.internal.compiler.Compiler(
				new NameEnvironmentImpl(unit),
				DefaultErrorHandlingPolicies.proceedWithAllProblems(),
				new HashMap(), new CompilerRequestorImpl(),
				new DefaultProblemFactory(Locale.getDefault()));
		compiler.compile(new ICompilationUnit[] { new CompilationUnitImpl(unit) });
	}

	private void transform(TypeDeclaration typeDec) throws Exception {
		new InterfaceImpl(typeDec).addDefaultImpl();
		new AutonomousWrapper(typeDec).autoDelegate();
		final ArrayList<MethodInvocation> list = new ArrayList<MethodInvocation>();
		final ArrayList<MethodInvocation> lockMiList = new ArrayList<MethodInvocation>();
		final ArrayList<MethodInvocation> insertMiList = new ArrayList<MethodInvocation>();
		typeDec.accept(new ASTVisitor() {

			@Override
			public boolean visit(VariableDeclarationStatement node) {
				List<VariableDeclarationFragment> list = node.fragments();
				for (VariableDeclarationFragment vdf : list) {
					parsedSource.registerVariableType(vdf.getName()
							.getIdentifier(), node.getType().toString());
				}
				return super.visit(node);
			}

			public boolean visit(SingleVariableDeclaration svd) {
				parsedSource
						.registerVariableType(svd.getName().getIdentifier(),
								svd.getType().toString());
				return super.visit(svd);
			}

			@Override
			public boolean visit(FieldDeclaration fd) {
				String type = fd.getType().toString();
				List<VariableDeclarationFragment> list = fd.fragments();
				for (VariableDeclarationFragment vdf : list) {
					parsedSource.registerVariableType(vdf.getName()
							.getIdentifier(), type);
				}
				return false;
			}

			@Override
			public boolean visit(MethodInvocation node) {

				if (node.getName().getIdentifier()
						.equals(SqlPosition.METHOD_SQL_EXECUTE)) {
					list.add(node);
				} else if (node.getName().getIdentifier().equals(METHOD_LOCK)) {
					lockMiList.add(node);
				}
				// else if
				// (node.getName().getIdentifier().equals(METHOD_INSERT)) {
				// insertMiList.add(node);
				// }

				return super.visit(node);
			}

		});
		lockMethodReplace(lockMiList);
		for (int i = 0; i < list.size(); i++) {
			MethodInvocation mi = list.get(i);
			StructuralPropertyDescriptor spd = mi.getLocationInParent();
			if (spd.getNodeClass() == MethodInvocation.class
					&& "expression".equals(spd.getId())) {
				/**
				 * parentNode is a MethodInvocation,e.g.
				 * __sqlje_execute(..).limit(start,end)
				 */
				MethodInvocation parentNode = (MethodInvocation) mi.getParent();
				StructuralPropertyDescriptor spd2 = parentNode
						.getLocationInParent();
				MethodInvocation miClone = (MethodInvocation) ASTNode
						.copySubtree(mi.getAST(), mi);
				miClone.arguments().addAll(
						ASTNode.copySubtrees(mi.getAST(),
								parentNode.arguments()));
				/**
				 * replace parentNode with a new methodInvocation, the
				 * methodInvocation expression is clone from mi ,but, contains
				 * two extra arguments,these two arguments will be used to
				 * generate some extra statements.
				 */
				if (spd2 instanceof ChildPropertyDescriptor) {
					parentNode.getParent().setStructuralProperty(spd2, miClone);
				} else if (spd2 instanceof ChildListPropertyDescriptor) {
					List<ASTNode> children = (List<ASTNode>) parentNode
							.getParent().getStructuralProperty(spd2);
					children.set(children.indexOf(parentNode), miClone);
				}
				list.set(i, miClone);
			}
		}
		executeMethodReplace(list);
	}

	/**
	 * replace $lock method<br>
	 * $lock("tableName","whereClause");<br>
	 * $lock("tableName");
	 * 
	 * @param list
	 * @throws Exception
	 */
	private void lockMethodReplace(List<MethodInvocation> list)
			throws Exception {
		for (MethodInvocation mi : list) {
			Statement s = findStatement(mi);
			List<Statement> stmts = (List<Statement>) s.getParent()
					.getStructuralProperty(s.getLocationInParent());
			int index = stmts.indexOf(s);
			List<Statement> gen_stmts = generate_lock_stmts(mi);
			stmts.remove(index);
			stmts.addAll(index, gen_stmts);
		}
	}

	private List<Statement> generate_lock_stmts(MethodInvocation mi)
			throws Exception {
		ArrayList<Statement> list = new ArrayList<Statement>();
		List<Expression> argsList = mi.arguments();
		String tableName = ((StringLiteral) argsList.get(0)).getLiteralValue();
		String whereClause = "";
		if (argsList.size() >= 1 && argsList.get(1) instanceof StringLiteral) {
			whereClause = ((StringLiteral) argsList.get(1)).getLiteralValue();
		}

		String sql = "SELECT * FROM " + tableName + " WHERE " + whereClause;

		LockOption lockOption = new LockOption(tableName, whereClause);
		System.out.println(sql);
		ParameterParser pp = new ParameterParser(sql);
		ParsedSql psql = pp.parse();
		String stmt_name = parsedSource.genId("ps");
		AST ast = mi.getAST();
		defineStatement(ast, psql, stmt_name, list, lockOption);
		performParameterBinding(ast, psql, list, stmt_name, lockOption);
		executeStatement(ast, stmt_name, list);
		pushStmtIntoList(ast, stmt_name, list);
		return list;
	}

	private Statement findStatement(ASTNode n) {
		ASTNode p = n.getParent();
		if (p instanceof Statement)
			return (Statement) p;
		return findStatement(p);
	}

	/**
	 * replace __sqlj_execute method
	 * 
	 * @param list
	 * @throws Exception
	 */
	private void executeMethodReplace(List<MethodInvocation> list)
			throws Exception {
		for (MethodInvocation mi : list) {
			String rs_id = mi.arguments().get(1).toString();
			Statement s = findStatement(mi);
			List<Statement> stmts = (List<Statement>) s.getParent()
					.getStructuralProperty(s.getLocationInParent());
			// autoAddSqlFlag(stmts, mi.getAST());//move to interfaceImpl
			int index = stmts.indexOf(s);
			ArrayList<Statement> gene_stmts = generate__sqlj_execute(mi);
			stmts.addAll(index, gene_stmts);
			index += gene_stmts.size();
			AST ast = mi.getAST();

			StructuralPropertyDescriptor loc = mi.getLocationInParent();
			if (loc.getNodeClass() == EnhancedForStatement.class
					&& "expression".equals(loc.getId())) {
				/*
				 * for(Map m:#{...}){ ... }
				 */
				EnhancedForStatement efs = (EnhancedForStatement) s;
				Expression newExp = createNewResultSetIteratorExpression(ast,
						rs_id, efs.getParameter().getType().toString());
				efs.setExpression(newExp);
				String refType = efs.getParameter().getType().toString();
				if (refType.equals(Map.class.getSimpleName())
						|| refType.equals(Map.class.getName())) {
					updateReferenceInFor(efs);
				}
			} else if (loc.getNodeClass() == VariableDeclarationFragment.class
					&& "initializer".equals(loc.getId())) {
				/*
				 * String name = #{select name from ...};
				 */
				VariableDeclarationStatement vds = (VariableDeclarationStatement) s;
				if (vds.getType() instanceof ParameterizedType) {
					// List<Bean> name = #{select * from ...}
					ParameterizedType _pt = (ParameterizedType) vds.getType();
					List<Type> types = _pt.typeArguments();
					if (types.size() != 1) {
						throw new Exception(_pt
								+ " has more than one Parameter");
					}
					// DataTransfer.transferAll
					Expression exp = ASTNodeUtil.createDataTransferExpression(
							ast, _pt.getType().toString(), types.get(0)
									.toString(), rs_id);
					mi.getParent().setStructuralProperty(loc,
							ASTNode.copySubtree(ast, exp));
					continue;
				} else if (vds.getType().toString()
						.equals(java.sql.ResultSet.class.getSimpleName())) {
					// ResultSet rs = #{..};
					mi.getParent().setStructuralProperty(loc,
							ast.newSimpleName(rs_id));
					continue;
				}
				// DataTransfer.transfer1
				Expression exp = ASTNodeUtil.createDataTransferExpression(ast,
						vds.getType().toString(), rs_id);
				mi.getParent().setStructuralProperty(loc,
						ASTNode.copySubtree(ast, exp));
			} else if (loc.getNodeClass() == Assignment.class
					&& "rightHandSide".equals(loc.getId())
					&& (s instanceof ExpressionStatement)) {
				/*
				 * name = #{select name from ...};
				 */
				ExpressionStatement es = (ExpressionStatement) s;
				Assignment assi = (Assignment) es.getExpression();
				Expression left = assi.getLeftHandSide();
				if (left instanceof SimpleName) {
					String varType = parsedSource.getVariableType(left
							.toString());
					if (varType != null) {
						// DataTransfer.transfer1
						Expression exp = ASTNodeUtil
								.createDataTransferExpression(ast, varType,
										rs_id);
						assi.setRightHandSide((Expression) ASTNode.copySubtree(
								ast, exp));
					}
				}
			} else if (loc.getNodeClass() == MethodInvocation.class
					&& "arguments".equals(loc.getId())) {
				/*
				 * process(1,#{...},"C");
				 */
				List<Expression> argslist = ((MethodInvocation) mi.getParent())
						.arguments();
				int idx = argslist.indexOf(mi);
				argslist.set(idx, ast.newSimpleName(rs_id));
			} else if (loc.getNodeClass() == ExpressionStatement.class
					&& "expression".equals(loc.getId())) {
				/*
				 * ${...};
				 */
				stmts.remove(index);
			} else if (loc.getNodeClass() == MethodInvocation.class
					&& "expression".equals(loc.getId())) {
				/*
				 * ... ${...}.limit()...
				 */
				// // reform,and put it into unHandled list
				// MethodInvocation parent = (MethodInvocation) mi.getParent();
				// StructuralPropertyDescriptor spd =
				// parent.getLocationInParent();
				// MethodInvocation cloneMi = (MethodInvocation) ASTNode
				// .copySubtree(ast, mi);
				// if (spd instanceof ChildListPropertyDescriptor) {
				// //
				// } else if (spd instanceof ChildPropertyDescriptor) {
				// parent.getParent().setStructuralProperty(spd, cloneMi);
				// }
				// // remove already generated statements,(those statements will
				// // generate again later)
				// stmts.removeAll(gene_stmts);
				// unHandled.add(cloneMi);
				// System.out.println(loc);
			} else {
				System.err.println(mi + "\n\t" + loc);
			}
		}
	}

	void autoAddSqlFlag(List<Statement> list, AST ast) {
		if (list.size() == 0) {
			list.add(createSqlFlag(ast));
		} else {
			Statement firstStmt = list.get(0);
			if (!(firstStmt instanceof VariableDeclarationStatement)) {
				list.add(0, createSqlFlag(ast));
			} else {
				VariableDeclarationStatement vds = (VariableDeclarationStatement) firstStmt;
				if (vds.getType().toString()
						.equals(SqlFlag.class.getSimpleName())) {
					// skip
				} else
					list.add(0, createSqlFlag(ast));
			}
		}
	}

	/**
	 * final SqlFlag $sql = new SqlFlag();
	 * 
	 * @param ast
	 * @return
	 */
	VariableDeclarationStatement createSqlFlag(AST ast) {
		String sqlFlagClassName = SqlFlag.class.getSimpleName();
		VariableDeclarationStatement vds = ASTNodeUtil
				.newVariableDeclarationStatement(ast, sqlFlagClassName,
						ASTNodeUtil.newVariableDeclarationFragment(ast,
								SQL_FLAG, ASTNodeUtil.newClassInstance(ast,
										sqlFlagClassName)));
		vds.modifiers().add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
		return vds;
	}

	void updateReferenceInFor(EnhancedForStatement efs) {
		SingleVariableDeclaration svd = efs.getParameter();
		final String ref = svd.getName().getIdentifier();
		final HashMap<String, String> columnTypeCache = new HashMap<String, String>();
		efs.getBody().accept(new ASTVisitor() {

			@Override
			public boolean visit(QualifiedName node) {
				if (node.getQualifier().getFullyQualifiedName().equals(ref)) {
					AST ast = node.getAST();
					ASTNode parent = node.getParent();
					StructuralPropertyDescriptor loc = node
							.getLocationInParent();
					String name = node.getName().getIdentifier();
					String type = columnTypeCache.get(name);
					if (type == null) {
						type = guessType(parent);
						columnTypeCache.put(name, type);
					}
					Expression newExp = ASTNodeUtil.newMethodInvocation(ast,
							ast.newSimpleName(ResultSetUtil.class
									.getSimpleName()), METHOD_GET, ast
									.newSimpleName(ref),
							newStringLiteral(ast, name),
							newTypeLiteral(ast, type));
					if (loc.isChildProperty()) {
						parent.setStructuralProperty(loc, newExp);
					} else if (loc.isChildListProperty()) {
						List<ASTNode> childList = (List<ASTNode>) parent
								.getStructuralProperty(loc);
						int idx = childList.indexOf(node);
						childList.set(idx, newExp);
					} else
						System.out.println(loc);

				}
				return false;
			}

			@Override
			public boolean visit(VariableDeclarationStatement node) {
				List<VariableDeclarationFragment> list = node.fragments();
				for (VariableDeclarationFragment vdf : list) {
					parsedSource.registerVariableType(vdf.getName().toString(),
							node.getType().toString());
				}
				return super.visit(node);
			}

			public boolean visit(MethodInvocation mi) {
				// update reference in sub sql execution
				if (mi.getName().getIdentifier()
						.equals(SqlPosition.METHOD_SQL_EXECUTE)) {
					int sqlId = Integer.parseInt(mi.arguments().get(0)
							.toString());
					SqlBlock sqljb = parsedSource.getSqlById(sqlId);
					try {
						ParsedSql psql = sqljb.getParsedSql();
						for (Parameter p : psql.getBindParameters()) {
							Expression exp = parseExpression(mi.getAST(),
									p.getExpression());
							if (exp instanceof QualifiedName) {
								QualifiedName qn = (QualifiedName) exp;
								if (qn.getQualifier().getFullyQualifiedName()
										.equals(ref)) {
									p.setExpression(String.format(
											"%s.get(\"%s\")", ref, qn.getName()
													.getIdentifier()));
								}
							}
						}
					} catch (ParserException e) {
						e.printStackTrace();
					}
				}
				return true;
			}
		});
	}

	String guessType(ASTNode parent) {
		while (!(parent instanceof Statement)) {
			if (parent instanceof Assignment) {
				Assignment assi = (Assignment) parent;
				Expression leftHandSide = assi.getLeftHandSide();
				if (!(leftHandSide instanceof SimpleName))
					continue;
				String var = ((SimpleName) leftHandSide).getIdentifier();
				String type = parsedSource.getOriginalVariableType(var);
				if (type != null)
					return type;
				break;
			} else if (parent instanceof VariableDeclarationFragment) {
				VariableDeclarationStatement vds = (VariableDeclarationStatement) parent
						.getParent();
				Type type = vds.getType();
				while (type.isArrayType()) {
					ArrayType atype = (ArrayType) type;
					type = atype.getElementType();
				}
				return type.toString();
			} else if (parent instanceof MethodInvocation) {
				break;
			}
			parent = parent.getParent();
		}
		return Object.class.getSimpleName();
	}

	void defineStatement(AST ast, ParsedSql ps, String stmt_name,
			ArrayList<Statement> stmt_list, Object extraOption) {
		String stmt_type = java.sql.PreparedStatement.class.getSimpleName();
		String prepare_method = METHOD_PREPARE_STATEMENT;
		if (ps.hasOutputParameter()) {
			stmt_type = java.sql.CallableStatement.class.getSimpleName();
			prepare_method = METHOD_PREPARE_CALL;
		}

		Expression sqlExpression = createSqlLiteralStatements(ast, ps,
				stmt_list);
		String sqlStringId = parsedSource.genId("sql");
		stmt_list.add(ASTNodeUtil.newVariableDeclarationStatement(ast,
				String.class.getSimpleName(), ASTNodeUtil
						.newVariableDeclarationFragment(ast, sqlStringId,
								sqlExpression)));
		if (extraOption instanceof LimitOption) {
			Assignment assn = ast.newAssignment();
			assn.setLeftHandSide(ast.newSimpleName(sqlStringId));
			assn.setRightHandSide(ASTNodeUtil.newMethodInvocation(ast,
					ast.newSimpleName(SQL_FLAG), SqlFlag.PREPARE_LIMIT_SQL,
					ast.newSimpleName(sqlStringId)));
			stmt_list.add(ast.newExpressionStatement(assn));
		} else if (extraOption instanceof LockOption) {
			LockOption lockOption = (LockOption) extraOption;
			Assignment assn = ast.newAssignment();
			assn.setLeftHandSide(ast.newSimpleName(sqlStringId));
			assn.setRightHandSide(ASTNodeUtil.newMethodInvocation(ast,
					ast.newSimpleName(SQL_FLAG), "lock",
					ASTNodeUtil.newStringLiteral(ast, lockOption.tableName),
					ASTNodeUtil.newStringLiteral(ast, lockOption.whereClause)));
			stmt_list.add(ast.newExpressionStatement(assn));
		}

		// PreparedStatement ps =
		// getSqlCallStack().getCurrentConnection().preparedStatement(sql);
		VariableDeclarationStatement vds = newVariableDeclarationStatement(
				ast,
				stmt_type,
				newVariableDeclarationFragment(
						ast,
						stmt_name,
						newMethodInvocation(
								ast,
								newMethodInvocation(
										ast,
										newMethodInvocation(ast, null,
												GET_SQLCALL_STACK),
										METHOD_GET_CONNECTION), prepare_method,
								ast.newSimpleName(sqlStringId))));
		stmt_list.add(vds);
	}

	void executeStatement(AST ast, String stmt_name,
			ArrayList<Statement> stmt_list) {
		// clear sql flag
		MethodInvocation mi = newMethodInvocation(ast,
				ast.newSimpleName(SQL_FLAG), SqlFlag.CLEAR);
		stmt_list.add(ast.newExpressionStatement(mi));
		MethodInvocation mi2 = newMethodInvocation(ast,
				ast.newSimpleName(stmt_name), METHOD_EXECUTE);
		stmt_list.add(ast.newExpressionStatement(mi2));

//		 String code = "try{" + stmt_name
//		 + ".execute();}catch(Exception _e){System.out.println("
//		 + stmt_name + ");throw _e;}";
//		 Block trys = (Block) createAST(code.toCharArray(),
//		 ASTParser.K_STATEMENTS);
//		 List list = ASTNode.copySubtrees(ast, trys.statements());
//		 stmt_list.addAll(list);

	}

	ArrayList<Statement> generate__sqlj_execute(MethodInvocation mi)
			throws ParserException {
		StructuralPropertyDescriptor loc = mi.getLocationInParent();
		LimitOption limitOption = null;
		List<Expression> params = mi.arguments();
		if (params.size() == 4) {
			// contains limit arguments
			limitOption = new LimitOption(
					(Expression) ASTNode.copySubtree(mi.getAST(), params.get(2)),
					(Expression) ASTNode.copySubtree(mi.getAST(), params.get(3)));
		}
		int sqlid = Integer.parseInt(params.get(0).toString());
		String stmt_name = parsedSource.genId("ps");
		AST ast = mi.getAST();
		SqlBlock sqljblock = parsedSource.getSqlById(sqlid);

		ParsedSql parsedSql = sqljblock.getParsedSql();

		ArrayList<Statement> generated_statements = new ArrayList<Statement>();
		// define Statement
		defineStatement(ast, parsedSql, stmt_name, generated_statements,
				limitOption);
		// bind parameters
		performParameterBinding(ast, parsedSql, generated_statements,
				stmt_name, limitOption);
		// execute
		executeStatement(ast, stmt_name, generated_statements);
		// set UPDATE_COUNT flag
		Assignment assi = ast.newAssignment();
		assi.setLeftHandSide(ast.newName(SQL_FLAG + ".UPDATECOUNT"));
		assi.setRightHandSide(newMethodInvocation(ast,
				ast.newSimpleName(stmt_name), METHOD_GET_UPDATE_COUNT));
		generated_statements.add(ast.newExpressionStatement(assi));
		// fetch output parameters
		performParameterFetching(ast, parsedSql, generated_statements,
				stmt_name);
		// set resultset
		String rs_name = params.get(1).toString();
		VariableDeclarationStatement rs_vds = newVariableDeclarationStatement(
				ast,
				java.sql.ResultSet.class.getSimpleName(),
				newVariableDeclarationFragment(
						ast,
						rs_name,
						newMethodInvocation(ast, ast.newSimpleName(stmt_name),
								METHOD_GET_RESULT_SET)));
		generated_statements.add(rs_vds);
		// put rs into list
		generated_statements.add(ast
				.newExpressionStatement(newMethodInvocation(ast,
						newMethodInvocation(ast, null, GET_SQLCALL_STACK),
						PUSH, ast.newSimpleName(rs_name))));
		// put Statement into list
		pushStmtIntoList(ast, stmt_name, generated_statements);
		return generated_statements;
	}

	void pushStmtIntoList(AST ast, String stmt_name, ArrayList<Statement> list) {
		list.add(ast.newExpressionStatement(newMethodInvocation(ast,
				newMethodInvocation(ast, null, GET_SQLCALL_STACK), PUSH,
				ast.newSimpleName(stmt_name))));
	}

	/**
	 * expression stands for <i>new
	 * ResultSetIterator&lt;typeName&gt;(rs_id,typeName.class)<i>
	 * 
	 * @param ast
	 * @param rs_id
	 * @param typeName
	 * @return
	 */

	Expression createNewResultSetIteratorExpression(AST ast, String rs_id,
			String typeName) {
		return newClassInstanceWithType(ast,
				ResultSetIterator.class.getSimpleName(), typeName,
				ast.newSimpleName(rs_id),
				ASTNodeUtil.newTypeLiteral(ast, typeName));
	}

	/**
	 * expression stands for the sql literal<br>
	 * StringLiteral or StringBuilder.toString() (if the sql is dynamic)<br>
	 * extra statements will add to <i>list<i>
	 * 
	 * @param ast
	 * @param psql
	 * @param list
	 * @param parsedSource
	 * @return
	 */
	Expression createSqlLiteralStatements(AST ast, ParsedSql psql,
			ArrayList<Statement> list) {
		if (!psql.isDynamic()) {
			return newStringLiteral(ast, psql.getFirstFragment());
		}
		String name = parsedSource.genId("sql");
		VariableDeclarationStatement vds_sb = newVariableDeclarationStatement(
				ast,
				StringBuilder.class.getSimpleName(),
				newVariableDeclarationFragment(
						ast,
						name,
						newClassInstance(ast,
								StringBuilder.class.getSimpleName())));
		list.add(vds_sb);
		List<String> fragments = psql.getFragments();
		List<Parameter> dyParas = psql.getDynamicParameters();
		for (int i = 0; i < fragments.size(); i++) {
			MethodInvocation mi_ = newMethodInvocation(ast, ast.newName(name),
					METHOD_APPEND, newStringLiteral(ast, fragments.get(i)));
			list.add(ast.newExpressionStatement(mi_));
			if (i < dyParas.size()) {

				mi_ = newMethodInvocation(ast, ast.newName(name),
						METHOD_APPEND,
						parseExpression(ast, dyParas.get(i).getExpression()));
				list.add(ast.newExpressionStatement(mi_));
			}
		}
		return newMethodInvocation(ast, ast.newName(name), METHOD_TO_STRING);
	}

	void performParameterBinding(AST ast, ParsedSql psql,
			ArrayList<Statement> list, String stmt_name, Object extraOption) {
		int i = 1;
		// parameter binding
		for (Parameter p : psql.getBindParameters()) {
			if (p.getType() == Parameter.OUT) {
				MethodInvocation mi1 = newMethodInvocation(
						ast,
						ast.newSimpleName(stmt_name),
						METHOD_REGISTER_OUT_PARAMETER,
						ast.newNumberLiteral("" + i),
						newQualifiedName(ast, java.sql.Types.class
								.getSimpleName(), parsedSource
								.getVariableSqlType(p.getExpression())));
				list.add(ast.newExpressionStatement(mi1));
			} else {
				MethodInvocation mi1 = newMethodInvocation(
						ast,
						ast.newSimpleName(stmt_name),
						"set"
								+ parsedSource.getVariableType(
										p.getExpression(), "Object"),
						ast.newNumberLiteral("" + i),
						parseExpression(ast, p.getExpression()));
				list.add(ast.newExpressionStatement(mi1));
			}
			i++;
		}
		if (extraOption instanceof LimitOption) {
			LimitOption limitOption = (LimitOption) extraOption;
			Expression callBindExp = ASTNodeUtil.newMethodInvocation(ast,
					ast.newSimpleName(SQL_FLAG), SqlFlag.PREPARE_LIMIT_PARA,
					ast.newSimpleName(stmt_name), limitOption.start,
					limitOption.end, ast.newNumberLiteral("" + i));
			list.add(ast.newExpressionStatement(callBindExp));
		}
	}

	void performParameterFetching(AST ast, ParsedSql psql,
			ArrayList<Statement> list, String stmt_name) {
		int i = 1;
		for (Parameter p : psql.getBindParameters()) {
			if (p.getType() == Parameter.OUT) {
				Assignment assi2 = ast.newAssignment();
				assi2.setLeftHandSide(parseExpression(ast, p.getExpression()));
				assi2.setRightHandSide(newMethodInvocation(
						ast,
						ast.newSimpleName(stmt_name),
						"get" + parsedSource.getVariableType(p.getExpression()),
						ast.newNumberLiteral("" + i)));
				list.add(ast.newExpressionStatement(assi2));
			}
			i++;
		}
	}

	ArrayList<Statement> generate__sqlj_execute_for(MethodInvocation mi) {
		ArrayList<Statement> list = new ArrayList<Statement>();
		list = generate__sqlj_execute_para(mi);
		return list;
	}

	ArrayList<Statement> generate__sqlj_execute_para(MethodInvocation mi) {
		ArrayList<Statement> list = null;
		try {
			list = generate__sqlj_execute(mi);
			ExpressionStatement es = (ExpressionStatement) list
					.get(list.size() - 2);// the last statement is
											// __sqlj_cs_gen2.execute();
			MethodInvocation es_mi = (MethodInvocation) es.getExpression();
			String ps_id = es_mi.getExpression().toString();
			String rs_id = mi.arguments().get(1).toString();
			AST ast = mi.getAST();
			VariableDeclarationStatement vds = newVariableDeclarationStatement(
					ast,
					java.sql.ResultSet.class.getSimpleName(),
					newVariableDeclarationFragment(
							ast,
							rs_id,
							newMethodInvocation(ast, ast.newSimpleName(ps_id),
									METHOD_GET_RESULT_SET)));
			list.add(vds);
		} catch (ParserException e) {
			throw new RuntimeException(e);
		}// new ArrayList<Statement>();
		return list;
	}

	static private class CompilationUnitImpl implements ICompilationUnit {
		private CompilationUnit unit;

		CompilationUnitImpl(CompilationUnit unit) {
			this.unit = unit;
		}

		public char[] getContents() {
			char[] contents = null;
			try {
				Document doc = new Document();
				TextEdit edits = unit.rewrite(doc, null);
				edits.apply(doc);
				String sourceCode = doc.get();
				if (sourceCode != null)
					contents = sourceCode.toCharArray();
			} catch (BadLocationException e) {
				throw new RuntimeException(e);
			}
			return contents;
		}

		public char[] getMainTypeName() {
			TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
			return classType.getName().getFullyQualifiedName().toCharArray();
		}

		public char[][] getPackageName() {
			String[] names = getSimpleNames(this.unit.getPackage().getName()
					.getFullyQualifiedName());
			char[][] packages = new char[names.length][];
			for (int i = 0; i < names.length; ++i)
				packages[i] = names[i].toCharArray();
			return packages;
		}

		private String[] getSimpleNames(String fullQname) {
			return fullQname.split("\\.");
		}

		public char[] getFileName() {
			String name = new String(getMainTypeName()) + ".java";
			return name.toCharArray();
		}

		@Override
		public boolean ignoreOptionalProblems() {
			return false;
		}
	}

	private static class NameEnvironmentImpl implements INameEnvironment {

		public NameEnvironmentImpl(CompilationUnit unit) {

		}

		@Override
		public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NameEnvironmentAnswer findType(char[] typeName,
				char[][] packageName) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isPackage(char[][] parentPackageName, char[] packageName) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void cleanup() {
			// TODO Auto-generated method stub

		}

	}

	private static class CompilerRequestorImpl implements ICompilerRequestor {

		@Override
		public void acceptResult(CompilationResult result) {
			System.out.println(result);
		}

	}

	private void addComment(final CompilationUnit unit, final ASTRewrite rewrite) {
		unit.accept(new ASTVisitor() {

			@Override
			public boolean visit(VariableDeclarationStatement node) {
				if (node.getType().toString().equals("PreparedStatement")) {
					StructuralPropertyDescriptor location = node
							.getLocationInParent();
					if (location.isChildListProperty()) {
						ASTNode parent = node.getParent();
						List list = (List) parent
								.getStructuralProperty(location);
						ListRewrite listRewrite = rewrite.getListRewrite(
								parent, (ChildListPropertyDescriptor) location);
						Statement placeHolder = (Statement) rewrite
								.createStringPlaceholder("//mycomment",
										ASTNode.EMPTY_STATEMENT);
						listRewrite.insertBefore(placeHolder, node, null);
					}
				}
				return super.visit(node);
			}

		});
	}
}
