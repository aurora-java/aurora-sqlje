package aurora.sqlje.ast;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
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
		required.put(ResultSet.class.getPackage().getName(), true);
		required.put(ISqlCallEnabled.class.getPackage().getName(), true);
		required.put(ParserException.class.getPackage().getName(), true);
		required.put(java.util.Map.class.getName(), false);
		try {
			Class cls=Class.forName("uncertain.composite.CompositeMap");
			required.put(cls.getPackage().getName(), true);
		}catch(Exception e) {
			
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
	
	private void collectFavouriteClass(CompilationUnit unit) {
		unit.accept(new ASTVisitor() {

			@Override
			public boolean visit(SingleVariableDeclaration node) {
				return super.visit(node);
			}

			@Override
			public boolean visit(VariableDeclarationStatement node) {
				return super.visit(node);
			}
			
		});
	}

}
