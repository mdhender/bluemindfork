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
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.milter.IMilterListener;
import net.bluemind.milter.action.MilterAction;
import net.bluemind.milter.action.MilterActionException;
import net.bluemind.milter.action.MilterActionsFactory;
import net.bluemind.milter.action.UpdatedMailMessage;
import net.bluemind.milter.cache.DirectoryCache;
import net.bluemind.milter.cache.DomainAliasCache;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserMailIdentities;

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
			String fromAddress = modifier.getMessage().getFrom().get(0).getAddress();
			getConnectedUserEmail(modifier).ifPresent(senderAddress -> {
				if (isNotAdmin(senderAddress) && !senderAddress.equals(fromAddress)) {
					verifyAclAndApplyHeader(modifier, context, senderAddress, fromAddress);
				}
			});
		}
	}

	private boolean isNotAdmin(String senderAddress) {
		return !"admin0@global.virt".equals(senderAddress);
	}

	private void verifyAclAndApplyHeader(UpdatedMailMessage modifier, IClientContext context, String senderAddress,
			String fromAddress) {
		DirectoryCache.getUserUidByEmail(context, context.getSenderDomain().uid, senderAddress)
				.ifPresent(senderUserUid -> DirectoryCache
						.getUserUidByEmail(context, context.getSenderDomain().uid, fromAddress)
						.ifPresentOrElse(fromUserUid -> {
							if (!senderUserUid.equals(fromUserUid)) {
								canSendAsOnBehalf(context, modifier, senderUserUid, fromUserUid,
										context.getSenderDomain().uid, senderAddress);
							}
						}, () -> {
							verifyExternalIdentity(senderUserUid, modifier, context, senderAddress, fromAddress);
						}));
	}

	private void verifyExternalIdentity(String senderUserUid, UpdatedMailMessage modifier, IClientContext context,
			String senderAddress, String fromAddress) {
		if (!hasExternalIdentity(context, senderUserUid, fromAddress)) {
			modifier.errorStatus = SMTP_ERROR_STATUS;
		}
	}

	private boolean hasExternalIdentity(IClientContext context, String senderUserUid, String fromAddress) {
		return hasRole(context, senderUserUid) && identityMatch(context, senderUserUid, fromAddress);
	}

	private boolean hasRole(IClientContext context, String senderUserUid) {
		return context.provider().instance(IUser.class, context.getSenderDomain().uid) //
				.getResolvedRoles(senderUserUid).stream()
				.anyMatch(role -> role.equals(BasicRoles.ROLE_EXTERNAL_IDENTITY));
	}

	private boolean identityMatch(IClientContext context, String senderUserUid, String fromAddress) {
		return context.provider().instance(IUserMailIdentities.class, context.getSenderDomain().uid, senderUserUid)
				.getIdentities().stream().anyMatch(i -> i.email.equals(fromAddress));
	}

	private Optional<String> getConnectedUserEmail(UpdatedMailMessage modifier) {
		return Optional.ofNullable(modifier.properties.get("{auth_authen}")) //
				.flatMap(authAuthens -> authAuthens.stream() //
						.map(Strings::emptyToNull) //
						.filter(Objects::nonNull).findFirst()) //
				.flatMap(login -> {
					Optional<String> resolvedLogin = DomainAliasCache.getLeftPartFromEmail(login);
					Optional<String> resolvedDomain = DomainAliasCache.getDomainFromEmail(login);
					return resolvedLogin.flatMap(l -> resolvedDomain.map(d -> new String[] { l, d }));
				}).map(latd -> {
					String alias = DomainAliasCache.getDomainAlias(latd[1]);
					return latd[0].concat("@").concat(alias);
				});
	}

	private void canSendAsOnBehalf(IClientContext context, UpdatedMailMessage modifier, String senderUid,
			String fromUid, String domainUid, String senderAddress) {
		List<AccessControlEntry> container = context.provider()
				.instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(fromUid)).getAccessControlList();

		List<Verb> filteredVerbs = container.stream()
				.filter(v -> (v.verb.can(Verb.SendAs) || v.verb.can(Verb.SendOnBehalf)) && v.subject.equals(senderUid))
				.map(v -> v.verb).toList();

		if (filteredVerbs.isEmpty()) {
			modifier.errorStatus = SMTP_ERROR_STATUS;
		} else {
			if (filteredVerbs.stream().noneMatch(v -> v.can(Verb.SendAs))) {
				addSenderHeader(context, modifier, domainUid, senderAddress);
			}
		}
	}

	private void addSenderHeader(IClientContext context, UpdatedMailMessage modifier, String domainUid,
			String senderAddress) {
		Optional<VCard> vCard = DirectoryCache.getVCard(context, domainUid, senderAddress);
		String displayname = vCard.isPresent() ? vCard.get().identification.formatedName.value
				: DomainAliasCache.getLeftPartFromEmail(senderAddress).orElse(senderAddress);
		modifier.addHeader("Sender", new Address(displayname, senderAddress).toString(), identifier());
	}

}
