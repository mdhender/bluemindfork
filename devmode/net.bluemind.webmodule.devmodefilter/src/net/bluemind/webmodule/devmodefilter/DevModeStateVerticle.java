package net.bluemind.webmodule.devmodefilter;

import java.io.File;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Verticle;

import com.google.common.io.Files;

import net.bluemind.core.utils.JsonUtils;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;

public class DevModeStateVerticle extends Verticle {

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new DevModeStateVerticle();
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(DevModeStateVerticle.class);
	private static final String defaultConfPath = "/etc/bm/dev.json";
	private DevModeState state;

	@Override
	public void start() {
		state = new DevModeState();

		loadStateFromFile();
		vertx.eventBus().registerHandler("devmode.state:get", (msg) -> msg.reply(JsonUtils.asString(state)));
		vertx.eventBus().registerHandler("devmode.state:put", (Message<String> msg) -> handlePut(msg));
		vertx.eventBus().registerHandler("devmode.state:reload", (msg) -> {
			loadStateFromFile();
			msg.reply(true);
		});

		notifyChange();
	}

	private void loadStateFromFile() {
		File devMode;
		File defaultDevMode = new File(defaultConfPath);
		File homeDirDevMode = new File(System.getProperty("user.home") + "/dev.json");

		logger.warn("/etc/bm/dev.json");
		if (defaultDevMode.exists()) {
			devMode = defaultDevMode;
		} else if (homeDirDevMode.exists()) {
			devMode = homeDirDevMode;
		} else {
			return;
		}

		try {
			String newState = Files.toString(devMode, Charset.forName("utf-8"));
			DevModeState t = JsonUtils.read(newState, DevModeState.class);
			t.filters = t.filters.stream().filter(f -> f.active).collect(Collectors.toList());
			t.forwardPorts = t.forwardPorts.stream().filter(f -> f.active).collect(Collectors.toList());
			state = t;
		} catch (Exception e) {
			logger.warn("error reading /etc/bm/dev.json", e);
			return;
		}

		logger.info("new state \n {}", JsonUtils.asString(state));
		notifyChange();
	}

	private Object handlePut(Message<String> msg) {
		try {
			state = JsonUtils.read(msg.body(), DevModeState.class);
		} catch (Exception e) {
			msg.reply(false);
			return null;
		}

		msg.reply(true);
		notifyChange();
		return null;
	}

	private void notifyChange() {
		logger.info("state changed");
		vertx.eventBus().publish("devmode.state", JsonUtils.asString(state));
	}

}
