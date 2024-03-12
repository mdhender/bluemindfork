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
import net.bluemind.eas.utils.EasLogUser;
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
		this.loginForSifting = loginForSifting != null ? loginForSifting.replace("@", "_at_") : EasLogUser.ANONYMOUS;
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
			EasLogUser.logExceptionAsUser(loginForSifting, e, logger);
		}
		return this;
	}

	@Override
	public IResponseBuilder container(NamespaceMapping ns, String name) {
		nsSetup(ns, name);
		try {
			encoder.writeElement(name);
		} catch (IOException e) {
			EasLogUser.logExceptionAsUser(loginForSifting, e, logger);
		}
		return this;
	}

	private void nsSetup(NamespaceMapping ns, String name) {
		if (currentNS != ns) {
			try {
				encoder.switchNamespace(ns.namespace());
			} catch (IOException e) {
				EasLogUser.logExceptionAsUser(loginForSifting, e, logger);
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
			EasLogUser.logExceptionAsUser(loginForSifting, e, logger);
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
			EasLogUser.logExceptionAsUser(loginForSifting, e, logger);
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
			if (event.failed()) {
				EasLogUser.logWarnAsUser(loginForSifting, logger, "Error while streaming to wbxml", event.cause());
			} else {
				Chunk c = event.result().body().getValue();
				if (c == Chunk.LAST) {
					EasLogUser.logDebugAsUser(loginForSifting, logger, "Last chunk after receiving {}bytes.", total);
					end.onResult(self);
				} else if (c == Chunk.UNKNOWN) {
					EasLogUser.logDebugAsUser(loginForSifting, logger, "Ignore unknown stream");
				} else {
					if (logger.isDebugEnabled()) {
						EasLogUser.logDebugAsUser(loginForSifting, logger, "Received chunk ({}byte(s))",
								c.buf.length);
					}
					total += c.buf.length;
					output.write(c.buf, () -> {
						next();
					});
				}
			}
		}

		public void next() {
			if (logger.isDebugEnabled()) {
				EasLogUser.logDebugAsUser(loginForSifting, logger, "Asking for nextChunk....");
			}
			eb.request(ByteSourceEventProducer.NEXT_CHUNK, streamId, this);
		}

	}

	private void streamToOutput(final DisposableByteSource streamable, final Callback<IResponseBuilder> completion) {
		LocalJsonObject<DisposableByteSource> source = new LocalJsonObject<>(streamable);
		final EventBus eb = VertxPlatform.eventBus();
		final IResponseBuilder self = this;
		eb.request(ByteSourceEventProducer.REGISTER, source, (AsyncResult<Message<String>> streamIdMsg) -> {
			String stream = streamIdMsg.result().body();
			output.setStreamId(stream);
			EasLogUser.logDebugAsUser(loginForSifting, logger, "Stream {} ready to go", stream);
			containerNamesStack.peek().setTextContent("[binary " + stream + "]");
			NextChunk nc = new NextChunk(eb, stream, output, self, completion);
			nc.next();
		});
	}

	private void base64ToOutput(final DisposableByteSource streamable, final Callback<IResponseBuilder> completion) {
		LocalJsonObject<DisposableByteSource> source = new LocalJsonObject<>(streamable);
		final EventBus eb = VertxPlatform.eventBus();
		final IResponseBuilder self = this;
		final Base64Output b64 = new Base64Output(output);
		final Callback<IResponseBuilder> preComplete = responseBuilder -> {
			b64.flush();
			completion.onResult(responseBuilder);
		};
		eb.request(ByteSourceEventProducer.REGISTER, source, (AsyncResult<Message<String>> streamIdMsg) -> {
			String stream = streamIdMsg.result().body();
			output.setStreamId(stream);
			EasLogUser.logInfoAsUser(loginForSifting, logger, "Stream {} ready to go as base64", stream);
			containerNamesStack.peek().setTextContent("[base64 " + stream + "]");
			NextChunk nc = new NextChunk(eb, stream, b64, self, preComplete);
			nc.next();
		});
	}

	@Override
	public void stream(NamespaceMapping ns, String name, DisposableByteSource streamable,
			final Callback<IResponseBuilder> completion) {
		container(ns, name);
		try {
			encoder.startString();
			streamToOutput(streamable, (IResponseBuilder data) -> {
				try {
					encoder.endString();
					data.endContainer();
				} catch (IOException e) {
					EasLogUser.logExceptionAsUser(loginForSifting, e, logger);
				}
				completion.onResult(data);
			});
		} catch (IOException e1) {
			EasLogUser.logExceptionAsUser(loginForSifting, e1, logger);
		}
	}

	@Override
	public void base64(NamespaceMapping ns, String name, DisposableByteSource streamable,
			final Callback<IResponseBuilder> completion) {
		container(ns, name);
		try {
			encoder.startString();
			base64ToOutput(streamable, (IResponseBuilder data) -> {
				try {
					encoder.endString();
					data.endContainer();
				} catch (IOException e) {
					EasLogUser.logExceptionAsUser(loginForSifting, e, logger);
				}
				completion.onResult(data);
			});
		} catch (IOException e1) {
			EasLogUser.logExceptionAsUser(loginForSifting, e1, logger);
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
				EasLogUser.logInfoAsUser(loginForSifting, logger, "[{}], poped container was {}",
						containerNamesStack.peek(), name, new Throwable());
			} else {
				EasLogUser.logInfoAsUser(loginForSifting, logger, "LAST POP {}", name);
			}
		}
		return this;
	}

	@Override
	public void opaqueStream(NamespaceMapping ns, String name, DisposableByteSource streamable,
			final Callback<IResponseBuilder> completion) {
		container(ns, name);
		try {
			encoder.startByteArray(streamable.size());
			streamToOutput(streamable, (IResponseBuilder data) -> {
				encoder.endByteArray();
				data.endContainer();
				completion.onResult(data);
			});
		} catch (IOException e) {
			EasLogUser.logExceptionAsUser(loginForSifting, e, logger);
		}
	}

	@Override
	public void end(Callback<Void> completion) {
		endContainer();
		String reqId = output.end();
		dumpDom(reqId);
		completion.onResult(null);
	}

	private void dumpDom(String requestId) {
		boolean valid = false;
		try {
			Validator.get().checkResponse(14.1, debugDom);
			valid = true;
		} catch (ValidationException ve) {
			EasLogUser.logErrorExceptionAsUser(loginForSifting, ve, logger,
					"rid: " + requestId + ", EAS sent a non-conforming response: " + ve.getMessage(), ve);
		}
		if (GlobalConfig.logDataForUser(loginForSifting)) {
			DOMDumper.dumpXml(logger, "rid: " + requestId + (valid ? ", " : ", INVALID") + " wbxml sent to device:\n",
					debugDom, loginForSifting);
		}
	}

}
