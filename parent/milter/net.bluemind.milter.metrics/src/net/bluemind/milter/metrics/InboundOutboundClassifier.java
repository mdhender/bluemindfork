/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.milter.metrics;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.address.AddressBuilder;
import org.apache.james.mime4j.field.address.ParseException;
import org.apache.james.mime4j.stream.Field;

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Registry;

import io.netty.buffer.Unpooled;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.milter.IMilterListener;
import net.bluemind.milter.MilterHeaders;
import net.bluemind.milter.MilterInstanceID;
import net.bluemind.milter.SmtpEnvelope;
import net.bluemind.milter.action.DomainAliasCache;

public class InboundOutboundClassifier implements IMilterListener {

	public static enum TrafficClass {
		INTERNAL, EXTERNAL;
	}

	public static class ClassifiedAddress {
		public final String email;
		public final TrafficClass traficClass;

		public ClassifiedAddress(String email, TrafficClass klass) {
			this.email = email;
			this.traficClass = klass;
		}
	}

	private ClassifiedAddress from;
	private final List<ClassifiedAddress> recipients;
	private final Registry registry;
	private final IdFactory idFactory;
	private int size;

	private Optional<ClassifiedAddress> classify(String email) {
		try {
			Mailbox parsed = AddressBuilder.DEFAULT.parseMailbox(email, DecodeMonitor.SILENT);
			ItemValue<Domain> domain = DomainAliasCache.getDomain(parsed.getDomain());
			if (domain != null) {
				return Optional.of(new ClassifiedAddress(parsed.getAddress(), TrafficClass.INTERNAL));
			} else {
				return Optional.of(new ClassifiedAddress(parsed.getAddress(), TrafficClass.EXTERNAL));
			}
		} catch (ParseException e) {
			return Optional.empty();
		}
	}

	public InboundOutboundClassifier(Registry registry, IdFactory idFactory) {
		this.recipients = new LinkedList<>();
		this.registry = registry;
		this.idFactory = idFactory;
	}

	@Override
	public Status onEnvFrom(String envFrom) {
		from = classify(envFrom).orElse(null);
		return Status.CONTINUE;
	}

	@Override
	public Status onEnvRcpt(String rcpt) {
		Optional<ClassifiedAddress> recipient = classify(rcpt);
		if (recipient.isPresent()) {
			recipients.add(recipient.get());
		}
		return Status.CONTINUE;
	}

	@Override
	public Status onHeader(String headerf, String headerv) {
		return Status.CONTINUE;
	}

	@Override
	public Status onEoh() {
		return Status.CONTINUE;
	}

	@Override
	public Status onBody(ByteBuffer bodyp) {
		this.size = Unpooled.wrappedBuffer(bodyp).readableBytes();
		return Status.CONTINUE;
	}

	@Override
	public Status onMessage(SmtpEnvelope envelope, Message message) {
		Field handled = message.getHeader().getField(MilterHeaders.HANDLED);
		if (handled == null || handled.getBody().equals(MilterInstanceID.get())) {
			if (from != null) {
				long inbound = 0;
				long outbound = 0;
				long internal = 0;
				if (from.traficClass == TrafficClass.INTERNAL) {
					outbound = recipients.stream().filter(r -> r.traficClass == TrafficClass.EXTERNAL).count();
					internal = recipients.stream().filter(r -> r.traficClass == TrafficClass.INTERNAL).count();
				} else if (from.traficClass == TrafficClass.EXTERNAL) {
					inbound = recipients.stream().filter(r -> r.traficClass == TrafficClass.INTERNAL).count();
				}
				Counter inboundCounter = registry.counter(idFactory.name("class", "type", "INBOUND"));
				Counter inboundSizeCounter = registry.counter(idFactory.name("size", "type", "INBOUND"));
				Counter internalCounter = registry.counter(idFactory.name("class", "type", "INTERNAL"));
				Counter internalSizeCounter = registry.counter(idFactory.name("size", "type", "INTERNAL"));
				Counter outboundCounter = registry.counter(idFactory.name("class", "type", "OUTBOUND"));
				Counter outboundSizeCounter = registry.counter(idFactory.name("size", "type", "OUTBOUND"));
				if (inbound > 0) {
					inboundCounter.increment(inbound);
					inboundSizeCounter.increment(inbound * size);
				}
				if (outbound > 0) {
					outboundCounter.increment(outbound);
					outboundSizeCounter.increment(outbound * size);
				}
				if (internal > 0) {
					internalCounter.increment(internal);
					internalSizeCounter.increment(internal * size);
				}
			}

		}
		return Status.CONTINUE;
	}

}
