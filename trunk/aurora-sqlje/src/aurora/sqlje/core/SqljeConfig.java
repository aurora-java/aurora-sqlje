package aurora.sqlje.core;

import uncertain.core.ILifeCycle;
import uncertain.ocm.IObjectCreator;
import uncertain.ocm.IObjectRegistry;
import uncertain.ocm.OCManager;

public class SqljeConfig implements ILifeCycle {

	private IObjectCreator ocManager;
	private IObjectRegistry objectRegistry;

	public SqljeConfig(IObjectCreator ocm, IObjectRegistry ior) {
		super();
		this.ocManager = ocm;
		this.objectRegistry = ior;
	}

	@Override
	public boolean startup() {
		InstanceManager instManager = new InstanceManager(ocManager);
		objectRegistry.registerInstance(instManager);
		System.out.println("SQLJE InstanceManager startup success");
		return true;
	}

	@Override
	public void shutdown() {

	}

}
