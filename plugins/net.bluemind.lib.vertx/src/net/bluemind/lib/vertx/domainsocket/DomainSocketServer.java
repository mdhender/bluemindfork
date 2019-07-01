/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.lib.vertx.domainsocket;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.impl.Closeable;
import org.vertx.java.core.impl.DefaultContext;
import org.vertx.java.core.impl.DefaultFutureResult;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.core.net.impl.DefaultNetSocket;
import org.vertx.java.core.net.impl.ServerID;
import org.vertx.java.core.net.impl.TCPSSLHelper;
import org.vertx.java.core.net.impl.VertxNetHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoop;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.ChannelGroupFutureListener;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.UnixChannel;
import io.netty.util.concurrent.GlobalEventExecutor;

public class DomainSocketServer implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(DomainSocketServer.class);

	private static Map<ServerID, DomainSocketServer> sharedDomainServers = new HashMap<>();
	private VertxInternal vertx;
	private DefaultContext actualCtx;

	private DefaultChannelGroup serverChannelGroup;
	private final Workers<DomainSocketServer> workers = new Workers<>();

	private final Map<Channel, DefaultNetSocket> socketMap = new ConcurrentHashMap<Channel, DefaultNetSocket>();

	private final TCPSSLHelper tcpHelper;
	private Handler<NetSocket> connectHandler;

	private boolean listening;

	private DomainSocketServer actualServer;
	private String path;

	private ChannelFuture bindFuture;
	private volatile ServerID id;

	private EpollEventLoopGroup epoolGroup;

	public DomainSocketServer(VertxInternal vertx) {
		this.vertx = vertx;
		tcpHelper = null;
		actualCtx = vertx.getOrCreateContext();
		actualCtx.addCloseHook(this);
	}

	public DomainSocketServer connectHandler(Handler<NetSocket> connectHandler) {
		this.connectHandler = connectHandler;
		return this;
	}

	public DomainSocketServer listen(final String path, final Handler<AsyncResult<DomainSocketServer>> listenHandler) {
		this.path = path;
		if (connectHandler == null) {
			throw new IllegalStateException("Set connect handler first");
		}
		if (listening) {
			throw new IllegalStateException("Listen already called");
		}
		listening = true;
		id = new ServerID(0, path);

		synchronized (sharedDomainServers) {

			DomainSocketServer shared = (DomainSocketServer) sharedDomainServers.get(id);

			if (shared == null) {
				workers.register(this);
				serverChannelGroup = new DefaultChannelGroup("vertx-acceptor-channels", GlobalEventExecutor.INSTANCE);
				new File(path).delete();
				log.info("bind socket to {} ", path);
				// Wildcard port will imply a new actual server each time

				ServerBootstrap bootstrap = new ServerBootstrap();
				epoolGroup = new EpollEventLoopGroup(1);
				bootstrap.group(epoolGroup);
				bootstrap.channel(EpollServerDomainSocketChannel.class);
				bootstrap.childHandler(new ChannelInitializer<UnixChannel>() {

					@Override
					protected void initChannel(UnixChannel ch) throws Exception {
						ch.pipeline().addLast(new ServerHandler());
					}
				});

				try {
					log.debug("bind socket to {} ", path);

					bindFuture = bootstrap.bind(new DomainSocketAddress(path)).addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							runListeners();
						}
					});
					this.addListener(new Runnable() {
						@Override
						public void run() {
							if (bindFuture.isSuccess()) {

								try {
									java.nio.file.Files.setPosixFilePermissions(Paths.get(path),
											// rwxr-x---
											// rw-rw-rw-
											PosixFilePermissions.fromString("rw-rw-rw-"));
								} catch (IOException e) {
								}

								log.info("unix server listening on " + DomainSocketServer.this.path + ":"
										+ bindFuture.channel().localAddress());
								// Update port to actual port - wildcard port 0
								// might have been used
								sharedDomainServers.put(new ServerID(0, path), DomainSocketServer.this);
							} else {
								sharedDomainServers.remove(new ServerID(0, path));
							}
						}
					});
					serverChannelGroup.add(bindFuture.channel());
				} catch (final Exception t) {
					log.error("bind socket to {} ", path, t);

					// Make sure we send the exception back through the handler
					// (if
					// any)
					if (listenHandler != null) {
						vertx.runOnContext(new VoidHandler() {
							@Override
							protected void handle() {
								listenHandler.handle(new DefaultFutureResult<DomainSocketServer>(t));
							}
						});
					} else {
						// No handler - log so user can see failure
						actualCtx.reportException(t);
					}
					listening = false;
					return this;
				}
				sharedDomainServers.put(new ServerID(0, path), this);

				actualServer = this;
			} else { // Server already exists with that host/port - we will use
						// that
				actualServer = shared;
				actualServer.workers.register(this);
			}
			// just add it to the future so it gets notified once the bind is
			// complete

			// bind event
			actualServer.addListener(new Runnable() {

				public void run() {
					if (listenHandler != null) {
						final AsyncResult<DomainSocketServer> res;
						if (actualServer.bindFuture.isSuccess()) {
							res = new DefaultFutureResult<DomainSocketServer>(DomainSocketServer.this);
						} else {
							listening = false;
							res = new DefaultFutureResult<DomainSocketServer>(actualServer.bindFuture.cause());
						}
						actualCtx.execute(new Runnable() {
							@Override
							public void run() {
								listenHandler.handle(res);
							}
						});
					} else if (!actualServer.bindFuture.isSuccess()) {
						// No handler - log so user can see failure
						actualCtx.reportException(actualServer.bindFuture.cause());
						listening = false;
					}
				}
			});

		}
		return this;

	}

	private class ServerHandler extends VertxNetHandler {
		public ServerHandler() {
			super(DomainSocketServer.this.vertx, socketMap);
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			final Channel ch = ctx.channel();
			EventLoop worker = ch.eventLoop();
			// choose event thread
			DomainSocketServer server = workers.next();

			HandlerHolder<NetSocket> holder = new HandlerHolder<>(server.actualCtx, worker, server.connectHandler);
			doConnected(ch, holder);
		}

		private void doConnected(Channel ch, HandlerHolder<NetSocket> handler) {
			DefaultNetSocket sock = new DefaultNetSocket(vertx, ch, handler.context, tcpHelper, false);
			socketMap.put(ch, sock);
			handler.context.runOnContext((v) -> {
				handler.handler.handle(sock);
			});
		}

	}

	private Queue<Runnable> bindListeners = new ConcurrentLinkedQueue<>();

	private boolean listenersRun;

	private synchronized void addListener(Runnable runner) {
		if (!listenersRun) {
			bindListeners.add(runner);
		} else {
			// Run it now
			runner.run();
		}
	}

	private synchronized void runListeners() {
		Runnable runner;
		while ((runner = bindListeners.poll()) != null) {
			runner.run();
		}
		listenersRun = true;
	}

	public void close() {
		close(null);
	}

	@Override
	public void close(final Handler<AsyncResult<Void>> done) {
		if (!listening) {
			if (done != null) {
				executeCloseDone(actualCtx, done, null);
			}
			return;
		}
		listening = false;
		synchronized (sharedDomainServers) {
			if (actualServer != null) {
				actualServer.actualClose(actualCtx, done);
			}
		}
		actualCtx.removeCloseHook(this);
	}

	private void actualClose(final DefaultContext closeContext, final Handler<AsyncResult<Void>> done) {
		if (id != null) {
			sharedDomainServers.remove(id);
		}

		for (DefaultNetSocket sock : socketMap.values()) {
			sock.close();
		}

		// We need to reset it since sock.internalClose() above can call into
		// the close handlers of sockets on the same thread
		// which can cause context id for the thread to change!

		vertx.setContext(closeContext);

		ChannelGroupFuture fut = serverChannelGroup.close();
		fut.addListener(new ChannelGroupFutureListener() {
			public void operationComplete(ChannelGroupFuture fut) throws Exception {
				executeCloseDone(closeContext, done, fut.cause());
			}
		});

	}

	private void executeCloseDone(final DefaultContext closeContext, final Handler<AsyncResult<Void>> done,
			final Exception e) {
		if (done != null) {
			closeContext.execute(new Runnable() {
				public void run() {
					done.handle(new DefaultFutureResult<Void>(e));
				}
			});
		}
	}
}
