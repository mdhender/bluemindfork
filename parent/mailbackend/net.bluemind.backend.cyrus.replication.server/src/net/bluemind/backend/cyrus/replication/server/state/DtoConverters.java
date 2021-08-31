/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.cyrus.replication.server.state;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.io.BaseEncoding;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.cyrus.replication.server.state.MboxRecord.MessageRecordBuilder;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.ConversationAnnotation;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.backend.mail.replica.api.MailboxRecordAnnotation;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplica.Acl;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.lib.jutf7.UTF7Converter;

public class DtoConverters {

	private static final Logger logger = LoggerFactory.getLogger(DtoConverters.class);

	private DtoConverters() {

	}

	public interface DtoNameConverter {
		String toReplica(MailboxReplicaRootDescriptor root, String nameWithoutPart);

		String fromReplica(String partition, MailboxReplicaRootDescriptor rd, MailboxReplica mr);
	}

	private static final DtoNameConverter userConverter = new DtoNameConverter() {

		@Override
		public String toReplica(MailboxReplicaRootDescriptor root, String nameWithoutPart) {
			String name = nameWithoutPart;
			if (root.isRoot(name)) {
				name = "INBOX";
			} else {
				String tmp = nameWithoutPart;
				String intFull = root.internalFullName();
				int toSkip = intFull.length() + 1;
				tmp = tmp.substring(toSkip).replace('.', '/').replace('^', '.');
				name = tmp;
			}
			return name;
		}

		@Override
		public String fromReplica(String partition, MailboxReplicaRootDescriptor rd, MailboxReplica mr) {
			String nameBase = partition.replace('_', '.') + "!" + rd.internalFullName();
			if (!"INBOX".equals(mr.fullName)) {
				// not the root
				nameBase = addFolderPart(nameBase, mr.fullName);
			}
			return nameBase;
		}
	};

	private static final DtoNameConverter sharedConverter = new DtoNameConverter() {

		@Override
		public String toReplica(MailboxReplicaRootDescriptor root, String nameWithoutPart) {
			String name = nameWithoutPart;
			if (root.isRoot(name)) {
				name = root.name.replace('^', '.');
			} else {
				name = name.replace('.', '/').replace('^', '.');
			}
			return name;
		}

		@Override
		public String fromReplica(String partition, MailboxReplicaRootDescriptor rd, MailboxReplica mr) {
			String nameBase = partition.replace('_', '.') + "!" + rd.internalFullName();
			int slash = mr.fullName.indexOf('/');
			if (slash != -1) {
				String trail = mr.fullName.substring(slash + 1);
				nameBase = addFolderPart(nameBase, trail);
			}
			return nameBase;
		}
	};

	public static String toReplicaName(MailboxReplicaRootDescriptor root, String nameWithoutPart) {
		return root.ns == Namespace.users ? userConverter.toReplica(root, nameWithoutPart)
				: sharedConverter.toReplica(root, nameWithoutPart);
	}

	private static String fromReplicaName(String partition, MailboxReplicaRootDescriptor rd, MailboxReplica mr) {
		return rd.ns == Namespace.users ? userConverter.fromReplica(partition, rd, mr)
				: sharedConverter.fromReplica(partition, rd, mr);
	}

	private static String addFolderPart(String nameBase, String trail) {
		String encoded = UTF7Converter.encode(trail).replace('.', '^').replace('/', '.');
		return nameBase + "." + encoded;
	}

	public static MailboxReplica from(MailboxReplicaRootDescriptor root, String nameWithoutPart, MailboxFolder mf,
			Namespace ns) {
		MailboxReplica mr = new MailboxReplica();
		mr.fullName = toReplicaName(root, nameWithoutPart);
		mr.highestModSeq = mf.getHighestModSeq();
		mr.xconvModSeq = mf.getXConvModSeq();
		mr.lastUid = mf.getLastUid();
		mr.recentUid = mf.getRecentUid();
		mr.options = mf.getOptions();
		mr.syncCRC = mf.getSyncCRC();
		mr.quotaRoot = mf.getQuotaRoot();
		mr.uidValidity = mf.getUidValidity();
		mr.lastAppendDate = new Date(mf.getLastAppendDate() * 1000);
		mr.pop3LastLogin = new Date(mf.getPop3lastLogin() * 1000);
		mr.recentTime = new Date(mf.getRecentTime() * 1000);
		Iterator<String> aclIterator = Splitter.on('\t').omitEmptyStrings().trimResults().split(mf.getAcl()).iterator();
		mr.acls = new LinkedList<>();
		while (aclIterator.hasNext()) {
			String subject = aclIterator.next();
			if (aclIterator.hasNext()) {
				String rights = aclIterator.next();
				mr.acls.add(MailboxReplica.Acl.create(subject, rights));
			} else {
				break;
			}
		}
		mr.deleted = ns.expunged();
		return mr;
	}

