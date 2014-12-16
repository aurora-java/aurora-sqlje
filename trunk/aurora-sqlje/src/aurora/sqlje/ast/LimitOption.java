package aurora.sqlje.ast;

import org.eclipse.jdt.core.dom.Expression;

/**
 * INTERNAL USE ONLY<br>
 * this class is not intend for user use.
 * 
 * @author jessen
 *
 */
public class LimitOption {
	public Expression start;
	public Expression end;

	public LimitOption(Expression start, Expression end) {
		this.start = start;
		this.end = end;
	}
}
