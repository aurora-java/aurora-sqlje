package aurora.sqlje.core;

import uncertain.composite.CompositeMap;
import uncertain.ocm.OCManager;

public class InstanceManager implements IInstanceManager {

	private OCManager ocManager;

	public InstanceManager(OCManager ocm) {
		super();
		this.ocManager = ocm;
	}

	@Override
	public <T extends ISqlCallEnabled> T createInstance(
			Class<? extends ISqlCallEnabled> clazz) {
		CompositeMap container = new CompositeMap();
		String eleName = clazz.getSimpleName();
		container.setName(eleName);
		String pkgName = null;
		if (clazz.getPackage() != null)
			pkgName = clazz.getPackage().getName();
		container.setNameSpaceURI(pkgName);
		ocManager.getClassRegistry().registerClass(eleName, pkgName, eleName);
		@SuppressWarnings("unchecked")
		T proc = (T) ocManager.createNewInstance(container);
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
