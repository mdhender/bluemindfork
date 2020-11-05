package net.bluemind.metrics.registry.tests;

import java.io.FileNotFoundException;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.unix.DomainSocketChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpVersion;

public class HttpClient {
	private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);
	private Channel ch;
	private Bootstrap bootstrap;
	private final HttpClientHandler handler = new HttpClientHandler();

	public void open() throws FileNotFoundException, InterruptedException {

		ChannelInitializer<DomainSocketChannel> chanInit = new ChannelInitializer<DomainSocketChannel>() {
			@Override
			public void initChannel(DomainSocketChannel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();
				pipeline.addLast(new HttpRequestEncoder());
				pipeline.addLast(new HttpResponseDecoder());
				// To avoid getting data in chunks while testing.
				// WARNING : As you add tests to BMRegistry, you might get a truncated JSON
				// TODO handle truncated data correctly so getMetrics can always send one JSON
				pipeline.addLast(new HttpObjectAggregator(65536));
				pipeline.addLast("handler", handler);
			}
		};
		bootstrap = ClientBootstrap.create(chanInit);
		ch = bootstrap.connect().sync().channel();
	}

	public void close() throws InterruptedException {
		logger.info("Closing http client");
		ch.flush();
		ch.closeFuture().sync();
	}

	public CompletableFuture<String> getMetrics() throws InterruptedException {
		CompletableFuture<String> future = new CompletableFuture<>();

		ch = bootstrap.connect().sync().channel();
		handler.setPromise(future);
		HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/metrics");
		ch.writeAndFlush(request);
		return future;
	}
}
