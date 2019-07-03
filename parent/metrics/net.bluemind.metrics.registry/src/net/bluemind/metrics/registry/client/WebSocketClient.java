package net.bluemind.metrics.registry.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.unix.DomainSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

public class WebSocketClient {

	private final URI uri;
	private Channel ch;
	private static final Logger logger = LoggerFactory.getLogger(WebSocketClient.class);

	public WebSocketClient() throws URISyntaxException {
		uri = new URI("ws://metrics.socket");
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
		handler.handshakeFuture().sync();
		logger.info("Websocket created");
	}

	public void close() throws InterruptedException {
		ch.writeAndFlush(new CloseWebSocketFrame());
		ch.closeFuture().sync();
	}

	public void sendTextFrame(final String text) throws IOException {
		ch.writeAndFlush(new TextWebSocketFrame(text));
	}
}
