package aurora.sqlje.core;

import uncertain.ocm.IObjectCreator;

public class InstanceManager implements IInstanceManager {

	private IObjectCreator ioc;

	public InstanceManager(IObjectCreator ioc) {
		super();
		this.ioc = ioc;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ISqlCallEnabled> T createInstance(
			Class<? extends ISqlCallEnabled> clazz) {
		T proc;
		try {
			proc = (T) ioc.createInstance(clazz);
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

}
