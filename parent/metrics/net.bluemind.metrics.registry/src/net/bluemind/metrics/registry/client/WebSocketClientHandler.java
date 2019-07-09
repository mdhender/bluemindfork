package net.bluemind.metrics.registry.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;

public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
	private static final Logger logger = LoggerFactory.getLogger(WebSocketClientHandler.class);

	private final WebSocketClientHandshaker handshaker;
	private ChannelPromise handshakeFuture;

	public WebSocketClientHandler(final WebSocketClientHandshaker handshaker) {
		this.handshaker = handshaker;
	}

	public ChannelFuture handshakeFuture() {
		return handshakeFuture;
	}

	@Override
	public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {
		handshakeFuture = ctx.newPromise();
	}

	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		handshaker.handshake(ctx.channel());
	}

	@Override
	public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
		logger.info("Inactive channel, disconnection");
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		final Channel ch = ctx.channel();
		if (!handshaker.isHandshakeComplete()) {
			// web socket client connected
			handshaker.finishHandshake(ch, (FullHttpResponse) msg);
			handshakeFuture.setSuccess();
			return;
		}

		if (msg instanceof FullHttpResponse) {
			final FullHttpResponse response = (FullHttpResponse) msg;
			throw new Exception("Unexpected FullHttpResponse (getStatus=" + response.status() + ", content="
					+ response.content().toString(CharsetUtil.UTF_8) + ')');
		}

		final WebSocketFrame frame = (WebSocketFrame) msg;
		if (frame instanceof CloseWebSocketFrame) {
			logger.info("Closing websocket");
			ch.close();
		}

	}

	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
		cause.printStackTrace();

		if (!handshakeFuture.isDone()) {
			handshakeFuture.setFailure(cause);
		}
		ctx.close();
	}
}
