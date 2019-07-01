package net.bluemind.webmodule.devmodefilter;

import java.util.function.Function;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;

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
		Handler<Message<String>> reponseHander = (msg) -> updateState(msg.body());
		vertx.eventBus().registerHandler("devmode.state", reponseHander);

		vertx.eventBus().send("devmode.state:get", true, reponseHander);
	}

	protected void updateState(String body) {
		state = JsonUtils.read(body, DevModeState.class);
		changeListener.apply(state);
	}

	public void stop() {

	}
}
