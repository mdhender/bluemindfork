package net.bluemind.webmodule.server.js;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class JsDependencyRegistry {

	private static final JsDependencyRegistry instance = new JsDependencyRegistry();

	public static JsDependencyRegistry getInstance() {
		return instance;
	}

	private ConcurrentHashMap<JsEntry, Set<JsDependency>> registry;

	private JsDependencyRegistry() {
		registry = new ConcurrentHashMap<>();
	}

	public void add(JsEntry js, JsDependency dependency) {
		Set<JsDependency> dependencies = registry.getOrDefault(js, new HashSet<>());
		dependencies.add(dependency);
		registry.putIfAbsent(js, dependencies);
	}

	public Set<JsDependency> get(JsEntry js) {
		return registry.getOrDefault(js, Collections.emptySet());
	}
}
