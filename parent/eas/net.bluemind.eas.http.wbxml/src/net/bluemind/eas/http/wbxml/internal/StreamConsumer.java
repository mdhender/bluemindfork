package net.bluemind.eas.http.wbxml.internal;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;
import com.google.common.io.CountingOutputStream;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.vertx.common.request.Requests;

public class StreamConsumer implements Handler<Buffer> {

	private static final Logger logger = LoggerFactory.getLogger(StreamConsumer.class);

	private final FileBackedOutputStream fbos;
	private final CountingOutputStream withStats;
	private final AuthorizedDeviceQuery query;
	public boolean corrupted = false;

	public StreamConsumer(AuthorizedDeviceQuery query) {
		fbos = new FileBackedOutputStream(32768, "stream-consumer");
		withStats = new CountingOutputStream(fbos);
		this.query = query;
	}

	public ByteSource inputStream() throws IOException {
		return fbos.asByteSource();
	}

	public boolean isEmptyRequestBody() {
		return withStats.getCount() == 0;
	}

	@Override
	public void handle(Buffer event) {
		if (!corrupted) {
			byte[] data = event.getBytes();
			try {
				withStats.write(data, 0, data.length);
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
				corrupted = true;
			}
		}
	}

	public void markEnd() {
		try {
			withStats.close();
		} catch (IOException e) {
		}
		Requests.tag(query.request(), "in.size", withStats.getCount() + "b");
	}

	public void dispose() {
		try {
			fbos.reset();
		} catch (IOException e) {
		}
	}

}