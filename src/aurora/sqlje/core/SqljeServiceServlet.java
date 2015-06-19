package aurora.sqlje.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.json.JSONException;
import org.json.JSONObject;

import uncertain.composite.CompositeMap;
import uncertain.composite.DynamicObject;
import uncertain.composite.JSONAdaptor;
import uncertain.core.UncertainEngine;
import uncertain.ocm.IObjectRegistry;
import uncertain.proc.IProcedureManager;
import uncertain.proc.IProcedureRegistry;
import uncertain.proc.Procedure;
import uncertain.proc.trace.StackTraceManager;
import uncertain.proc.trace.TraceElement;
import aurora.database.service.IDatabaseServiceFactory;
import aurora.service.IService;
import aurora.service.ServiceContext;
import aurora.service.ServiceThreadLocal;
import aurora.service.exception.IExceptionDescriptor;
import aurora.service.http.HttpServiceFactory;
import aurora.service.http.HttpServiceInstance;
import aurora.service.http.WebContextInit;
import aurora.service.json.JSONServiceContext;
import aurora.service.json.JSONServiceInterpreter;

public class SqljeServiceServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2165135565181841193L;
	// public static final String POST_SERVICE = "post-service";
	public static final String PRE_SERVICE = "pre-service";
	protected UncertainEngine mUncertainEngine;
	protected IProcedureManager mProcManager;
	protected ServletConfig mConfig;
	protected ServletContext mContext;
	private HttpServiceFactory mServiceFactory;
	private IInstanceManager instMgr;
	private IProcedureRegistry mProcRegistry;

	// Procedure mPreServiceProc;
	// Procedure mPostServiceProc;

	public static String getServiceName(HttpServletRequest request) {
		String service_name = request.getServletPath();
		if (service_name.charAt(0) == '/')
			service_name = service_name.substring(1);
		return service_name;
	}

	protected HttpServiceInstance createServiceInstance(
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		final String name = getServiceName(request);
		final HttpServiceInstance svc = mServiceFactory.createHttpService(name,
				request, response, this);
		return svc;
	}

	protected void handleException(HttpServletRequest request,
			HttpServletResponse response, Throwable ex) throws IOException,
			ServletException {

		ex.printStackTrace();
	}

	protected void cleanUp(IService service) {
	}

	/**
	 * By default, set no-cache directive to client. Sub class can override this
	 * method to provide customized cache control.
	 */
	protected void writeCacheDirection(HttpServletResponse response,
			IService service) {
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Expires", "0");
	}

	// public static Procedure getProcedureToRun(IProcedureManager procManager,
	// IService service) throws Exception {
	// String procedure_name = null;
	// IEventDispatcher config = service.getConfig();
	// config.fireEvent(E_DetectProcedure.EVENT_NAME, new Object[] { service });
	// ServiceController controller = ServiceController
	// .createServiceController(service.getServiceContext()
	// .getObjectContext());
	// if (!controller.getContinueFlag())
	// return null;
	// procedure_name = controller.getProcedureName();
	// Procedure proc = procManager.loadProcedure(procedure_name);
	// return proc;
	// }

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doService(request, response);
	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doService(request, response);
	}

	protected void doService(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		// check if UncertainEngine is inited properly
		if (!mUncertainEngine.isRunning()) {
			StringBuilder msg = new StringBuilder(
					"Application failed to initialize");
			Throwable thr = mUncertainEngine.getInitializeException();
			if (thr != null)
				msg.append(":").append(thr.getMessage());
			response.sendError(500, msg.toString());
			return;
		}

		// begin service
		StackTraceManager stm = new StackTraceManager();
		request.setCharacterEncoding("UTF-8");// form post encoding
		HttpServiceInstance svc = null;
		ServiceContext ctx = null;
		boolean is_success = true;
		SqlCallStack callStack = null;

		try {
			svc = createServiceInstance(request, response);
			ctx = svc.getServiceContext();
			ctx.setStackTraceManager(stm);
			ServiceThreadLocal.setCurrentThreadContext(ctx.getObjectContext());
			ServiceThreadLocal.setSource(request.getRequestURI());
			// populateService(request, response, svc);
			writeCacheDirection(response, svc);

			// TODO
			IDatabaseServiceFactory dsf = (IDatabaseServiceFactory) getObjectRegistry()
					.getInstanceOfType(IDatabaseServiceFactory.class);
			DataSource ds = dsf.getDataSource();
			Connection conn = ds.getConnection();
			conn.setAutoCommit(false);
			callStack = new SqlCallStack(ds, conn);
			CompositeMap contextMap = ctx.getObjectContext();
			callStack.setContextData(contextMap);
			String pathInfo = request.getPathInfo();
			String[] segs = pathInfo.split("\\/");
			String className = segs[1];
			String methodName = segs[2];
			String batch = null;
			if (segs.length > 3)
				batch = segs[3];

			svc.setName(className + "#" + methodName);
			JSONServiceContext jsonCtx = (JSONServiceContext) DynamicObject
					.cast(contextMap, JSONServiceContext.class);
			new JSONServiceInterpreter().preParseParameter(jsonCtx);
			CompositeMap param = contextMap.getChild("parameter");

			// //pre-service

			Procedure pre_service_proc = null;

			if (mProcRegistry != null) {
				pre_service_proc = mProcRegistry.getProcedure(PRE_SERVICE);
			}

			if (pre_service_proc != null)
				is_success = svc.invoke(pre_service_proc);

			// /end pre-service

			if (is_success) {
				ISqlCallEnabled proc = instMgr.createInstance(className);
				if (proc != null) {
					proc._$setSqlCallStack(callStack);
					Method method = proc.getClass().getMethod(methodName,
							CompositeMap.class);
					if (method != null) {
						if ("batch".equals(batch)) {
							List<CompositeMap> childs = param.getChilds();
							if (childs != null) {
								for (CompositeMap m : childs) {
									method.invoke(proc, m);
								}
							}
						} else
							method.invoke(proc, param);
					}
				} else {
					throw new IllegalArgumentException(className
							+ " is not a valid SQLJE procedure.");
				}

				Object output = contextMap.get("output");
				if (output == null)
					output = param;
				if (output instanceof CharSequence) {
					response.setContentType(JSONServiceInterpreter.DEFAULT_JSON_CONTENT_TYPE);
					response.getWriter().write(output.toString());
				} else if (output instanceof CompositeMap) {
					JSONObject json = new JSONObject();
					// Write success flag
					json.put("success", is_success);
					JSONObject o = JSONAdaptor
							.toJSONObject((CompositeMap) output);
					json.put("result", o);
					response.setContentType(JSONServiceInterpreter.DEFAULT_JSON_CONTENT_TYPE);
					response.getWriter().write(json.toString());
				} else if (output instanceof InputStream) {
					transfer((InputStream) output, response.getOutputStream());
				} else if (output instanceof Reader) {
					Reader reader = (Reader) output;
					transfer(reader, response.getWriter());
				} else if (output instanceof File) {
					File file = (File) output;
					response.setHeader("Content-disposition",
							"attachment; filename=" + file.getName());
					response.setDateHeader("Expires", 0);
					response.setContentType("application/x-msdownload");
					transfer(new FileInputStream(file),
							response.getOutputStream());
				} else {
					throw new IllegalArgumentException(
							"Target for SQLJE output is illegal." + output);
				}
			}
		} catch (Throwable ex) {
			is_success = false;
			/*
			 * if(ctx.getException()==null) ctx.setException(ex);
			 */
			mUncertainEngine.logException("Error when executing service "
					+ request.getRequestURI(), ex);
			while (ex.getCause() != null) {
				ex = ex.getCause();
			}
			ctx.setException(ex);
			IExceptionDescriptor ed = (IExceptionDescriptor) getObjectRegistry()
					.getInstanceOfType(IExceptionDescriptor.class);
			CompositeMap error = ed.process(ctx, ex);
			ctx.setError(error);
			handleException(request, response, ex);
			try {
				JSONObject json = new JSONObject();
				// Write success flag
				json.put("success", false);
				JSONObject o = JSONAdaptor.toJSONObject(error);
				json.put("error", o);
				response.setContentType(JSONServiceInterpreter.DEFAULT_JSON_CONTENT_TYPE);
				response.getWriter().write(json.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} finally {

			if (is_success) {
				try {
					// TODO commit
					if (callStack != null) {
						callStack.commit();
						callStack.cleanUp();
					}
				} catch (Throwable e) {
					mUncertainEngine.logException("Error when commit service "
							+ request.getRequestURI(), e);
				}
			} else {
				try {
					// TODO rollback
					if (callStack != null) {
						callStack.rollback();
						callStack.cleanUp();
					}
				} catch (Throwable e) {
					mUncertainEngine.logException(
							"Error when rollback service "
									+ request.getRequestURI(), e);
				}
			}

			// release resource
			svc.release();

			// set overall finish time
			TraceElement elm = stm.getRootNode();
			if (elm != null)
				elm.setExitTime(System.currentTimeMillis());

			ServiceThreadLocal.remove();
			cleanUp(svc);

		}
	}

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		mConfig = config;
		mContext = config.getServletContext();
		mUncertainEngine = WebContextInit.getUncertainEngine(mContext);
		if (mUncertainEngine == null)
			throw new ServletException("Uncertain engine not initialized");
		mServiceFactory = (HttpServiceFactory) mUncertainEngine
				.getObjectRegistry()
				.getInstanceOfType(HttpServiceFactory.class);
		mProcRegistry = (IProcedureRegistry) mUncertainEngine
				.getObjectRegistry()
				.getInstanceOfType(IProcedureRegistry.class);
		if (mServiceFactory == null)
			throw new ServletException(
					"No ServiceFactory instance registered in UncertainEngine");
		instMgr = (IInstanceManager) getObjectRegistry().getInstanceOfType(
				IInstanceManager.class);
		if (instMgr == null)
			throw new ServletException(
					"No InstanceManager(SQLJE) registered in UncertainEngine");
	}

	public UncertainEngine getUncertainEngine() {
		return mUncertainEngine;
	}

	public IObjectRegistry getObjectRegistry() {
		return mUncertainEngine == null ? null : mUncertainEngine
				.getObjectRegistry();
	}

	// //-----

	public static void transfer(InputStream is, OutputStream os)
			throws IOException {
		byte[] b = new byte[64 * 1024];
		int len = -1;
		try {
			while ((len = is.read(b)) != -1) {
				os.write(b, 0, len);
			}
		} finally {
			try {
				is.close();
			} finally {
				os.close();
			}
		}
	}

	public static void transfer(Reader reader, Writer writer)
			throws IOException {
		char[] c = new char[8 * 1024];
		int len = -1;
		try {
			while ((len = reader.read(c)) != -1) {
				writer.write(c, 0, len);
			}
		} finally {
			try {
				reader.close();
			} finally {
				writer.flush();
				writer.close();
			}
		}
	}
}
