package aurora.sqlje.core;

import uncertain.core.ILifeCycle;
import uncertain.ocm.IObjectCreator;
import uncertain.ocm.IObjectRegistry;

public class SqljeConfig implements ILifeCycle {

	private IObjectRegistry objectRegistry;

	public SqljeConfig(IObjectRegistry ior) {
		super();
		this.objectRegistry = ior;
	}

	@Override
	public boolean startup() {
		InstanceManager instManager;
		try {
			instManager = (InstanceManager) ((IObjectCreator) objectRegistry)
					.createInstance(InstanceManager.class);
			objectRegistry.registerInstance(instManager);
			System.out.println("SQLJE InstanceManager startup success");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public void shutdown() {

	}

}
