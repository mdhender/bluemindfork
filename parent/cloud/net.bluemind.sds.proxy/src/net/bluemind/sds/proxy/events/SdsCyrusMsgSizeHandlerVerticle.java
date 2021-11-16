package net.bluemind.sds.proxy.events;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.Message;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.system.api.SysConfKeys;

public class SdsCyrusMsgSizeHandlerVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(SdsCyrusMsgSizeHandlerVerticle.class);

	public static class SdsSizeCheckFactory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new SdsCyrusMsgSizeHandlerVerticle();
		}
	}

	@Override
	public void start() {

		AtomicReference<SharedMap<String, String>> ref = new AtomicReference<>();
		MQ.init().thenAccept(v -> ref.set(MQ.sharedMap(Shared.MAP_SYSCONF)));
		AtomicLong configuredSize = new AtomicLong(DefaultValues.MAX_SIZE);

		vertx.setPeriodic(20000, tid -> {
			SharedMap<String, String> map = ref.get();
			if (map != null) {
				String sizeLimit = map.get(SysConfKeys.message_size_limit.name());
				Optional.ofNullable(sizeLimit).map(Long::parseLong).ifPresent(newSize -> {
					long oldSize = configuredSize.get();
					if (newSize != oldSize) {
						logger.info("Max message size changed from {} to {}", oldSize, newSize);
						configuredSize.set(newSize);
					}
				});
			} else {
				logger.warn("Sysconf shared map not available for size check, using max={}", configuredSize.get());
			}
		});

		vertx.eventBus().consumer(SdsAddresses.SIZE_VALIDATION, (Message<Long> message) -> {
			long reqSize = message.body().longValue();
			long maxSize = configuredSize.get();
			message.reply(reqSize > 0 && reqSize <= maxSize);
		});
	}

}
