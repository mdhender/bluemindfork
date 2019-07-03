package net.bluemind.metrics.registry.client;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;

@Sharable
public class HttpClientHandler extends SimpleChannelInboundHandler<HttpObject> {
	private static final Logger logger = LoggerFactory.getLogger(HttpClientHandler.class);
	private CompletableFuture<String> future = null;

	@Override
	public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
		if (msg instanceof HttpContent) {
			ByteBuf content = ((HttpContent) msg).content();
			if (future != null && !future.isDone()) {
				future.complete(content.toString(StandardCharsets.UTF_8));
				future = null;
			}
		} else if (!(msg instanceof HttpResponse)) {
			logger.info("Received non HttpResponse, non HtppContent data.");
		}
	}

	public void setPromise(CompletableFuture<String> future) {
		this.future = future;
	}
}
