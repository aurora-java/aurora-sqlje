package aurora.sqlje.parser;

public class CharStack {
	private StringBuilder sb = new StringBuilder(20);

	public char push(char c) {
		sb.append(c);
		return c;
	}

	public char peek() {
		return sb.charAt(sb.length() - 1);
	}

	public char pop() {
		char c = sb.charAt(sb.length() - 1);
		sb.deleteCharAt(sb.length() - 1);
		return c;
	}

	public boolean isEmpty() {
		return sb.length() == 0;
	}

	public void clear() {
		sb.delete(0, sb.length());
	}

	public String toString() {
		return sb.toString();
	}
}
