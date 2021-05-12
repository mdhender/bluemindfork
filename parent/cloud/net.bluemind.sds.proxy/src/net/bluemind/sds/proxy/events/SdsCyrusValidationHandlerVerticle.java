package net.bluemind.sds.proxy.events;

import java.io.File;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.mail.replica.api.ICyrusValidationPromise;
import net.bluemind.config.Token;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxPromiseServiceProvider;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.topology.Topology;
import net.bluemind.network.topology.TopologyException;

public class SdsCyrusValidationHandlerVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(SdsCyrusValidationHandlerVerticle.class);
	private static final int CORE_EXCEPTION = 0;
	private HttpClientProvider clientProvider;
	private ILocator cachingLocator;

	public static class SdsCoreAPIFactory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return false;
		}

		@Override
		public Verticle newInstance() {
			return new SdsCyrusValidationHandlerVerticle();
		}
	}

	@Override
	public void start() {
		this.clientProvider = new HttpClientProvider(vertx);
		this.cachingLocator = (String service, AsyncHandler<String[]> asyncHandler) -> {
			Optional<IServiceTopology> topology = Topology.getIfAvailable();
			if (topology.isPresent()) {
				String core = topology.get().core().value.address();
				String[] resp = new String[] { core };
				asyncHandler.success(resp);
			} else {
				asyncHandler.failure(new TopologyException("topology not available"));
			}
		};

		vertx.eventBus().consumer(SdsAddresses.VALIDATION, (Message<Buffer> message) -> {
			JsonObject json = JsonHelper.getJsonFromString(message.body().toString());
			if (JsonHelper.isValidJson(json, "mailbox", "partition", "mboxpath")) {
				String mailbox = json.getString("mailbox");
				String partition = json.getString("partition");
				String mboxpath = json.getString("mboxpath");
				ICyrusValidationPromise cyrusValidationApi = getValidationApi(vertx);
				cyrusValidationApi.prevalidate(mailbox, partition).thenAccept((Boolean result) -> {
					logger.info("BM Core {} creation of {}/{}", Boolean.TRUE.equals(result) ? "approves" : "rejects",
							partition, mailbox);
					if (Boolean.TRUE.equals(result) && mboxpath != null) {
						// Ensure the cyrus data folder exists for the creation of future messages
						// downloaded from S3
						File fsMbox = new File(mboxpath);
						if (!fsMbox.exists()) {
							logger.info("create missing cyrus folder {}", fsMbox.getPath());
							fsMbox.mkdirs();
						}
					}
					message.reply(result);
				}).exceptionally(ex -> {
					logger.error("Unable to get approval of {}/{}: {}", partition, mailbox, ex.getMessage());
					message.fail(CORE_EXCEPTION, ex.getMessage());
					return null;
				});
			} else {
				logger.error("Invalid payload: {}", json);
				message.reply(false);
			}
		});
	}

	private ICyrusValidationPromise getValidationApi(Vertx vertx) {

		VertxPromiseServiceProvider provider = new VertxPromiseServiceProvider(clientProvider, cachingLocator,
				Token.admin0());
		return provider.instance("bm/core", ICyrusValidationPromise.class);
	}
}
