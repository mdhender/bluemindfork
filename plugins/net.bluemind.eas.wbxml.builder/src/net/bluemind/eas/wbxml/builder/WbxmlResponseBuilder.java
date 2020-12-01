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
package net.bluemind.eas.wbxml.builder;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import net.bluemind.eas.config.global.GlobalConfig;
import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.base.DisposableByteSource;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.utils.DOMDumper;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.validation.ValidationException;
import net.bluemind.eas.validation.Validator;
import net.bluemind.eas.wbxml.WbxmlOutput;
import net.bluemind.eas.wbxml.builder.vertx.ByteSourceEventProducer;
import net.bluemind.eas.wbxml.builder.vertx.Chunk;
import net.bluemind.eas.wbxml.writers.WbxmlEncoder;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.vertx.common.LocalJsonObject;

public class WbxmlResponseBuilder implements IResponseBuilder {

	private static final Logger logger = LoggerFactory.getLogger(WbxmlResponseBuilder.class);

	private final WbxmlOutput output;
	private final Deque<Element> containerNamesStack; // for "debugging" purpose
	private final String loginForSifting;
	private Document debugDom;
	private NamespaceMapping currentNS;
	private WbxmlEncoder encoder;

	public WbxmlResponseBuilder(String loginForSifting, WbxmlOutput output) {
		this.output = output;
		this.containerNamesStack = new ArrayDeque<>();
		this.loginForSifting = loginForSifting != null ? loginForSifting.replace("@", "_at_") : "anonymous";
	}

