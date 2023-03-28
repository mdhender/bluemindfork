package net.bluemind.webmodule.server.js;

import java.util.List;

public interface JsDependency {
	String getValue();

	List<JsEntry> getEntries(JsDependency dependency, List<JsEntry> js);

}