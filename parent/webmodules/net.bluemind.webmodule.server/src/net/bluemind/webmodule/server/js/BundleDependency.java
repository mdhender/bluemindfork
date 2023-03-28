package net.bluemind.webmodule.server.js;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BundleDependency implements JsDependency {

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BundleDependency other = (BundleDependency) obj;
		return Objects.equals(name, other.name);
	}

	private final String name;

	public BundleDependency(String name) {
		this.name = name;
	}

	@Override
	public String getValue() {
		return name;
	}

	@Override
	public String toString() {
		return getValue();
	}

	@Override
	public List<JsEntry> getEntries(JsDependency dependency, List<JsEntry> entries) {
		return entries.stream().filter(entry -> name.equals(entry.getBundle())).collect(Collectors.toList());
	}

}