	@Override
	public IResponseBuilder start(NamespaceMapping ns) {
		encoder = new WbxmlEncoder(ns.namespace(), output);
		try {
			encoder.header();
			this.debugDom = DOMUtils.createDoc(ns.namespace(), ns.root());
			containerNamesStack.push(debugDom.getDocumentElement());
			container(ns, ns.root());
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return this;
	}

	@Override
	public IResponseBuilder container(NamespaceMapping ns, String name) {
		nsSetup(ns, name);
		try {
			encoder.writeElement(name);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return this;
	}

	private void nsSetup(NamespaceMapping ns, String name) {
		if (currentNS != ns) {
			try {
				encoder.switchNamespace(ns.namespace());
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
		currentNS = ns;
		if (!name.equals(containerNamesStack.peek().getLocalName())) {
			containerNamesStack.push(DOMUtils.createElement(containerNamesStack.peek(), ns.namespace() + ":" + name));
		}
	}

	@Override
	public IResponseBuilder container(String name) {
		return container(currentNS, name);
	}

	@Override
	public IResponseBuilder token(NamespaceMapping ns, String name) {
		nsSetup(ns, name);
		try {
			encoder.writeEmptyElement(name);
			containerNamesStack.pop();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return this;
	}

	@Override
	public IResponseBuilder token(String name) {
		return token(currentNS, name);
	}

	@Override
	public IResponseBuilder text(NamespaceMapping ns, String name, String value) {
		container(ns, name);
		try {
			containerNamesStack.peek().setTextContent(value);
			encoder.writeStrI(value);
			endContainer();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return this;
	}

	@Override
	public IResponseBuilder text(String name, String value) {
		return text(currentNS, name, value);
	}

	private final class NextChunk implements Handler<AsyncResult<Message<LocalJsonObject<Chunk>>>> {

		private EventBus eb;
		private IResponseBuilder self;
		private Callback<IResponseBuilder> end;
		private WbxmlOutput output;
		private String streamId;
		private long total = 0;

		public NextChunk(EventBus eb, String streamId, WbxmlOutput o, IResponseBuilder self,
				Callback<IResponseBuilder> end) {
			this.eb = eb;
			this.streamId = streamId;
			this.output = o;
			this.self = self;
			this.end = end;
		}

		@Override
		public void handle(AsyncResult<Message<LocalJsonObject<Chunk>>> event) {
			MDC.put("user", loginForSifting);
			Chunk c = event.result().body().getValue();
			if (c == Chunk.LAST) {
				logger.debug("Last chunk after receiving {}bytes.", total);
				end.onResult(self);
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("Received chunk ({}byte(s))", c.buf.length);
				}
				total += c.buf.length;
				output.write(c.buf, new WbxmlOutput.QueueDrained() {

					@Override
					public void drained() {
						MDC.put("user", loginForSifting);
						next();
						MDC.put("user", "anonymous");
					}
				});
			}
			MDC.put("user", "anonymous");
		}

		public void next() {
			if (logger.isDebugEnabled()) {
				logger.debug("Asking for nextChunk....");
			}
			eb.request(ByteSourceEventProducer.NEXT_CHUNK, streamId, this);
		}

	}

	private void streamToOutput(final DisposableByteSource streamable, final Callback<IResponseBuilder> completion) {
		LocalJsonObject<DisposableByteSource> source = new LocalJsonObject<>(streamable);
		final EventBus eb = VertxPlatform.eventBus();
		final IResponseBuilder self = this;
		eb.request(ByteSourceEventProducer.REGISTER, source, new Handler<AsyncResult<Message<String>>>() {

			@Override
			public void handle(AsyncResult<Message<String>> streamIdMsg) {
				MDC.put("user", loginForSifting);
				String stream = streamIdMsg.result().body();
				logger.debug("Stream {} ready to go", stream);
				containerNamesStack.peek().setTextContent("[binary " + stream + "]");
				NextChunk nc = new NextChunk(eb, stream, output, self, completion);
				nc.next();
				MDC.put("user", "anonymous");
			}
		});
	}

	private void base64ToOutput(final DisposableByteSource streamable, final Callback<IResponseBuilder> completion) {
		LocalJsonObject<DisposableByteSource> source = new LocalJsonObject<>(streamable);
		final EventBus eb = VertxPlatform.eventBus();
		final IResponseBuilder self = this;
		final Base64Output b64 = new Base64Output(output);
		final Callback<IResponseBuilder> preComplete = new Callback<IResponseBuilder>() {

			@Override
			public void onResult(IResponseBuilder data) {
				MDC.put("user", loginForSifting);
				b64.flush();
				completion.onResult(data);
				MDC.put("user", "anonymous");
			}

		};
		eb.request(ByteSourceEventProducer.REGISTER, source, new Handler<AsyncResult<Message<String>>>() {

			@Override
			public void handle(AsyncResult<Message<String>> streamIdMsg) {
				MDC.put("user", loginForSifting);
				String stream = streamIdMsg.result().body();
				logger.info("Stream {} ready to go as base64", stream);
				containerNamesStack.peek().setTextContent("[base64 " + stream + "]");
				NextChunk nc = new NextChunk(eb, stream, b64, self, preComplete);
				nc.next();
				MDC.put("user", "anonymous");
			}
		});
	}

	@Override
	public void stream(NamespaceMapping ns, String name, DisposableByteSource streamable,
			final Callback<IResponseBuilder> completion) {
		container(ns, name);
		try {
			encoder.startString();
			streamToOutput(streamable, new Callback<IResponseBuilder>() {

				@Override
				public void onResult(IResponseBuilder data) {
					MDC.put("user", loginForSifting);
					try {
						encoder.endString();
						data.endContainer();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
					completion.onResult(data);
					MDC.put("user", "anonymous");
				}
			});
		} catch (IOException e1) {
			logger.error(e1.getMessage(), e1);
		}
	}

	@Override
	public void base64(NamespaceMapping ns, String name, DisposableByteSource streamable,
			final Callback<IResponseBuilder> completion) {
		container(ns, name);
		try {
			encoder.startString();
			base64ToOutput(streamable, new Callback<IResponseBuilder>() {

				@Override
				public void onResult(IResponseBuilder data) {
					MDC.put("user", loginForSifting);
					try {
						encoder.endString();
						data.endContainer();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
					completion.onResult(data);
					MDC.put("user", "anonymous");
				}
			});
		} catch (IOException e1) {
			logger.error(e1.getMessage(), e1);
		}
	}

	@Override
	public void stream(String name, DisposableByteSource streamable, Callback<IResponseBuilder> completion) {
		stream(currentNS, name, streamable, completion);
	}

	@Override
	public IResponseBuilder endContainer() {
		encoder.end();
		Element name = containerNamesStack.pop();
		if (logger.isDebugEnabled()) {
			if (!containerNamesStack.isEmpty()) {
				logger.info("[{}], poped container was {}", containerNamesStack.peek(), name, new Throwable());
			} else {
				logger.info("LAST POP {}", name);
			}
		}
		return this;
	}

	@Override
	public void opaqueStream(NamespaceMapping ns, String name, DisposableByteSource streamable,
			final Callback<IResponseBuilder> completion) {
		container(ns, name);
		try {
			encoder.startByteArray((int) streamable.size());
			streamToOutput(streamable, new Callback<IResponseBuilder>() {

				@Override
				public void onResult(IResponseBuilder data) {
					MDC.put("user", loginForSifting);
					encoder.endByteArray();
					data.endContainer();
					completion.onResult(data);
					MDC.put("user", "anonymous");
				}
			});
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void end(Callback<Void> completion) {
		MDC.put("user", loginForSifting);
		endContainer();
		String reqId = output.end();
		dumpDom(reqId);
		completion.onResult(null);
		MDC.put("user", "anonymous");
	}

	private void dumpDom(String requestId) {
		boolean valid = false;
		try {
			Validator.get().checkResponse(14.1, debugDom);
			valid = true;
		} catch (ValidationException ve) {
			logger.error("rid: " + requestId + ", EAS sent a non-conforming response: " + ve.getMessage(), ve);
		}
		if (GlobalConfig.DATA_IN_LOGS) {
			DOMDumper.dumpXml(logger, "rid: " + requestId + (valid ? ", VALID" : ", INVALID") + " wbxml sent to PDA:\n",
					debugDom);
		}
	}

}
