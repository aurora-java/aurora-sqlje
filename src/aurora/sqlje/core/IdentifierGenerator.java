package aurora.sqlje.core;

public class IdentifierGenerator {
	private static final String common_prefix = "_$sqlje_";
	private int seq = 0;

	private IdentifierGenerator() {
	}

	public static IdentifierGenerator newInstance() {
		return new IdentifierGenerator();
	}

	public String gen(String namePart) {
		return common_prefix + namePart + "_gen" + (seq++);
	}
}
