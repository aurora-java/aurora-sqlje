package aurora.sqlje.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import uncertain.ocm.IObjectRegistry;
import uncertain.ocm.OCManager;
import aurora.sqlje.core.database.AbstractInsert;
import aurora.sqlje.core.database.IDatabaseDescriptor;
import aurora.sqlje.core.database.MysqlInsert;
import aurora.sqlje.core.database.OracleInsert;

public class SqlFlag {
	public static final String PREPARE_LIMIT_SQL = "_$prepareLimitSql";
	public static final String PREPARE_LIMIT_PARA = "_$prepareLimitParaBinding";

	public IInstanceManager instManager;
	public ISqlCallStack callStack;

	public static final String CLEAR = "clear";
	public int UPDATECOUNT = 0;

	public SqlFlag() {

	}

	public void setInstanceManager(IInstanceManager instMgr) {
		this.instManager = instMgr;
	}

	public void setSqlCallStack(ISqlCallStack callStack) {
		this.callStack = callStack;
	}

	public void clear() {

	}

	public int rowrount() {
		return UPDATECOUNT;
	}

	public String _$prepareLimitSql(String osql) {
		IDatabaseDescriptor dbDescriptor = callStack.getDatabaseDescriptor();
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
		IDatabaseDescriptor dbDesc = callStack.getDatabaseDescriptor();
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
		IObjectRegistry reg = ((InstanceManager) instManager)
				.getObjectRegistry();
		IDatabaseDescriptor dbDesc = callStack.getDatabaseDescriptor();
		AbstractInsert insert = null;
		if (dbDesc.isOracle())
			insert = new OracleInsert(callStack, bean, tableName, pkName);
		else
			insert = new MysqlInsert(callStack, bean, tableName, pkName);

		OCManager ocm = (OCManager) reg.getInstanceOfType(OCManager.class);
		insert.setReflectionMapper(ocm.getReflectionMapper());
		insert.insert();
	}

	public void insert(Map map, String tableName, String pkName)
			throws Exception {
		IObjectRegistry reg = ((InstanceManager) instManager)
				.getObjectRegistry();
		IDatabaseDescriptor dbDesc = callStack.getDatabaseDescriptor();
		AbstractInsert insert = null;
		if (dbDesc.isOracle())
			insert = new OracleInsert(callStack, map, tableName, pkName);
		else
			insert = new MysqlInsert(callStack, map, tableName, pkName);

		OCManager ocm = (OCManager) reg.getInstanceOfType(OCManager.class);
		insert.setReflectionMapper(ocm.getReflectionMapper());
		insert.insert();
	}

}
