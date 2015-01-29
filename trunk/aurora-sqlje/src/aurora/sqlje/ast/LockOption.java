package aurora.sqlje.ast;

public class LockOption {
	public String tableName;
	public String whereClause;

	public LockOption(String tableName, String whereClause) {
		this.tableName = tableName;
		this.whereClause = whereClause;
	}
}
