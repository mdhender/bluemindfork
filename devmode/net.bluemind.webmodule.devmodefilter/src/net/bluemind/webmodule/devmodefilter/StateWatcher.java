package net.bluemind.webmodule.devmodefilter;

import java.util.function.Function;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import net.bluemind.core.utils.JsonUtils;

public class StateWatcher {

	private final Vertx vertx;
	public DevModeState state;
	private Function<DevModeState, Void> changeListener;

	public StateWatcher(Vertx vertx, Function<DevModeState, Void> changeListener) {
		this.vertx = vertx;
		this.changeListener = changeListener;
	}

	public void start() {
		Handler<Message<String>> basic = (msg) -> updateState(msg.body());
		Handler<AsyncResult<Message<String>>> canFail = msg -> {
			if (msg.succeeded()) {
				updateState(msg.result().body());
			}
		};

		vertx.eventBus().consumer("devmode.state", basic);
		vertx.eventBus().request("devmode.state:get", true, canFail);
	}

	protected void updateState(String body) {
		state = JsonUtils.read(body, DevModeState.class);
		changeListener.apply(state);
	}

	public void stop() {

	}
}
