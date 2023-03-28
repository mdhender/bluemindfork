package net.bluemind.webmodules.webapp.webfilters.legacy;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LegacyAppRedirectionLoader {
	private static final Logger logger = LoggerFactory.getLogger(LegacyAppRedirectionLoader.class);

	private Map<String, String> webmoduleIndexes;

	public Map<String, String> load() {
		loadWebmodulesIndexes();
		return forEachExtensions("net.bluemind.webapp", "application").filter(element -> isOldApplication(element))
				.collect(Collectors.toMap(element -> getOldApplicationIndex(element),
						element -> element.getAttribute("href")));
	}

	private boolean isOldApplication(IConfigurationElement element) {
		IConfigurationElement[] children = element.getChildren("embed");
		if (children.length > 0) {
			String src = children[0].getAttribute("src");
			String base = src.substring(0, src.lastIndexOf("/"));
			if (webmoduleIndexes.containsKey(base)) {
				logger.info("External app {} with base {} is {} legacy app", src, base,
						base + "/" + webmoduleIndexes.get(base));
				return !src.equals(base + "/" + webmoduleIndexes.get(base));
			}
			logger.info("External app {} is not a legacy App");
		}
		return false;
	}

	private String getOldApplicationIndex(IConfigurationElement element) {
		String src = element.getChildren("embed")[0].getAttribute("src");
		String base = src.substring(0, src.lastIndexOf("/"));
		return base + "/" + webmoduleIndexes.get(base);
	}

	private void loadWebmodulesIndexes() {
		webmoduleIndexes = forEachExtensions("net.bluemind.webmodule", "web-module")
				.collect(Collectors.toMap(element -> element.getAttribute("root"),
						element -> Optional.ofNullable(element.getAttribute("index")).orElse("index.html")));
	}

	private Stream<IConfigurationElement> forEachExtensions(String pointName, String elementName) {
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(pointName);
		if (point == null) {
			logger.error("point {} not found.", pointName);
			return Stream.empty();
		}
		return Arrays.stream(point.getExtensions())
				.flatMap(extension -> Arrays.stream(extension.getConfigurationElements()))
				.filter(element -> element.getName().equals(elementName));
	}

}
