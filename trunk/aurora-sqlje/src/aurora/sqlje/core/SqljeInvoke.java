package aurora.sqlje.core;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import uncertain.composite.CompositeMap;
import uncertain.composite.TextParser;
import uncertain.exception.BuiltinExceptionFactory;
import uncertain.ocm.IObjectRegistry;
import uncertain.proc.AbstractEntry;
import uncertain.proc.ProcedureRunner;
import aurora.database.service.IDatabaseServiceFactory;
import aurora.database.service.SqlServiceContext;
import aurora.sqlje.exception.MethodNotDeclaredException;
import aurora.sqlje.exception.SqljeInitException;

public class SqljeInvoke extends AbstractEntry {
	private IInstanceManager instManager;
	private IDatabaseServiceFactory dsf;
	private String procName = "";
	private String method = "";
	private IObjectRegistry reg;

	public SqljeInvoke(IInstanceManager instManager,
			IDatabaseServiceFactory dsf, IObjectRegistry ior) {
		super();
		this.instManager = instManager;
		this.dsf = dsf;
		this.reg = ior;
	}

	@Override
	public void run(ProcedureRunner runner) throws Exception {
		CompositeMap context = runner.getContext();
		SqlServiceContext sqlServiceContext = SqlServiceContext
				.createSqlServiceContext(context);
		sqlServiceContext.initConnection(reg, null);
		ISqlCallStack sqlCallStack = new SqlCallStack(dsf.getDataSource(),
				sqlServiceContext.getConnection());
		sqlCallStack.setContextData(context);
		if (procName == null)
			throw BuiltinExceptionFactory.createAttributeMissing(this,
					"procName");
		procName = TextParser.parse(procName, context);
		if (method == null)
			throw BuiltinExceptionFactory
					.createAttributeMissing(this, "method");
		method = TextParser.parse(method, context);
		try {
			Class clazz = Class.forName(procName);
			if (ISqlCallEnabled.class.isAssignableFrom(clazz)) {
				ISqlCallEnabled proc = instManager.createInstance(clazz);
				if (proc == null)
					throw new SqljeInitException("Can't create SQLJE proc : "
							+ clazz, new NullPointerException());
				proc._$setSqlCallStack(sqlCallStack);
				Method m = getMethod(proc);
				m.invoke(proc, context.getChild("parameter"));
			} else {
				throw new Exception("Illegal SQLJE proc : " + procName);
			}
			sqlCallStack.commit();
		} catch (ClassNotFoundException e) {
			sqlCallStack.rollback();
			throw new SqljeInitException(procName + " not exists", e);
		} catch (Exception e) {
			sqlCallStack.rollback();
			e.printStackTrace();
			throw e;
		} finally {
			sqlCallStack.cleanUp();
		}
	}

	private Method getMethod(ISqlCallEnabled proc) throws Exception {
		for (Method m : proc.getClass().getMethods()) {
			if ((m.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC
					&& m.getName().equals(method)) {
				Class[] paramTypes = m.getParameterTypes();
				if (paramTypes.length == 1
						&& paramTypes[0].isAssignableFrom(CompositeMap.class))
					return m;
			}
		}

		throw new MethodNotDeclaredException(proc, method);
	}

	public void setProc(String proc) {
		this.procName = proc;
	}

	public String getProc() {
		return procName;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

}
