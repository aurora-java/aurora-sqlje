package aurora.sqlje.exception;

public class TooManyColumnsException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3903904413987717787L;
	public TooManyColumnsException() {
		super("TOO_MANY_COLUMNS");
	}

}
