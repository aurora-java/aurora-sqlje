package aurora.sqlje.core;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import uncertain.ocm.IObjectCreator;
import uncertain.ocm.IObjectRegistry;
import aurora.database.service.IDatabaseServiceFactory;

public class InstanceManager implements IInstanceManager {

	private IObjectRegistry ior;
	IDatabaseServiceFactory dsf;

	public InstanceManager(IObjectRegistry ior, IDatabaseServiceFactory dsf) {
		super();
		this.ior = ior;
		this.dsf = dsf;
	}

	public InstanceManager(IObjectRegistry ior) {
		this.ior = ior;
	}

	public IObjectRegistry getObjectRegistry() {
		return ior;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ISqlCallEnabled> T createInstance(
			Class<? extends ISqlCallEnabled> clazz) {
		T proc;
		try {
			proc = (T) ((IObjectCreator) ior).createInstance(clazz);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		if (proc != null)
			proc._$setInstanceManager(this);
		return proc;
	}

	@Override
	public <T extends ISqlCallEnabled> T createInstance(
			Class<? extends ISqlCallEnabled> clazz, ISqlCallEnabled caller) {
		T proc = createInstance(clazz);
		if (proc != null)
			proc._$setSqlCallStack(caller.getSqlCallStack());
		return proc;
	}

	public <T extends ISqlCallEnabled> T createInstanceForTransaction(
			Class<? extends ISqlCallEnabled> clazz) throws SQLException {
		T inst = createInstance(clazz);
		ISqlCallStack stack = createCallStack();
		inst._$setSqlCallStack(stack);
		return inst;
	}

	private ISqlCallStack createCallStack() throws SQLException {
		DataSource ds = dsf.getDataSource();
		Connection initConnection = ds.getConnection();
		initConnection.setAutoCommit(false);
		SqlCallStack callStack = new SqlCallStack(ds, initConnection);
		return callStack;
	}
}
