package aurora.sqlje.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;

import javax.sql.DataSource;

import uncertain.ocm.IObjectCreator;
import uncertain.ocm.IObjectRegistry;
import aurora.database.service.IDatabaseServiceFactory;

public class InstanceManager implements IInstanceManager {

	private IObjectRegistry ior;
	IDatabaseServiceFactory dsf;

	private HashMap<String, ProcClassEntry> classCache = new HashMap<String, ProcClassEntry>(
			100);
	private ClassLoader currentClassLoader = getClass().getClassLoader();

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

	@Override
	public <T extends ISqlCallEnabled> T createInstance(String name) {
		try {
			Class<? extends ISqlCallEnabled> clazz = loadProcClass(name);
			return createInstance(clazz);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public <T extends ISqlCallEnabled> T createInstance(String name,
			ISqlCallEnabled caller) {
		try {
			Class<? extends ISqlCallEnabled> clazz = loadProcClass(name);
			return createInstance(clazz, caller);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	private Class<? extends ISqlCallEnabled> loadProcClass(String name)
			throws ClassNotFoundException {
		return (Class<? extends ISqlCallEnabled>) new SQLJEClassLoader(
				currentClassLoader).findClass(name);
	}

	private ISqlCallStack createCallStack() throws SQLException {
		DataSource ds = dsf.getDataSource();
		Connection initConnection = ds.getConnection();
		initConnection.setAutoCommit(false);
		SqlCallStack callStack = new SqlCallStack(ds, initConnection);
		return callStack;
	}

	static class ProcClassEntry {
		long lastUpdate;
		Class clazz;
	}

	class SQLJEClassLoader extends ClassLoader {
		public SQLJEClassLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {

			try {
				String path = name.replace('.', '/') + ".class";
				URL url = getResource(path);
				if(url == null)
					throw new ClassNotFoundException(name);
				if ("file".equals(url.getProtocol())) {
					File file = new File(url.getFile());
					if (!file.exists())
						throw new ClassNotFoundException(name
								+ " is not exists");
					ProcClassEntry e = classCache.get(name);
					if (e == null) {
						e = new ProcClassEntry();
						e.clazz = defineClassFromFile(name, file);
						e.lastUpdate = file.lastModified();
						classCache.put(name, e);
						System.out.println("initial load class " + name);
						return e.clazz;
					}
					if (e.lastUpdate >= file.lastModified()) {
						System.out.println("get class " + name + " from cache");
						return e.clazz;
					}
					e.clazz = defineClassFromFile(name, file);
					e.lastUpdate = file.lastModified();
					System.out.println("update class define :" + name);
					return e.clazz;
				}
				InputStream input = url.openStream();
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				byte[] b = new byte[8 * 1024];
				for (int len = -1; (len = input.read(b)) != -1;)
					buffer.write(b, 0, len);

				byte[] classData = buffer.toByteArray();

				return defineClass(name, classData, 0, classData.length);

			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			throw new ClassNotFoundException(name);
		}

		private Class<?> defineClassFromFile(String name, File f)
				throws ClassNotFoundException {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(
					(int) f.length());
			byte[] bf = new byte[8 * 1024];
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(f);
				for (int len = -1; (len = fis.read(bf)) != -1;)
					bos.write(bf, 0, len);
			} catch (FileNotFoundException e) {
				throw new ClassNotFoundException(name + " is not found", e);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			byte[] bytes = bos.toByteArray();
			return defineClass(name, bytes, 0, bytes.length);
		}

	}

}
