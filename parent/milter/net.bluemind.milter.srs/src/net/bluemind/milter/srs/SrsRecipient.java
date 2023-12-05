/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.milter.srs;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.lib.srs.SrsData;
import net.bluemind.lib.srs.SrsHash;
import net.bluemind.milter.cache.DomainAliasCache;
import net.bluemind.milter.map.RecipientCanonical;
import net.bluemind.milter.map.RecipientCanonicalFactory;

public class SrsRecipient implements RecipientCanonical {
	public static class SrsRecipientFactory implements RecipientCanonicalFactory {
		private static final Logger logger = LoggerFactory.getLogger(SrsRecipientFactory.class);

		@Override
		public RecipientCanonical create() {
			Optional<SrsHash> srsHash = SrsHash.build(InstallationId.getIdentifier());
			if (!srsHash.isPresent()) {
				logger.warn("SRS action is disabled");
			}

			return srsHash.map(srshash -> new SrsRecipient(srshash)).orElse(null);
		}
	}

	private final SrsHash srsHash;

	private SrsRecipient(SrsHash srsHash) {
		this.srsHash = srsHash;
	}

	@Override
	public String getIdentifier() {
		return "milter.srs.recipient";
	}

	@Override
	public Optional<String> execute(String email) {
		if (Boolean.TRUE.equals(SysconfHelper.srsDisabled.get())) {
			return Optional.empty();
		}

		return Optional.ofNullable(email).filter(this::isFromLocalDomain).map(this::recipientRewrite)
				.orElseGet(Optional::empty);
	}

	private boolean isFromLocalDomain(String email) {
		return DomainAliasCache.getDomainFromEmail(email).map(d -> DomainAliasCache.allAliases().contains(d))
				.orElse(false);
	}

	private Optional<String> recipientRewrite(String recipient) {
		return DomainAliasCache.getLeftPartFromEmail(recipient)
				.flatMap(leftPart -> SrsData.fromLeftPart(srsHash, leftPart)).map(SrsData::originalEmail);
	}
}
