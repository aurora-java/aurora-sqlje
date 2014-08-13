package aurora.sqlje.exception;

import aurora.sqlje.core.ISqlCallEnabled;

public class MethodNotDeclaredException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4598557608994286835L;
	private ISqlCallEnabled proc;
	private String methodName;

	public MethodNotDeclaredException(ISqlCallEnabled proc, String methodName) {
		super("no public method :" + methodName + " declared in proc :"
				+ proc.getClass().getName());
		this.proc = proc;
		this.methodName = methodName;
	}

	public ISqlCallEnabled getProcedure() {
		return proc;
	}

	public String getMethod() {
		return methodName;
	}

}
