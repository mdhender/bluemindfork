package net.bluemind.sds.proxy.events;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

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

public class SdsCyrusValidationHandlerVerticle extends Verticle {
	private static final Logger logger = LoggerFactory.getLogger(SdsCyrusValidationHandlerVerticle.class);
	private static final int CORE_EXCEPTION = 0;
	static ICyrusValidationPromise cli;

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
		cli = getProvider();

		vertx.eventBus().registerHandler(SdsAddresses.VALIDATION, (Message<Buffer> message) -> {
			JsonObject json = JsonHelper.getJsonFromString(message.body().toString());

			if (JsonHelper.isValidJson(json, "mailbox", "partition")) {
				String mailbox = json.getString("mailbox");
				String partition = json.getString("partition");

				cli.prevalidate(mailbox, partition).thenAccept((Boolean result) -> {
					logger.info("BM Core {} creation of {}/{}", result ? "approves" : "rejects", partition, mailbox);
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

	private ICyrusValidationPromise getProvider() {
		ILocator cachingLocator = (String service, AsyncHandler<String[]> asyncHandler) -> {
			Optional<IServiceTopology> topology = Topology.getIfAvailable();
			if (topology.isPresent()) {
				String core = topology.get().core().value.address();
				String[] resp = new String[] { core };
				asyncHandler.success(resp);
			} else {
				asyncHandler.failure(new TopologyException("topology not available"));
			}
		};
		HttpClientProvider clientProvider = new HttpClientProvider(vertx);
		VertxPromiseServiceProvider provider = new VertxPromiseServiceProvider(clientProvider, cachingLocator,
				Token.admin0());
		return provider.instance("bm/core", ICyrusValidationPromise.class);
	}
}
