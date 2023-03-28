package net.bluemind.webmodule.server.js;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ScriptDependency implements JsDependency {

    private final String path;

	public ScriptDependency(String path) {
		this.path = path;
	}

	@Override
	public String getValue() {
		return path;
	}

	@Override
	public String toString() {
		return getValue();
	}

	@Override
	public List<JsEntry> getEntries(JsDependency dependency, List<JsEntry> entries) {
		Optional<JsEntry> entry = entries.stream().filter(js -> path.equals(js.path)).findFirst();
		if (entry.isPresent()) {
			return Collections.singletonList(entry.get());
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(path);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ScriptDependency other = (ScriptDependency) obj;
		return Objects.equals(path, other.path);
	}

}