package aurora.sqlje.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import aurora.sqlje.core.ISqlCallStack;

public class UpdateOperation {

	private ISqlCallStack sqlCallStack;
	private String tableName;
	private String pkName;
	private Map map;
	private Object bean;

	public UpdateOperation(ISqlCallStack callStack, Object bean) {
		super();
		sqlCallStack = callStack;
		this.bean = bean;
	}

	public UpdateOperation(ISqlCallStack callStack, Object bean,
			String tableName, String pkName) {
		super();
		this.sqlCallStack = callStack;
		this.bean = bean;
		this.tableName = tableName;
		this.pkName = pkName;
	}

	public UpdateOperation(ISqlCallStack callStack, Map map, String tableName,
			String pkName) {
		this.sqlCallStack = callStack;
		this.map = map;
		this.tableName = tableName;
		this.pkName = pkName;
	}

	/**
	 * 
	 * @return update count
	 */
	public int doUpdate() throws SQLException, Exception {
		if (bean == null && map == null)
			throw new IllegalArgumentException(
					"Can not do update without data model(bean or map)");
		if (tableName == null || tableName.length() == 0)
			throw new IllegalArgumentException(
					"Can not do update without tableName");
		if (pkName == null || pkName.length() == 0)
			throw new IllegalArgumentException(
					"Can not do update without pkName");

		String parsedSql = generateUpdateSql();
		Connection conn = sqlCallStack.getCurrentConnection();
		PreparedStatement ps = conn.prepareStatement(parsedSql);
		try {

		} finally {
			if (ps != null)
				try {
					ps.close();
				} catch (Exception e) {
				}
		}
		return -1;
	}

	private String generateUpdateSql() {
		if (bean != null)
			return generateUpdateSqlForBean();
		if (map != null)
			return generateUpdateSqlForMap();
		return null;
	}

	private String generateUpdateSqlForBean() {
		return "";
	}

	private String generateUpdateSqlForMap() {
		return "";
	}

}
