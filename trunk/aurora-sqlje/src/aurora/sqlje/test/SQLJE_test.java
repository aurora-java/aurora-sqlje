package aurora.sqlje.test;

import uncertain.composite.CompositeMap;
import java.util.List;
import java.sql.*;
import aurora.sqlje.exception.*;
import java.util.Map;
import aurora.sqlje.core.*;

/**
 * dasdasdadadad adasdas
 */
public class SQLJE_test implements aurora.sqlje.core.ISqlCallEnabled {
	public void hello(CompositeMap param) throws Exception {
		PreparedStatement _$sqlje_ps_gen4 = getSqlCallStack()
				.getCurrentConnection().prepareStatement(
						"select * from sys_user");
		_$sqlje_ps_gen4.execute();
		$sql.UPDATECOUNT = _$sqlje_ps_gen4.getUpdateCount();
		ResultSet _$sqlje_rs_gen0 = _$sqlje_ps_gen4.getResultSet();
		getSqlCallStack().push(_$sqlje_rs_gen0);
		getSqlCallStack().push(_$sqlje_ps_gen4);
		for (CompositeMap m : new ResultSetIterator<CompositeMap>(
				_$sqlje_rs_gen0, CompositeMap.class)) {
			System.out.println("Hello " + m.getString("user_name") + " "
					+ m.getInt("user_id"));
			param.createChild("record").put("user_name", m.get("user_name"));
		}
		long id = 21;
		PreparedStatement _$sqlje_ps_gen3 = getSqlCallStack()
				.getCurrentConnection().prepareStatement(
						"SELECT * FROM sys_user WHERE user_id=? FOR UPDATE");
		_$sqlje_ps_gen3.setLong(1, id);
		_$sqlje_ps_gen3.execute();
		getSqlCallStack().push(_$sqlje_ps_gen3);
		PreparedStatement _$sqlje_ps_gen5 = getSqlCallStack()
				.getCurrentConnection().prepareStatement(
						"select * from sys_user");
		_$sqlje_ps_gen5.execute();
		$sql.UPDATECOUNT = _$sqlje_ps_gen5.getUpdateCount();
		ResultSet _$sqlje_rs_gen1 = _$sqlje_ps_gen5.getResultSet();
		getSqlCallStack().push(_$sqlje_rs_gen1);
		getSqlCallStack().push(_$sqlje_ps_gen5);
		List<Map> a = DataTransfer.transferAll(List.class, Map.class,
				_$sqlje_rs_gen1);
		System.out.println(a);
		Map am = new java.util.HashMap();
		am.put("name", "Spider");
		new aurora.sqlje.core.database.MysqlInsert(getSqlCallStack(), am,
				"animals", "id").insert();
	}

	@Autonomous
	public int aaa() throws Exception {
		Connection _$autonomous_connection_ = getSqlCallStack()
				.createConnection();
		Exception _$autonomous_exception_ = null;
		try {
			try {
				PreparedStatement _$sqlje_ps_gen6 = getSqlCallStack()
						.getCurrentConnection().prepareStatement(
								"select sysdate from dual");
				_$sqlje_ps_gen6.execute();
				$sql.UPDATECOUNT = _$sqlje_ps_gen6.getUpdateCount();
				ResultSet _$sqlje_rs_gen2 = _$sqlje_ps_gen6.getResultSet();
				getSqlCallStack().push(_$sqlje_rs_gen2);
				getSqlCallStack().push(_$sqlje_ps_gen6);
			} catch (Exception e) {
				throw e;
			}
			return 1;
		} catch (Exception e) {
			_$autonomous_exception_ = e;
			throw e;
		} finally {
			if (_$autonomous_exception_ == null)
				_$autonomous_connection_.commit();
			else
				_$autonomous_connection_.rollback();
			getSqlCallStack().free(_$autonomous_connection_);
		}
	}

	protected aurora.sqlje.core.ISqlCallStack _$sqlje_sqlCallStack = null;
	protected aurora.sqlje.core.IInstanceManager _$sqlje_instanceManager = null;
	protected SqlFlag $sql = new SqlFlag();

	public aurora.sqlje.core.IInstanceManager getInstanceManager() {
		return _$sqlje_instanceManager;
	}

	public void _$setInstanceManager(aurora.sqlje.core.IInstanceManager args0) {
		_$sqlje_instanceManager = args0;
	}

	public void _$setSqlCallStack(aurora.sqlje.core.ISqlCallStack args0) {
		_$sqlje_sqlCallStack = args0;
	}

	public aurora.sqlje.core.ISqlCallStack getSqlCallStack() {
		return _$sqlje_sqlCallStack;
	}
}
