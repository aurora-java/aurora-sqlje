package aurora.sqlje.ast;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import aurora.sqlje.core.ISqlCallEnabled;
import aurora.sqlje.exception.ParserException;

public class ImportOrganizer {
	/**
	 * String:something to import<br>
	 * Boolean: true means import package(xxx.*),false means import class only;
	 */
	private static HashMap<String, Boolean> required = new HashMap<String, Boolean>();
	static {
		required.put(ResultSet.class.getPackage().getName(), true);//java.sql.*
		required.put(ISqlCallEnabled.class.getPackage().getName(), true);//aurora.sqlje.core.*
		required.put(ParserException.class.getPackage().getName(), true);//aurora.sqlje.exception.*
		required.put(java.util.Map.class.getName(), false);//java.util.Map
		required.put(java.util.List.class.getName(), false);//java.util.List
		try {
			Class cls = Class.forName("uncertain.composite.CompositeMap");
			required.put(cls.getPackage().getName(), true);//uncertain.composite.*
		} catch (Exception e) {

		}
	}
	private List<ImportDeclaration> importList;
	private AST ast;

	public ImportOrganizer(CompilationUnit result) {
		this.importList = result.imports();
		this.ast = result.getAST();
	}

	public void organize() {
		Set<String> sets = new HashSet<String>(required.keySet());
		for (ImportDeclaration id : importList) {
			sets.remove(id.getName());
		}
		for (String i : sets) {
			ImportDeclaration id = ast.newImportDeclaration();
			id.setName(ast.newName(i));
			id.setOnDemand(required.get(i));
			importList.add(id);
		}
	}

	private void detectSpecial(CompilationUnit unit) {
		unit.accept(new ASTVisitor() {

			@Override
			public boolean visit(SingleVariableDeclaration node) {
				return super.visit(node);
			}

			@Override
			public boolean visit(VariableDeclarationStatement node) {
				return super.visit(node);
			}

			public boolean visit(MethodDeclaration node) {
				List<IExtendedModifier> modifiers = node.modifiers();
				for (IExtendedModifier em : modifiers) {
					if (AutonomousWrapper.isAutonomousAnnotation(em)) {
						
					}
				}
				return super.visit(node);
			}

		});
	}

}
