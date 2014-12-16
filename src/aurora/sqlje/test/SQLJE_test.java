package aurora.sqlje.test;

import uncertain.composite.CompositeMap;
import java.util.List;
import java.sql.*;
import aurora.sqlje.exception.*;
import java.util.Map;
import uncertain.composite.*;
import aurora.sqlje.core.*;

/**
 * dasdasdadadad adasdas
 */
public class SQLJE_test implements aurora.sqlje.core.ISqlCallEnabled {
	public void hello(CompositeMap param) throws Exception {
		aurora.sqlje.test.Animal a = new aurora.sqlje.test.Animal();
		a.name = "cat";
		$sql.insert(a);
		System.out.println(a.id);
	}

	public int aaa() throws Exception {
		String a = "%dd%";
		int start = 2;
		int end = 10;
		String _$sqlje_sql_gen3 = "select * from sys_user where user_name like ?";
		_$sqlje_sql_gen3 = $sql._$prepareLimitSql(_$sqlje_sql_gen3);
		PreparedStatement _$sqlje_ps_gen2 = getSqlCallStack()
				.getCurrentConnection().prepareStatement(_$sqlje_sql_gen3);
		_$sqlje_ps_gen2.setString(1, a);
		$sql._$prepareLimitParaBinding(_$sqlje_ps_gen2, start, end, 2);
		$sql.clear();
		_$sqlje_ps_gen2.execute();
		$sql.UPDATECOUNT = _$sqlje_ps_gen2.getUpdateCount();
		ResultSet _$sqlje_rs_gen0 = _$sqlje_ps_gen2.getResultSet();
		getSqlCallStack().push(_$sqlje_rs_gen0);
		getSqlCallStack().push(_$sqlje_ps_gen2);
		ResultSet rs = _$sqlje_rs_gen0;
		String _$sqlje_sql_gen5 = "select * from sys_user where user_name like ?";
		_$sqlje_sql_gen5 = $sql._$prepareLimitSql(_$sqlje_sql_gen5);
		PreparedStatement _$sqlje_ps_gen4 = getSqlCallStack()
				.getCurrentConnection().prepareStatement(_$sqlje_sql_gen5);
		_$sqlje_ps_gen4.setString(1, a);
		$sql._$prepareLimitParaBinding(_$sqlje_ps_gen4, start + 1, end, 2);
		$sql.clear();
		_$sqlje_ps_gen4.execute();
		$sql.UPDATECOUNT = _$sqlje_ps_gen4.getUpdateCount();
		ResultSet _$sqlje_rs_gen1 = _$sqlje_ps_gen4.getResultSet();
		getSqlCallStack().push(_$sqlje_rs_gen1);
		getSqlCallStack().push(_$sqlje_ps_gen4);
		for (Map m : new ResultSetIterator<Map>(_$sqlje_rs_gen1, Map.class)) {
		}
		return 1;
	}

	protected aurora.sqlje.core.IInstanceManager _$sqlje_instanceManager = null;
	protected aurora.sqlje.core.ISqlCallStack _$sqlje_sqlCallStack = null;
	protected SqlFlag $sql = new SqlFlag();

	public aurora.sqlje.core.ISqlCallStack getSqlCallStack() {
		return _$sqlje_sqlCallStack;
	}

	public void _$setInstanceManager(aurora.sqlje.core.IInstanceManager args0) {
		_$sqlje_instanceManager = args0;
		$sql.setInstanceManager(_$sqlje_instanceManager);
	}

	public aurora.sqlje.core.IInstanceManager getInstanceManager() {
		return _$sqlje_instanceManager;
	}

	public void _$setSqlCallStack(aurora.sqlje.core.ISqlCallStack args0) {
		_$sqlje_sqlCallStack = args0;
		$sql.setSqlCallStack(_$sqlje_sqlCallStack);
	}
}
