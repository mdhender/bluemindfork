package net.bluemind.eas.validation;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerRequest;
import org.w3c.dom.Document;

import net.bluemind.eas.config.global.GlobalConfig;
import net.bluemind.eas.utils.RunnableExtensionLoader;
import net.bluemind.vertx.common.request.Requests;

public final class Validator {
	private static final Logger logger = LoggerFactory.getLogger(Validator.class);

	private static IProtocolValidator validator;

	static {
		RunnableExtensionLoader<IProtocolValidator> rel = new RunnableExtensionLoader<>();
		List<IProtocolValidator> validators = rel.loadExtensions("net.bluemind.eas.validation", "protocol", "validator",
				"impl");
		if (validators.isEmpty()) {
			validator = new IProtocolValidator() {

				@Override
				public void checkResponse(double pv, Document doc) throws ValidationException {
				}

				@Override
				public void checkRequest(double pv, Document doc) throws ValidationException {
				}
			};
		} else {
			validator = validators.get(0);
		}
		logger.info("Using validator {}", validator);
	}

	public static IProtocolValidator get() {
		return validator;
	}

	/**
	 * Returns true if the request is valid. When the request is invalid, this
	 * method will terminate the request and the call should stop processing the
	 * request.
	 * 
	 * @param httpServerRequest
	 * @param doc
	 * @return
	 */
	public static boolean check(HttpServerRequest httpServerRequest, double protocolVersion, Document doc) {
		try {
			validator.checkRequest(protocolVersion, doc);
		} catch (ValidationException ve) {
			String m = "Request is an INVALID AS request: " + ve.getMessage();
			if (GlobalConfig.FAIL_ON_INVALID_REQUESTS) {
				httpServerRequest.response().setStatusCode(400).setStatusMessage(m).end();
				return false;
			} else {
				Requests.tag(httpServerRequest, "request", "invalid");
				logger.warn(m);
			}
		}
		return true;

	}

}
