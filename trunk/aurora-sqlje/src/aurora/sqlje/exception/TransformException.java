package aurora.sqlje.exception;

import org.eclipse.jdt.core.compiler.IProblem;

public class TransformException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1464027994023351919L;
	public IProblem[] problems;

	public TransformException(IProblem[] problems) {
		this.problems = problems;
	}
}
