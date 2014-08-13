package aurora.sqlje.exception;

public class SqljeInitException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3819727849223556579L;
	Throwable rootCause;

	public SqljeInitException(String msg, Exception e) {
		super(msg);
		if (e != null)
			this.rootCause = getRootCause0(e);
	}
	
	public SqljeInitException(Exception e) {
		this(e.getMessage(),e);
	}

	public Throwable getRootCause() {
		return rootCause;
	}

	private Throwable getRootCause0(Throwable thr) {
		while (thr.getCause() != null)
			thr = thr.getCause();
		return thr;
	}

}
