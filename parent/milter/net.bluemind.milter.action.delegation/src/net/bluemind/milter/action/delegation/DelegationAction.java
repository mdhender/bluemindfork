/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.milter.action.delegation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.columba.ristretto.message.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.Regex;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.milter.IMilterListener;
import net.bluemind.milter.SysconfHelper;
import net.bluemind.milter.action.MilterAction;
import net.bluemind.milter.action.MilterActionException;
import net.bluemind.milter.action.MilterActionsFactory;
import net.bluemind.milter.action.UpdatedMailMessage;
import net.bluemind.milter.cache.DirectoryCache;
import net.bluemind.milter.cache.DomainAliasCache;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.user.api.IUser;

public class DelegationAction implements MilterAction {
	private static final Logger logger = LoggerFactory.getLogger(DelegationAction.class);
	private static final IMilterListener.Status SMTP_ERROR_STATUS = IMilterListener.Status.DELEGATION_ACL_FAIL;

	public static class DelegationActionFactory implements MilterActionsFactory {

		@Override
		public MilterAction create() {
			return new DelegationAction();
		}
	}

	@Override
	public String identifier() {
		return "milter.delegation";
	}

	@Override
	public String description() {
		return "Milter delegation action";
	}

	@Override
	public void execute(UpdatedMailMessage modifier, Map<String, String> configuration,
			Map<String, String> evaluationData, IClientContext context) {
		ItemValue<Domain> contextDomain = context.getSenderDomain();
		String contextAliasDomain = contextDomain.value.defaultAlias;
		ItemValue<Domain> domainItem = DomainAliasCache.getDomain(contextAliasDomain);
		if (domainItem == null) {
			String msg = String.format("Cannot find domain/alias %s", contextDomain);
			logger.warn(msg);
			throw new MilterActionException(msg);
		}

		if (!modifier.getMessage().getFrom().isEmpty()) {
			resolveConnectedUserAddress(context, modifier)
					.ifPresent(info -> verifyAclAndApplyHeader(modifier, context, info));
		}

	}

	private boolean isAdmin(String senderAddress) {
		return "admin0@global.virt".equals(senderAddress);
	}

	private void verifyAclAndApplyHeader(UpdatedMailMessage modifier, IClientContext context,
			DelegationHeaderInfo info) {
		try {
			DirectoryCache.getUserUidByEmail(context, context.getSenderDomain().uid, info.sender)
					.ifPresentOrElse(senderUserUid -> DirectoryCache
							.getUserUidByEmail(context, context.getSenderDomain().uid, info.from)
							.ifPresentOrElse(fromUserUid -> {
								if (!senderUserUid.equals(fromUserUid)) {
									canSendAsOnBehalf(context, modifier, senderUserUid, fromUserUid, info);
								}
							}, () -> {
								verifySenderCanUseExternalIdentity(senderUserUid, modifier, context, info.sender);
							}),
							() -> logger.error("User (email sender) matching to address '{}' not found", info.sender));
		} catch (ServerFault e) {
			if (e.getCode() == ErrorCode.INVALID_PARAMETER) {
				modifier.errorStatus = SMTP_ERROR_STATUS;
				String errorMsg = """
						Message cannot be delivered because one of these login email have not been found
						Sender: '%s'
						From: '%s'

						Return SMTP Status: %s
						""".formatted(info.sender, info.from, SMTP_ERROR_STATUS);
				logger.error(errorMsg);
			} else {
				modifier.errorStatus = SMTP_ERROR_STATUS;
				String errorMsg = """
						Message cannot be delivered
						Sender: '%s'
						From: '%s'

						Return SMTP Status: %s
						Error message: %s
						""".formatted(info.sender, info.from, SMTP_ERROR_STATUS, e.getMessage());
				logger.error(errorMsg);
			}
		}
	}

