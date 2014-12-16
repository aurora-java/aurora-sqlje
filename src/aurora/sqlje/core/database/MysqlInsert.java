package aurora.sqlje.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import aurora.sqlje.core.ISqlCallStack;

public class MysqlInsert extends AbstractInsert {

	public MysqlInsert(ISqlCallStack context, Map map, String tableName,
			String pkName) {
		super(context, map, tableName, pkName);
	}

	public MysqlInsert(ISqlCallStack context, Object bean) {
		super(context, bean);
	}

	public MysqlInsert(ISqlCallStack context, Object bean, String tableName,
			String pkName) {
		super(context, bean, tableName, pkName);
	}

	@Override
	protected Object execute(PreparedStatement ps) throws SQLException {
		ps.execute();
		ResultSet rs = ps.getGeneratedKeys();
		if (rs != null && rs.next()) {
			return rs.getLong(1);
		}
		return null;
	}

	@Override
	protected String getDateExpression() {
		return "curdate()";
	}

	@Override
	protected String getTimeExpression() {
		return "now()";
	}

	@Override
	protected String getInsertExpressionForPk() {
		return null;
	}

	@Override
	protected PreparedStatement createStatement(Connection conn, String sql)
			throws SQLException {
		return conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	}

}