	public static MailboxFolder from(String partition, MailboxReplicaRootDescriptor rootDesc,
			ItemValue<MailboxReplica> replicaIV) {
		MailboxReplicaRootDescriptor rd = rootDesc;
		if (replicaIV.flags.contains(ItemFlag.Deleted)) {
			logger.debug("Switching namespace for deleted folder {}", replicaIV.value);
			if (rd.ns == Namespace.users) {
				rd = changeNs(rd, Namespace.deleted);
			} else if (rd.ns == Namespace.shared) {
				rd = changeNs(rd, Namespace.deletedShared);
			}
		}
		MailboxReplica mr = replicaIV.value;
		MailboxFolder mf = new MailboxFolder();
		mf.setPartition(CyrusPartition.forServerAndDomain(mr.dataLocation, partition).name);
		mf.setName(fromReplicaName(partition, rd, mr));
		mf.setUniqueId(replicaIV.uid);
		mf.setHighestModSeq(mr.highestModSeq);
		mf.setXConvModSeq(mr.xconvModSeq);
		mf.setLastUid(mr.lastUid);
		mf.setRecentUid(mr.recentUid);
		mf.setOptions(mr.options);
		mf.setSyncCRC(mr.syncCRC);
		mf.setQuotaRoot(mr.quotaRoot);
		mf.setUidValidity(mr.uidValidity);
		mf.setLastAppendDate(mr.lastAppendDate.getTime() / 1000);
		mf.setPop3lastLogin(mr.pop3LastLogin.getTime() / 1000);
		mf.setRecentTime(mr.recentTime.getTime() / 1000);
		mf.setAcl(aclString(mr.acls));
		return mf;
	}

	private static MailboxReplicaRootDescriptor changeNs(MailboxReplicaRootDescriptor toCopy, Namespace ns) {
		MailboxReplicaRootDescriptor copy = new MailboxReplicaRootDescriptor();
		copy.dataLocation = toCopy.dataLocation;
		copy.name = toCopy.name;
		copy.ns = ns;
		return copy;
	}

	private static String aclString(List<Acl> acls) {
		return acls.stream().map(acl -> acl.subject + "\t" + acl.rights).collect(Collectors.joining("\t")) + "\t";
	}

	public static MboxRecord from(MailboxRecord mr) {
		return builderFrom(mr).build();
	}

	public static MboxRecord from(MailboxRecord mr, long conversationId) {
		MessageRecordBuilder builder = builderFrom(mr);
		builder.annotations(
				Collections.<MailboxRecordAnnotation>singletonList(new ConversationAnnotation(conversationId)));
		return builder.build();

	}

	private static MessageRecordBuilder builderFrom(MailboxRecord mr) {
		MessageRecordBuilder b = MboxRecord.builder();
		b.uid(mr.imapUid).modseq(mr.modSeq);
		b.internalDate(mr.internalDate.getTime() / 1000).lastUpdated(mr.lastUpdated.getTime() / 1000);
		b.body(mr.messageBody);
		b.flags(mr.flags.stream().map(item -> item.flag).collect(Collectors.toList()));
		b.annotations(Collections.emptyList());
		return b;
	}

	public static MailboxRecord from(MboxRecord replRec) {
		MailboxRecord mr = new MailboxRecord();
		mr.imapUid = replRec.uid();
		mr.internalDate = new Date(replRec.internalDate() * 1000);
		mr.lastUpdated = new Date(replRec.lastUpdated() * 1000);
		mr.messageBody = replRec.bodyGuid();
		mr.modSeq = replRec.modseq();
		mr.flags = new LinkedList<>();
		for (String f : replRec.flags()) {
			switch (f.toLowerCase()) {
			case "\\answered":
				mr.flags.add(MailboxItemFlag.System.Answered.value());
				break;
			case "\\flagged":
				mr.flags.add(MailboxItemFlag.System.Flagged.value());
				break;
			case "\\deleted":
				mr.flags.add(MailboxItemFlag.System.Deleted.value());
				break;
			case "\\draft":
				mr.flags.add(MailboxItemFlag.System.Draft.value());
				break;
			case "\\seen":
				mr.flags.add(MailboxItemFlag.System.Seen.value());
				break;
			case "\\needscleanup":
				mr.internalFlags.add(InternalFlag.needsCleanup);
				break;
			case "\\archived":
				mr.internalFlags.add(InternalFlag.archived);
				break;
			case "\\unlinked":
				mr.internalFlags.add(InternalFlag.unlinked);
				break;
			case "\\expunged":
				mr.internalFlags.add(InternalFlag.expunged);
				break;
			default:
				mr.flags.add(new MailboxItemFlag(f));
				break;
			}
		}

		mr.conversationId = replRec.annotations().stream().filter(a -> "/vendor/cmu/cyrus-imapd/thrid".equals(a.entry))
				.findFirst().map(a -> new BigInteger(BaseEncoding.base16().decode(a.value.toUpperCase())).longValue())
				.orElse(null);

		return mr;
	}

}
