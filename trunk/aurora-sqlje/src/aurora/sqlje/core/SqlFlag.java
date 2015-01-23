package aurora.sqlje.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import uncertain.ocm.IObjectRegistry;
import uncertain.ocm.OCManager;
import aurora.sqlje.core.database.AbstractInsertOperation;
import aurora.sqlje.core.database.DeleteOperation;
import aurora.sqlje.core.database.IDatabaseDescriptor;
import aurora.sqlje.core.database.MysqlInsert;
import aurora.sqlje.core.database.OracleInsert;

public class SqlFlag {
	public static final String PREPARE_LIMIT_SQL = "_$prepareLimitSql";
	public static final String PREPARE_LIMIT_PARA = "_$prepareLimitParaBinding";


	public static final String CLEAR = "clear";
	public int UPDATECOUNT = 0;
	private ISqlCallEnabled self_sqlje;

	public SqlFlag(ISqlCallEnabled self_sqlje) {
		this.self_sqlje=self_sqlje;
	}


	public void clear() {

	}

	public int rowcount() {
		return UPDATECOUNT;
	}

	public String _$prepareLimitSql(String osql) {
		IDatabaseDescriptor dbDescriptor = self_sqlje.getSqlCallStack().getDatabaseDescriptor();
		StringBuilder sb = new StringBuilder(osql.length() + 100);
		if (!dbDescriptor.isOracle()) {
			sb.append(osql).append("  LIMIT ?,?");
		} else {
			sb.append("SELECT * FROM (SELECT z0.*,rownum rn FROM (\n");
			sb.append(osql);
			sb.append("\n) z0 WHERE rownum < ? ) WHERE rn > ?");
		}
		// System.out.println(sb);
		return sb.toString();
	}

	public void _$prepareLimitParaBinding(PreparedStatement ps, int start,
			int end, int startIdx) throws SQLException {
		IDatabaseDescriptor dbDesc = self_sqlje.getSqlCallStack().getDatabaseDescriptor();
		if (dbDesc.isOracle()) {
			ps.setInt(startIdx, end);
			ps.setInt(startIdx + 1, start);
		} else {
			ps.setInt(startIdx, start);
			ps.setInt(startIdx + 1, end);
		}
	}

	public void insert(Object bean) throws Exception {
		insert(bean, null, null);
	}

	public void insert(Object bean, String tableName, String pkName)
			throws Exception {
		IObjectRegistry reg = ((InstanceManager) self_sqlje.getInstanceManager())
				.getObjectRegistry();
		ISqlCallStack callStack = self_sqlje.getSqlCallStack();
		IDatabaseDescriptor dbDesc = callStack.getDatabaseDescriptor();
		AbstractInsertOperation insert = null;
		if (dbDesc.isOracle())
			insert = new OracleInsert(callStack, bean, tableName, pkName);
		else
			insert = new MysqlInsert(callStack, bean, tableName, pkName);

		OCManager ocm = (OCManager) reg.getInstanceOfType(OCManager.class);
		insert.setReflectionMapper(ocm.getReflectionMapper());
		insert.doInsert();
	}

	public void insert(Map map, String tableName, String pkName)
			throws Exception {
		IObjectRegistry reg = ((InstanceManager) self_sqlje.getInstanceManager())
				.getObjectRegistry();
		ISqlCallStack callStack = self_sqlje.getSqlCallStack();
		IDatabaseDescriptor dbDesc = callStack.getDatabaseDescriptor();
		AbstractInsertOperation insert = null;
		if (dbDesc.isOracle())
			insert = new OracleInsert(callStack, map, tableName, pkName);
		else
			insert = new MysqlInsert(callStack, map, tableName, pkName);

		OCManager ocm = (OCManager) reg.getInstanceOfType(OCManager.class);
		insert.setReflectionMapper(ocm.getReflectionMapper());
		insert.doInsert();
	}

	public void delete(String tableName, String pkName, Object pk)
			throws Exception {
		new DeleteOperation(self_sqlje.getSqlCallStack(), tableName, pkName, pk).doDelete();
	}

	public void delete(Object bean) throws Exception {
		new DeleteOperation(self_sqlje.getSqlCallStack(), bean).doDelete();
	}

}
