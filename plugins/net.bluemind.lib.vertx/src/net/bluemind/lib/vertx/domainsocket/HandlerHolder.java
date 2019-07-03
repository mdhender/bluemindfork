/*
 * Copyright (c) 2011-2013 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package net.bluemind.lib.vertx.domainsocket;

import org.vertx.java.core.Handler;
import org.vertx.java.core.impl.DefaultContext;

import io.netty.channel.EventLoop;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class HandlerHolder<T> {
	public final Handler<T> handler;
	public final EventLoop worker;
	public final DefaultContext context;

	HandlerHolder(DefaultContext context, EventLoop worker, Handler<T> handler) {
		this.context = context;
		this.worker = worker;
		this.handler = handler;
	}

}
