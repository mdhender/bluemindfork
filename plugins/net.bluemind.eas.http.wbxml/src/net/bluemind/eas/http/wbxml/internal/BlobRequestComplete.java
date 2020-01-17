package net.bluemind.eas.http.wbxml.internal;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.wbxml.BlobHandlerBase;

public class BlobRequestComplete implements Handler<Void> {
	private static final Logger logger = LoggerFactory.getLogger(BlobRequestComplete.class);

	private StreamConsumer consumer;
	private AuthorizedDeviceQuery event;
	private BlobHandlerBase blobHandlerBase;

	public BlobRequestComplete(BlobHandlerBase wbxmlHandlerBase, StreamConsumer consumer, AuthorizedDeviceQuery event) {
		this.blobHandlerBase = wbxmlHandlerBase;
		this.consumer = consumer;
		this.event = event;
	}

	@Override
	public void handle(Void v) {
		consumer.markEnd();
		if (consumer.corrupted) {
			consumer.dispose();
			badRequest();
		} else {
			try {
				ByteSource in = consumer.inputStream();
				blobHandlerBase.handle(event, in, new Handler<Void>() {

					@Override
					public void handle(Void event) {
						logger.info("Disposing temporary mail storage.");
						consumer.dispose();
					}
				});
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
				badRequest();
			}
		}
	}

	private void badRequest() {
		HttpServerResponse resp = event.request().response();
		resp.setStatusCode(400).setStatusMessage("request error").end();
	}

}