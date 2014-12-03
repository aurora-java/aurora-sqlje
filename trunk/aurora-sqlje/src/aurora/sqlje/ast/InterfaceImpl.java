package aurora.sqlje.ast;

import static aurora.sqlje.ast.ASTNodeUtil.newSimpleType;
import static aurora.sqlje.ast.ASTNodeUtil.newVariableDeclarationFragment;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

import aurora.sqlje.core.ISqlCallEnabled;
import aurora.sqlje.core.ISqlCallStack;
import aurora.sqlje.core.SqlFlag;

public class InterfaceImpl {
	public static final String SQLCALLSTACK_NAME = "_$sqlje_sqlcallstack_";

	private HashMap<Class<?>, String> variableTypeMap = new HashMap<Class<?>, String>();

	private TypeDeclaration typeDec;
	private AST ast;

	public InterfaceImpl(TypeDeclaration td) {
		this.typeDec = td;
		this.ast = td.getAST();
	}

	public void addDefaultImpl() {
		// add necessary implements
		typeDec.superInterfaceTypes()
				.add(ASTNodeUtil.newSimpleType(ast,
						ISqlCallEnabled.class.getName()));
		// create necessary fields

		List<BodyDeclaration> bodys = typeDec.bodyDeclarations();

		//variableTypeMap.put(Integer.class, AstTransform.UPDATE_COUNT);


		List<MethodDeclaration> methods = new ArrayList<MethodDeclaration>();
		for (Method m : ISqlCallEnabled.class.getMethods()) {

			MethodDeclaration md = createFromMethod(ast, m);

			Block body = createSimpleAssignOrReturnBlock(md, m);
			md.setBody(body);
			methods.add(md);
		}
		for(Class<?> c:variableTypeMap.keySet()) {
			createField(typeDec, c, variableTypeMap.get(c), false);
		}
		createSqlFlag(ast, typeDec);
		bodys.addAll(methods);
	}

	private void createField(TypeDeclaration td, Class<?> type, String name,
			boolean init) {
		AST ast = td.getAST();
		FieldDeclaration fd = ast
				.newFieldDeclaration(newVariableDeclarationFragment(ast, name,
						null));
		fd.setType(newSimpleType(ast, type.getName()));
		fd.modifiers().add(ast.newModifier(ModifierKeyword.PROTECTED_KEYWORD));
		td.bodyDeclarations().add(fd);
	}
	
	/**
	 *  SqlFlag $sql = new SqlFlag();
	 * 
	 * @param ast
	 * @return
	 */
	void createSqlFlag(AST ast,TypeDeclaration td) {
		String sqlFlagClassName = SqlFlag.class.getSimpleName();

		FieldDeclaration fd = ast.newFieldDeclaration(ASTNodeUtil.newVariableDeclarationFragment(ast,
								AstTransform.SQL_FLAG, ASTNodeUtil.newClassInstance(ast,
										sqlFlagClassName)));
		fd.setType(newSimpleType(ast, sqlFlagClassName));
		fd.modifiers().add(ast.newModifier(ModifierKeyword.PROTECTED_KEYWORD));
		td.bodyDeclarations().add(fd);
		//return vds;
	}

	public static MethodDeclaration createFromMethod(AST ast, Method m) {
		MethodDeclaration md = ast.newMethodDeclaration();
		md.setName(ast.newSimpleName(m.getName()));
		if (m.getReturnType() != void.class)
			md.setReturnType2(ast.newSimpleType(ast.newName(m.getReturnType()
					.getName())));
		md.modifiers().addAll(ast.newModifiers(Modifier.PUBLIC));
		for (Class c : m.getExceptionTypes()) {
			md.thrownExceptions().add(ast.newName(c.getName()));
		}
		int i_ = 0;
		for (Class c : m.getParameterTypes()) {
			md.parameters().add(
					ASTNodeUtil.newSingleVariableDeclaration(ast,
							ASTNodeUtil.newSimpleType(ast, c.getName()), "args"
									+ i_, null));
		}
		return md;
	}

	private Block createSimpleAssignOrReturnBlock(MethodDeclaration md, Method m) {
		Block block = ast.newBlock();
		int i = 0;
		List<SingleVariableDeclaration> params = md.parameters();
		for (Class<?> type : m.getParameterTypes()) {
			String varName = getVarName(type, true);
			block.statements().add(
					ast.newExpressionStatement(ASTNodeUtil.newSimpleAssignment(
							ast, varName, params.get(i).getName().toString())));
		}
		if (m.getReturnType() != void.class) {
			String retName = getVarName(m.getReturnType(), true);
			ReturnStatement rs = ast.newReturnStatement();
			rs.setExpression(ast.newSimpleName(retName));
			block.statements().add(rs);
		}
		return block;
	}

	private String getVarName(Class<?> type, boolean create) {
		String varName = variableTypeMap.get(type);

		if (varName == null && create) {
			String typeName = type.getSimpleName();
			int idx = 0;
			// interface maybe
			if (typeName.charAt(0) == 'I'
					&& Character.isUpperCase(typeName.charAt(1)))
				idx = 1;
			typeName = Character.toLowerCase(typeName.charAt(idx))
					+ typeName.substring(idx + 1);
			varName = "_$sqlje_" + typeName;
			variableTypeMap.put(type, varName);
		}

		return varName;
	}
}