	private void verifySenderCanUseExternalIdentity(String senderUserUid, UpdatedMailMessage modifier,
			IClientContext context, String senderAddress) {
		if (!hasRole(context, senderUserUid)) {
			modifier.errorStatus = SMTP_ERROR_STATUS;
			String msg = """
					Message cannot be delivered because '%s' has no been found as an External Identity

					Return SMTP Status: %s
					""".formatted(senderAddress, SMTP_ERROR_STATUS);
			logger.error(msg);
		}
	}

	private boolean hasRole(IClientContext context, String senderUserUid) {
		return context.provider().instance(IUser.class, context.getSenderDomain().uid) //
				.getResolvedRoles(senderUserUid).stream()
				.anyMatch(role -> role.equals(BasicRoles.ROLE_EXTERNAL_IDENTITY));
	}

	private Optional<DelegationHeaderInfo> resolveConnectedUserAddress(IClientContext context,
			UpdatedMailMessage modifier) {

		Optional<String> authentLogin = Optional.ofNullable(modifier.properties.get("{auth_authen}"))
				.map(authAuthens -> authAuthens.stream().map(Strings::emptyToNull).filter(Objects::nonNull).findFirst()
						.orElse(null));

		Optional<String> sender = authentLogin.filter(a -> !Regex.EMAIL.validate(a))
				.map(a -> DomainAliasCache.getDomainFromEmail(a).map(DomainAliasCache::getDomainAlias)
						.orElseGet(() -> DomainAliasCache.getDomainAlias(SysconfHelper.defaultDomain.get())))
				.map(d -> Optional.ofNullable(authentLogin.get().concat("@").concat(d))).orElse(authentLogin);

		return sender.map(senderAddress -> {
			if (isAdmin(senderAddress)) {
				return null;
			}

			String fromAddress = modifier.getMessage().getFrom().get(0).getAddress();
			if (!senderAddress.equalsIgnoreCase(fromAddress)) {
				return DelegationHeaderInfo.create(this, senderAddress, fromAddress);
			}

			return null;
		});
	}

	private void canSendAsOnBehalf(IClientContext context, UpdatedMailMessage modifier, String senderUid,
			String fromUid, DelegationHeaderInfo info) {
		String msg = """
				Try to send a message using delegation:
				Domain: %s
				Sender Address: %s
				From Address: %s
				""".formatted(context.getSenderDomain().uid, info.sender, info.from);
		logger.info(msg);

		List<AccessControlEntry> container = context.provider()
				.instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(fromUid)).getAccessControlList();

		List<Verb> filteredVerbs = container.stream()
				.filter(v -> (v.verb.can(Verb.SendAs) || v.verb.can(Verb.SendOnBehalf)) && v.subject.equals(senderUid))
				.map(v -> v.verb).toList();

		if (filteredVerbs.isEmpty()) {
			modifier.errorStatus = SMTP_ERROR_STATUS;
			String errorMsg = """
					Message cannot be delivered because of insufficient delegation rights
					Sender: '%s'
					From: '%s'

					Return SMTP Status: %s
					""".formatted(info.sender, info.from, SMTP_ERROR_STATUS);
			logger.error(errorMsg);
		} else {
			if (filteredVerbs.stream().noneMatch(v -> v.can(Verb.SendAs))) {
				addSenderHeader(context, modifier, info);
			}
		}
	}

	private void addSenderHeader(IClientContext context, UpdatedMailMessage modifier, DelegationHeaderInfo info) {
		try {
			Optional<VCard> vCard = DirectoryCache.getVCard(context, context.getSenderDomain().uid, info.sender);
			String displayname = vCard.isPresent() ? vCard.get().identification.formatedName.value
					: DomainAliasCache.getLeftPartFromEmail(info.sender).orElse(info.sender);
			modifier.addHeader("Sender", new Address(displayname, info.sender).toString(), identifier());
		} catch (Exception e) {
			throw new MilterActionException(e);
		}
	}

	private class DelegationHeaderInfo {
		private String sender;
		private String from;

		private static DelegationHeaderInfo create(DelegationAction action, String sender, String from) {
			DelegationHeaderInfo info = action.new DelegationHeaderInfo();
			info.sender = sender;
			info.from = from;
			return info;
		}
	}

}
