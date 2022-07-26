package net.bluemind.lib.elasticsearch;

public class RewritableIndex {

	private final String prefix;
	private final String readAlias;
	private final String writeAlias;

	private RewritableIndex(String prefix) {
		this.prefix = prefix;
		this.readAlias = prefix + "_read_alias";
		this.writeAlias = prefix + "_write_alias";
	}

	public String prefix() {
		return prefix;
	}

	public String readAlias() {
		return readAlias;
	}

	public String writeAlias() {
		return writeAlias;
	}

	public String newName() {
		return String.format("%s_%d", prefix, System.currentTimeMillis() / 1000);
	}

	static RewritableIndex fromPrefix(String prefix) {
		return new RewritableIndex(prefix);
	}

}
