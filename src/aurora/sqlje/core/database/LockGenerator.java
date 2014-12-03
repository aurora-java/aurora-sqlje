package aurora.sqlje.core.database;


public class LockGenerator {

	public static String generateLockSql(IDatabaseDescriptor dbDesc,
			String tableName, String whereClause) {
		if (dbDesc.isOracle() || dbDesc.isMysql())
			return generateSelectForUpdateSql(tableName, whereClause);
		else if (dbDesc.isSqlServer())
			return generateWithLockSql(tableName, whereClause);
		return null;
	}

	/**
	 * used for oracle,mysql
	 * 
	 * @param tableName
	 * @param whereClause
	 *            can be null
	 * @return
	 */
	public static String generateSelectForUpdateSql(String tableName,
			String whereClause) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM ").append(tableName);
		if (whereClause != null) {
			sb.append(" WHERE ");
			sb.append(whereClause);
		}
		sb.append(" FOR UPDATE");
		return sb.toString();
	}

	/**
	 * used for sqlserver
	 * 
	 * @param tableName
	 * @param whereClause
	 *            when null use HOLDLOCK else use ROWLOCK
	 * @return
	 */
	public static String generateWithLockSql(String tableName,
			String whereClause) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM ").append(tableName);
		if (whereClause != null) {
			sb.append(" WITH(ROWLOCK)");
			sb.append(" WHERE ").append(whereClause);
		} else {
			sb.append(" WITH(HOLDLOCK)");
		}
		return sb.toString();
	}
}
