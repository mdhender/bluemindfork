package net.bluemind.metrics.registry.client;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;

import net.bluemind.metrics.registry.impl.Mapper;
import net.bluemind.metrics.registry.json.RegJson;

public class AgentPushClient {

	private static final Logger logger = LoggerFactory.getLogger(AgentPushClient.class);

	private final ArrayBlockingQueue<byte[]> queue;
	private final ObjectWriter writer;

	public AgentPushClient(ArrayBlockingQueue<byte[]> sharedQueue) {
		this.queue = sharedQueue;
		this.writer = Mapper.get().writerFor(RegJson.class);
	}

	public void queue(RegJson dto) {
		try {
			byte[] chunk = writer.writeValueAsBytes(dto);
			boolean accepted = queue.offer(chunk, 1, TimeUnit.SECONDS);
			while (!accepted) {
				logger.warn("Blocking push to agent queue for {} byte(s)", chunk.length);
				accepted = queue.offer(chunk, 1, TimeUnit.SECONDS);
			}
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

	}
}
