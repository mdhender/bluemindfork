package net.bluemind.eas.http.wbxml.internal;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.w3c.dom.Document;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.eas.config.global.GlobalConfig;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.utils.DOMDumper;
import net.bluemind.eas.validation.Validator;
import net.bluemind.eas.wbxml.WBXMLTools;
import net.bluemind.vertx.common.request.Requests;

public class WbxmlRequestComplete implements Handler<Void> {
	private static final Logger logger = LoggerFactory.getLogger(WbxmlRequestComplete.class);

	private StreamConsumer consumer;
	private AuthorizedDeviceQuery event;
	private WbxmlHandlerBase wbxmlHandlerBase;

	public WbxmlRequestComplete(WbxmlHandlerBase wbxmlHandlerBase, StreamConsumer consumer,
			AuthorizedDeviceQuery event) {
		this.wbxmlHandlerBase = wbxmlHandlerBase;
		this.consumer = consumer;
		this.event = event;
	}

	@Override
	public void handle(Void v) {
		MDC.put("user", event.loginAtDomain().replace("@", "_at_"));
		consumer.markEnd();
		if (consumer.corrupted) {
			consumer.dispose();
			badRequest();
		} else if (consumer.isEmptyRequestBody()) {
			// empty Ping or Sync command means something
			wbxmlHandlerBase.handle(event, null);
		} else {
			try (InputStream in = consumer.inputStream().openBufferedStream()) {
				Document document = WBXMLTools.toXml(in);
				boolean valid = Validator.check(event.request(), event.protocolVersion(), document);
				if (!valid || GlobalConfig.DATA_IN_LOGS) {
					DOMDumper.dumpXml(logger, "rid: " + Requests.tag(event.request(), "rid")
							+ (valid ? ", Valid document" : ", INVALID document") + " from pda:\n", document);
				}
				if (valid) {
					// Validator.check will provide a bad request response if
					// the request is invalid
					wbxmlHandlerBase.handle(event, document);
				}
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
				badRequest();
			} finally {
				consumer.dispose();
			}
		}
		MDC.put("user", "anonymous");
	}

	private void badRequest() {
		HttpServerResponse resp = event.request().response();
		resp.setStatusCode(400).setStatusMessage("WBXML error").end();
		MDC.put("user", "anonymous");
	}

}