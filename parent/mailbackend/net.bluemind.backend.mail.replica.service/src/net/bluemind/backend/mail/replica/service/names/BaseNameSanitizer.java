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
package net.bluemind.backend.mail.replica.service.names;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;

import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.persistence.MailboxReplicaStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.lib.jutf7.UTF7Converter;

public abstract class BaseNameSanitizer implements INameSanitizer {

	protected final MailboxReplicaRootDescriptor root;
	protected final MailboxReplicaStore rawStore;
	protected final ContainerStoreService<MailboxReplica> contStore;
	private static final Logger logger = LoggerFactory.getLogger(BaseNameSanitizer.class);

	protected BaseNameSanitizer(MailboxReplicaRootDescriptor root, MailboxReplicaStore rawStore,
			ContainerStoreService<MailboxReplica> contStore) {
		this.root = root;
		this.rawStore = rawStore;
		this.contStore = contStore;
	}

	protected String decodeIfUTF7(String s) {
		if (CharMatcher.ascii().matchesAllOf(s)) {
			try {
				return UTF7Converter.decode(s);
			} catch (Error err) { // NOSONAR
				// because jutf7 does not honor onMalformedInput(REPLACE) and
				// Charset.decode
				// throws an Error in that case
				if (logger.isDebugEnabled()) {
					logger.debug("{} looks like utf-7 but it is not", s);
				}
				return s;
			}
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("{} contains non-ascii chars, not decoding as utf7.", s);
			}
			return s;
		}
	}

	@Override
	public <T extends MailboxFolder> T sanitizeNames(T replica) {
		logger.debug("SAN_IN n: {}, fn: {}, p: {} in {}", replica.name, replica.fullName, replica.parentUid, root);
		T result = sanitizeNamesImpl(replica);
		logger.debug("SAN_OUT n: {}, fn: {}, p: {} in {}", result.name, result.fullName, result.parentUid, root);
		return result;
	}

	private <T extends MailboxFolder> T sanitizeNamesImpl(T replica) {

		if (replica.name == null && replica.fullName == null) {
			throw new ServerFault("One of name or fullName must not be null");
		}

		// name has slashes & the rest is null, tests do that
		if (replica.name != null && replica.fullName == null) {
			int slash = replica.name.lastIndexOf('/');
			if (slash != -1) {
				String name = replica.name.substring(slash + 1);
				String fn = replica.name;
				replica.name = name;
				replica.fullName = fn;
			}
		}

		// root folder of mailbox
		if (replica.name != null && replica.fullName == null && replica.parentUid == null) {
			replica.name = decodeIfUTF7(replica.name.replace('^', '.'));
			if (!validRootName(replica.name)) {
				throw new ServerFault("Invalid root " + replica + " in " + root);
			}
			replica.fullName = replica.name;
			return replica;
		}

		if (replica.name != null && replica.parentUid != null) {
			replica.name = decodeIfUTF7(replica.name.replace('^', '.'));
			ItemValue<MailboxReplica> parent = contStore.get(replica.parentUid, null);
			if (parent == null) {
				throw new ServerFault("name & parentUid given but parentUid " + replica.parentUid + " was not found.");
			}
			replica.fullName = parent.value.fullName + "/" + replica.name;
			return replica;
		}

		if (replica.fullName != null) {
			replica.fullName = decodeIfUTF7(replica.fullName.replace('^', '.'));
			int lastSlash = replica.fullName.lastIndexOf('/');
			if (lastSlash == -1) {
				// a root ?
				if (!validRootName(replica.fullName)) {
					throw new ServerFault("Invalid root " + replica + " in " + root);
				}
				replica.name = replica.fullName;
				replica.parentUid = null;
			} else {
				String parentName = replica.fullName.substring(0, lastSlash);
				replica.name = replica.fullName.substring(lastSlash + 1);
				String computedParentUid = JdbcAbstractStore.doOrFail(() -> rawStore.byName(parentName));
				if (computedParentUid != null) {
					if (replica.parentUid != null && !computedParentUid.equals(replica.parentUid)) {
						logger.warn("[{}] {} parentUid changed from {} to {}", root, replica.fullName,
								replica.parentUid, computedParentUid);
					}
				} else {
					logger.warn("[{}] could not resolve parent {}. Hole in hierarchy !", root, parentName);
				}
				replica.parentUid = computedParentUid;
			}
			return replica;
		}

		throw new ServerFault("Could not sanitize " + replica);
	}

	public abstract boolean validRootName(String name);

}
