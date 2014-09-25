package aurora.sqlje.core.database;

import aurora.sqlje.core.ISqlCallStack;

public class LockGenerator {
	/**
	 * lock rows specified by whereClause<br>
	 * <strong>NOTE.</strong><br>
	 * database maybe escalation ROW_LOCK to TABLE_LOCK
	 * 
	 * @param callStack
	 * @param tableName
	 * @param whereClause
	 * @throws Exception
	 */
	public static void $lock(ISqlCallStack callStack, String tableName,
			String whereClause) throws Exception {

	}

	/**
	 * lock whole table
	 * 
	 * @param callStack
	 * @param tableName
	 * @throws Exception
	 */
	public static void $lock(ISqlCallStack callStack, String tableName)
			throws Exception {
		$lock(callStack, tableName, null);
	}

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
	private static String generateWithLockSql(String tableName,
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
