package net.bluemind.metrics.registry.client;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.unix.DomainSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.shaded.org.jctools.queues.MpscUnboundedArrayQueue;
import net.bluemind.metrics.registry.impl.Mapper;
import net.bluemind.metrics.registry.json.RegJson;

public class WebSocketClient {

	private final URI uri;
	private Channel ch;
	private static final Logger logger = LoggerFactory.getLogger(WebSocketClient.class);

	private final ScheduledExecutorService flushExecutor;
	private final MpscUnboundedArrayQueue<RegJson> metricsQueue;

	public WebSocketClient() throws URISyntaxException {
		uri = new URI("ws://metrics.socket");
		flushExecutor = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory("metrics-flush", true));
		metricsQueue = new MpscUnboundedArrayQueue<>(4096);
	}

	public void open() throws FileNotFoundException, InterruptedException {
		final WebSocketClientHandler handler = new WebSocketClientHandler(
				WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, null, 1280000));

		ChannelInitializer<DomainSocketChannel> chanInit = new ChannelInitializer<DomainSocketChannel>() {
			@Override
			public void initChannel(DomainSocketChannel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();
				pipeline.addLast("http-codec", new HttpClientCodec());
				pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
				pipeline.addLast("ws-handler", handler);
			}
		};
		Bootstrap bootstrap = ClientBootstrap.create(chanInit);
		ch = bootstrap.connect().sync().channel();
		ChannelFuture handshaked = handler.handshakeFuture().sync();
		logger.info("Websocket created {}", handshaked);
		flushExecutor.scheduleAtFixedRate(this::flushImpl, 0, 1, TimeUnit.SECONDS);
	}

	private void flushImpl() {
		if (metricsQueue.isEmpty()) {
			return;
		}
		try {
			LongAdder count = new LongAdder();
			metricsQueue.drain(dto -> {
				try {
					ByteBuf asBuffer = Unpooled.wrappedBuffer(Mapper.get().writeValueAsBytes(dto));
					ch.write(new TextWebSocketFrame(asBuffer));
					count.increment();
				} catch (JsonProcessingException jpe) {
					logger.error(jpe.getMessage(), jpe);
				}
			});
			ch.flush();
			long flushed = count.sum();
			if (flushed > 2048) {
				logger.info("Flushed {} metrics to agent.", flushed);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	public void queue(RegJson dto) {
		boolean accepted = metricsQueue.offer(dto);
		if (!accepted) {
			// should not occur as the queue is unbounded
			logger.warn("Metric {} was rejected by queue", dto);
		}
		if (metricsQueue.size() > 2048) {
			flushExecutor.submit(this::flushImpl);
		}
	}
}
