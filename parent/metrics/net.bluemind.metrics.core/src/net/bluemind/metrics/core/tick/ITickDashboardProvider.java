package net.bluemind.metrics.core.tick;

import java.io.InputStream;

import org.vertx.java.core.json.JsonObject;

import com.google.common.io.ByteStreams;

import net.bluemind.core.api.fault.ServerFault;

public interface ITickDashboardProvider {

	String name();

	InputStream content();

	default JsonObject jsonContent() {
		try (InputStream in = content()) {
			String s = new String(ByteStreams.toByteArray(in));
			JsonObject obj = new JsonObject(s);
			obj.putString("name", name());
			return obj;
		} catch (Exception e) {
			throw new ServerFault("Invalid dashboard content");
		}
	}
}
