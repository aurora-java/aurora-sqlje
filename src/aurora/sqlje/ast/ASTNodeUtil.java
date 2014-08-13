package aurora.sqlje.ast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class ASTNodeUtil {

	/**
	 * create an ParenthesizedExpression via a expression string<br>
	 * the expression can be complex.
	 * 
	 * @param ast
	 * @param expr
	 *            complex expression e.g. <u>a.name+'-'+a.address<u>
	 * @return
	 */
	public static Expression parseExpression(AST ast, String expr) {
		AST a = ast.newParenthesizedExpression().getAST();
		return (Expression) ASTNode.copySubtree(a,
				createAST(expr.toCharArray(), ASTParser.K_EXPRESSION));
	}

	public static ClassInstanceCreation newClassInstance(AST ast,
			String className, Expression... args) {
		ClassInstanceCreation inst = ast.newClassInstanceCreation();
		inst.setType(ast.newSimpleType(ast.newName(className)));
		for (Expression e : args) {
			inst.arguments().add(e);
		}
		return inst;
	}

	public static Expression newClassInstanceWithType(AST ast,
			String className, String typeName, Expression... args) {
		ClassInstanceCreation cic = ast.newClassInstanceCreation();
		ParameterizedType pt = newParameterizedType(ast, className, typeName);
		cic.setType(pt);
		for (Expression e : args) {
			cic.arguments().add(e);
		}
		return cic;
	}

	public static QualifiedName newQualifiedName(AST ast, String leftPart,
			String rightPart) {
		return ast.newQualifiedName(ast.newName(leftPart),
				ast.newSimpleName(rightPart));
	}

	public static VariableDeclarationFragment newVariableDeclarationFragment(
			AST ast, String name, Expression init) {
		VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
		vdf.setName(ast.newSimpleName(name));
		if (init == null)
			init = ast.newNullLiteral();
		vdf.setInitializer(init);
		return vdf;
	}

	public static VariableDeclarationStatement newVariableDeclarationStatement(
			AST ast, String typeName, VariableDeclarationFragment... vdfs) {
		VariableDeclarationStatement vds = ast
				.newVariableDeclarationStatement(vdfs[0]);
		vds.setType(ast.newSimpleType(ast.newName(typeName)));
		for (int i = 1; i < vdfs.length; i++)
			vds.fragments().add(vdfs[i]);
		return vds;
	}

	public static SingleVariableDeclaration newSingleVariableDeclaration(
			AST ast, Type type, String name, Expression init) {
		SingleVariableDeclaration svd = ast.newSingleVariableDeclaration();
		svd.setType(type);
		svd.setName(ast.newSimpleName(name));
		if (init != null)
			svd.setInitializer(init);
		return svd;
	}

	public static MethodInvocation newMethodInvocation(AST ast, Expression exp,
			String name, Expression... args) {
		MethodInvocation mi = ast.newMethodInvocation();
		mi.setExpression(exp);
		mi.setName(ast.newSimpleName(name));
		for (Expression e : args) {
			mi.arguments().add(e);
		}
		return mi;
	}

	public static TypeLiteral newTypeLiteral(AST ast, String type) {
		TypeLiteral tl = ast.newTypeLiteral();
		if (isPrimitiveType(type))
			tl.setType(ast.newPrimitiveType(PrimitiveType.toCode(type)));
		else {
			tl.setType(ast.newSimpleType(ast.newName(type)));
		}
		return tl;
	}

	public static Type newSimpleType(AST ast, String type) {
		return ast.newSimpleType(ast.newName(type));
	}

	public static ParameterizedType newParameterizedType(AST ast,
			String className, String typeName) {
		ParameterizedType pt = ast.newParameterizedType(ast.newSimpleType(ast
				.newName(className)));
		pt.typeArguments().add(ast.newSimpleType(ast.newName(typeName)));
		return pt;
	}

	private static final Set<String> primitive_types = new HashSet<String>(
			Arrays.asList("int", "double", "long", "short", "byte", "char",
					"boolean", "float"));

	public static boolean isPrimitiveType(String type) {
		return primitive_types.contains(type);
	}

	public static StringLiteral newStringLiteral(AST ast, String value) {
		StringLiteral sl = ast.newStringLiteral();
		sl.setLiteralValue(value);
		return sl;
	}

	public static Assignment newSimpleAssignment(AST ast, String left,
			String right) {
		Assignment assi = ast.newAssignment();
		assi.setLeftHandSide(ast.newName(left));
		assi.setRightHandSide(ast.newName(right));
		return assi;
	}

	public static <T extends ASTNode> T createAST(char[] cs, int kind) {
		long t0 = System.currentTimeMillis();
		ASTParser parser = ASTParser.newParser(AstTransform.API_LEVEL);
		parser.setKind(kind);
		parser.setSource(cs);
		Map options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
		parser.setCompilerOptions(options);
		T r = (T) parser.createAST(null);
		// System.out.println(System.currentTimeMillis() - t0);
		return r;
	}
}
