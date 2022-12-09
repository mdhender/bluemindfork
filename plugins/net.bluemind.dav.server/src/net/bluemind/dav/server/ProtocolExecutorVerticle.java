package net.bluemind.dav.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.dav.server.proto.IDavProtocol;
import net.bluemind.dav.server.proto.IProtocolFactory;
import net.bluemind.dav.server.proto.MethodMessage;
import net.bluemind.dav.server.proto.ProtocolFactory;
import net.bluemind.dav.server.proto.delete.DeleteProtocol;
import net.bluemind.dav.server.proto.get.GetIcsProtocol;
import net.bluemind.dav.server.proto.get.GetVcfProtocol;
import net.bluemind.dav.server.proto.mkcalendar.MkCalendarProtocol;
import net.bluemind.dav.server.proto.move.MoveProtocol;
import net.bluemind.dav.server.proto.options.OptionsProtocol;
import net.bluemind.dav.server.proto.post.BookMultiputProtocol;
import net.bluemind.dav.server.proto.post.FreeBusyProtocol;
import net.bluemind.dav.server.proto.post.PushProtocol;
import net.bluemind.dav.server.proto.post.VEventStuffPostProtocol;
import net.bluemind.dav.server.proto.propfind.PropFindProtocol;
import net.bluemind.dav.server.proto.proppatch.PropPatchProtocol;
import net.bluemind.dav.server.proto.put.PutProtocol;
import net.bluemind.dav.server.proto.report.ReportProtocol;
import net.bluemind.dav.server.store.LoggedCore;

public final class ProtocolExecutorVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(ProtocolExecutorVerticle.class);

	public void start() {
		registerProtocol(vertx.eventBus(), new DeleteProtocol());
		registerProtocol(vertx.eventBus(), new GetIcsProtocol());
		registerProtocol(vertx.eventBus(), new GetVcfProtocol());
		registerProtocol(vertx.eventBus(), new MkCalendarProtocol());
		registerProtocol(vertx.eventBus(), new OptionsProtocol());
		registerProtocol(vertx.eventBus(), new BookMultiputProtocol());
		registerProtocol(vertx.eventBus(), new FreeBusyProtocol());
		registerProtocol(vertx.eventBus(), new PushProtocol());
		registerProtocol(vertx.eventBus(), new VEventStuffPostProtocol());
		registerProtocol(vertx.eventBus(), new PropFindProtocol());
		registerProtocol(vertx.eventBus(), new PropPatchProtocol());
		registerProtocol(vertx.eventBus(), new PutProtocol());
		registerProtocol(vertx.eventBus(), new ReportProtocol());
		registerProtocol(vertx.eventBus(), new MoveProtocol());
	}

	private <Q, R> void registerProtocol(EventBus eb, IDavProtocol<Q, R> proto) {
		final IProtocolFactory<Q, R> factory = new ProtocolFactory<>(proto);
		logger.debug("Registered {} on bus for proto.", factory.getExecutorAddress());
		eb.consumer(factory.getExecutorAddress(), (final Message<JsonObject> event) -> {
			try {
				@SuppressWarnings("unchecked")
				LocalJsonObject<MethodMessage<Q>> withSid = (LocalJsonObject<MethodMessage<Q>>) event.body();
				LoggedCore lc = withSid.getValue().lc;
				logger.debug("Got core: {}", lc);
				Q query = withSid.getValue().query;
				logger.debug("Decoded payload as {}", query);
				factory.getProtocol().execute(lc, query, (R response) -> {
					try {
						event.reply(new LocalJsonObject<>(response));
					} catch (Exception t) {
						logger.error(t.getMessage(), t);
					}
				});
			} catch (Exception t) {
				logger.error(t.getMessage(), t);
				event.reply(new LocalJsonObject<>(t));
			}

		});
	}
}
