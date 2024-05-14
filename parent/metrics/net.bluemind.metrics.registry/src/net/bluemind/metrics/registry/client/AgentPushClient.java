package net.bluemind.metrics.registry.client;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import net.bluemind.metrics.registry.impl.Mapper;
import net.bluemind.metrics.registry.json.RegJson;

public class AgentPushClient {

	private ArrayBlockingQueue<byte[]> queue;
	private static final Logger logger = LoggerFactory.getLogger(AgentPushClient.class);

	public AgentPushClient(ArrayBlockingQueue<byte[]> sharedQueue) {
		this.queue = sharedQueue;
	}

	public void queue(RegJson dto) {
		try {
			byte[] chunk = Mapper.get().writeValueAsBytes(dto);
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
