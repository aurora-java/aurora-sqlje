package aurora.sqlje.exception;

public class UserDefinedException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6310162646994786115L;

	public UserDefinedException(String message) {
		super(message);
	}

	public static UserDefinedException create(String message) {
		return new UserDefinedException(message);
	}
}
