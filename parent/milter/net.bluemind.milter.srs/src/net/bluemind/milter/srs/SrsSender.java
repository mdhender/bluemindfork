/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.milter.srs;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.stream.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.config.InstallationId;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.milter.MilterHeaders;
import net.bluemind.milter.action.DomainAliasCache;
import net.bluemind.milter.action.MilterPreAction;
import net.bluemind.milter.action.MilterPreActionsFactory;
import net.bluemind.milter.action.UpdatedMailMessage;
import net.bluemind.milter.srs.tools.SrsHash;
import net.bluemind.milter.srs.tools.SrsUtils;
import net.bluemind.system.api.SysConfKeys;

public class SrsSender implements MilterPreAction {
	private static final Logger logger = LoggerFactory.getLogger(SrsSender.class);

	public static class SrsSenderFactory implements MilterPreActionsFactory {
		private static final Logger logger = LoggerFactory.getLogger(SrsSenderFactory.class);

		@Override
		public MilterPreAction create() {
			Optional<SrsHash> srsHash = SrsHash.build(InstallationId.getIdentifier());
			if (!srsHash.isPresent()) {
				logger.warn("SRS action is disabled");
			}

			return srsHash.map(srshash -> new SrsSender(srshash)).orElse(null);
		}
	}

	private final SrsHash srsHash;
	private final Supplier<String> defaultDomain;

	/**
	 * Use only for JUnits
	 * 
	 * @param defaultDomain
	 * @return
	 */
	public static SrsSender build(String defaultDomain) {
		return SrsHash.build(InstallationId.getIdentifier()).map(srsHash -> new SrsSender(srsHash, defaultDomain))
				.orElse(null);
	}

	private SrsSender(SrsHash srsHash) {
		this.srsHash = srsHash;

		AtomicReference<SharedMap<String, String>> sysconf = new AtomicReference<>();
		MQ.init().thenAccept(v -> sysconf.set(MQ.sharedMap("system.configuration")));

		defaultDomain = () -> Optional.ofNullable(sysconf.get())
				.map(sm -> sm.get(SysConfKeys.default_domain.name()) != null
						&& !sm.get(SysConfKeys.default_domain.name()).isEmpty()
								? sm.get(SysConfKeys.default_domain.name())
								: null)
				.orElse(null);
	}

	private SrsSender(SrsHash srsHash, String defaultDomain) {
		this.srsHash = srsHash;
		this.defaultDomain = () -> defaultDomain;
	}

	@Override
	public String getIdentifier() {
		return "milter.srs.sender";
	}

	@Override
	public boolean execute(UpdatedMailMessage modifiedMail) {
		Collection<String> mailFrom = modifiedMail.properties.get("{mail_addr}");
		if (mailFrom == null) {
			logger.warn("No mail from value: {}", modifiedMail.properties.get("{mail_addr}"));
			return false;
		}

		String senderEmail = mailFrom.stream().filter(e -> !Strings.isNullOrEmpty(e)).findFirst().orElse(null);
		if (senderEmail == null) {
			logger.warn("Invalid mail from value: {}", modifiedMail.properties.get("{mail_addr}"));
			return false;
		}

		Collection<String> rcptTo = modifiedMail.properties.get("{rcpt_addr}");
		if (rcptTo == null) {
			// No mail recipient ?!
			logger.warn("No recipient address ?!");
			return false;
		}

		if (DomainAliasCache.allAliases().stream().anyMatch(d -> senderEmail.endsWith("@" + d))) {
			if (logger.isDebugEnabled()) {
				logger.debug("sender is from local domain {}", senderEmail);
			}

			// senderEmail is from local domain - no sender SRS
			return false;
		}

		if (rcptTo.stream().map(SrsUtils::getDomainFromEmail).filter(Optional::isPresent).map(Optional::get)
				.allMatch(d -> DomainAliasCache.allAliases().contains(d))) {
			if (logger.isDebugEnabled()) {
				logger.debug("recipients are in local domain {}", rcptTo);
			}

			// recipients are in local domain - no sender SRS
			return false;
		}

		senderSrsDomain(modifiedMail).ifPresent(srsDomain -> SrsData.forEmail(srsHash, senderEmail)
				.ifPresent(srsData -> updateEnvelopSender(modifiedMail, srsDomain, srsData)));

		return modifiedMail.envelopSender.isPresent();
	}

	private void updateEnvelopSender(UpdatedMailMessage modifiedMail, String srsDomain, SrsData srsData) {
		if (logger.isDebugEnabled()) {
			logger.debug("Update envelop sender from {} to {}", srsData.originalEmail(), srsData.srsEmail(srsDomain));
		}

		modifiedMail.envelopSender = Optional.of(srsData.srsEmail(srsDomain));
	}

	private Optional<String> senderSrsDomain(UpdatedMailMessage modifiedMail) {
		Optional<String> senderSrs = senderSrsDomainFromHeader(modifiedMail);

		if (senderSrs.isPresent()) {
			if (logger.isDebugEnabled()) {
				logger.info("SRS sender domain get from " + MilterHeaders.SIEVE_REDIRECT + " header");
			}
			return senderSrs;
		}

		return Optional.ofNullable(modifiedMail.properties.get("{auth_authen}"))
				.map(authAuthens -> authAuthens.stream().filter(Objects::nonNull).findFirst().orElse(null))
				.map(login -> SrsUtils.getDomainFromEmail(login).map(this::getDomainAlias)
						.orElseGet(() -> getDomainAlias(defaultDomain.get())));
	}

	private Optional<String> senderSrsDomainFromHeader(UpdatedMailMessage modifiedMail) {
		return Optional.ofNullable(modifiedMail.getMessage()).map(Message::getHeader)
				.map(header -> header.getField(MilterHeaders.SIEVE_REDIRECT)).map(Field::getBody)
				.map(email -> SrsUtils.getDomainFromEmail(email)
						.filter(domain -> DomainAliasCache.allAliases().contains(domain)).orElse(null))
				.map(this::getDomainAlias);
	}

	private String getDomainAlias(String domain) {
		// return domain defaultAlias if domain is domainUid or domain is not an alias.
		// Otherwise, return domain
		return Optional.ofNullable(domain).map(DomainAliasCache::getDomain)
				.filter(domainValue -> domainValue.uid.equals(domain) || !domainValue.value.aliases.contains(domain))
				.map(domainValue -> domainValue.value.defaultAlias).orElse(domain);
	}
}
