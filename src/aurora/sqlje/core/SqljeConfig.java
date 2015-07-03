package aurora.sqlje.core;

import uncertain.composite.CompositeMap;
import uncertain.core.ILifeCycle;
import uncertain.ocm.AbstractLocatableObject;
import uncertain.ocm.IObjectCreator;
import uncertain.ocm.IObjectRegistry;
import aurora.service.exception.ExceptionDescriptorConfig;
import aurora.service.exception.IExceptionDescriptor;
import aurora.sqlje.core.database.StdField;
import aurora.sqlje.core.reflect.BeanAnalyzer;
import aurora.sqlje.exception.NoDataFoundException;
import aurora.sqlje.exception.TooManyColumnsException;
import aurora.sqlje.exception.TooManyRowsException;
import aurora.sqlje.exception.UserDefinedException;
import aurora.sqlje.exception.UserDefinedExceptionDescriptor;

public class SqljeConfig extends AbstractLocatableObject implements ILifeCycle {

	private IObjectRegistry objectRegistry;

	private StdField[] stdFields;

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

			ExceptionDescriptorConfig edc = (ExceptionDescriptorConfig) objectRegistry
					.getInstanceOfType(IExceptionDescriptor.class);
			if (edc != null) {
				edc.addExceptionDescriptor(createExceptionHandleItem(
						UserDefinedException.class,
						UserDefinedExceptionDescriptor.class));
				edc.addExceptionDescriptor(createExceptionHandleItem(
						NoDataFoundException.class,
						UserDefinedExceptionDescriptor.class));
				edc.addExceptionDescriptor(createExceptionHandleItem(
						TooManyColumnsException.class,
						UserDefinedExceptionDescriptor.class));
				edc.addExceptionDescriptor(createExceptionHandleItem(
						TooManyRowsException.class,
						UserDefinedExceptionDescriptor.class));
			}

			System.out.println("SQLJE InstanceManager startup success");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void setStdfields(StdField[] stdfs) {
		BeanAnalyzer.instance().setStdfields(stdfs);
		this.stdFields = stdfs;
	}

	public StdField[] getStdfields() {
		return stdFields;
	}

	private static CompositeMap createExceptionHandleItem(Class<?> expClass,
			Class<?> handleClass) {
		String className = handleClass.getSimpleName();
		CompositeMap item = new CompositeMap(className);
		item.put("exception", expClass.getName());
		item.put("handleclass", handleClass.getName());
		item.put("xmlns", handleClass.getPackage().getName());
		return item;
	}

	@Override
	public void shutdown() {

	}

}
