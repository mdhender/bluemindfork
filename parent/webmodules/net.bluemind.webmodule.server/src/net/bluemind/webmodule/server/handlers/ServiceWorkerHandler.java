package net.bluemind.webmodule.server.handlers;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.common.freemarker.EquinoxTemplateLoader;
import net.bluemind.core.api.BMVersion;
import net.bluemind.webmodule.server.WebModule;

public class ServiceWorkerHandler implements IWebModuleConsumer, Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(ServiceWorkerHandler.class);
	private WebModule module;
	private Template mainTemplate;

	public ServiceWorkerHandler() {
		Configuration freemarkerCfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		freemarkerCfg.setTemplateLoader(new EquinoxTemplateLoader(this.getClass().getClassLoader(), "/templates/"));
		freemarkerCfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
		try {
			mainTemplate = freemarkerCfg.getTemplate("sw.ftl.js");
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void handle(HttpServerRequest request) {
		Map<String, Object> model = new HashMap<>();
		model.put("version", BMVersion.getVersion());
		model.put("files", getAssetsList());
		model.put("scope", module.root);

		Writer sw = new StringWriter();
		try {
			mainTemplate.process(model, sw);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		HttpServerResponse resp = request.response();
		byte[] data = sw.toString().getBytes();
		resp.putHeader("Content-Length", "" + data.length);
		resp.putHeader(HttpHeaders.CONTENT_TYPE, "application/javascript");
		resp.write(Buffer.buffer(data));
		resp.setStatusCode(200);
		resp.end();
	}

	private List<String> getAssetsList() {
		List<String> files = new ArrayList<>();
		files.add("style/customstyle.css");
		files.add("index.html");
		files.addAll(module.resources.stream().flatMap(resource -> resource.getResources().stream())
				.filter(ServiceWorkerHandler::assetsFilter).collect(Collectors.toSet()));
		return files;
	}

	private static Boolean assetsFilter(String path) {

		return !(path.startsWith(".") || path.endsWith(".devmode.js")|| path.endsWith(".nocache.js") || path.startsWith("WEB-INF"));
	}

	@Override
	public void setModule(WebModule module) {
		this.module = module;
	}

}
