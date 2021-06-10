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
package net.bluemind.core.backup.continuous.restore.mbox;

public class MboxFillTask {

//	private static final Logger logger = LoggerFactory.getLogger(MboxFillTask.class);
//
//	private ItemValue<Domain> dom;
//	private ItemValue<Mailbox> mboxIv;
//	private final CloneState cloneState;
//	private final List<ILiveStream> avail;
//	private final Map<String, ItemValue<Server>> clonedTopology;
//
//	private final UidDatalocMapping foldersMapping;
//
//	public MboxFillTask(ItemValue<Domain> dom, ItemValue<Mailbox> mboxIv, CloneState cloneState,
//			List<ILiveStream> avail, Map<String, ItemValue<Server>> topo, UidDatalocMapping mapping) {
//		this.dom = dom;
//		this.mboxIv = mboxIv;
//		this.cloneState = cloneState;
//		this.avail = avail;
//		this.clonedTopology = topo;
//		this.foldersMapping = mapping;
//	}
//
//	public Map<String, ItemValue<MailboxReplica>> run(IServerTaskMonitor mon) {
//		String subtree = IMailReplicaUids.subtreeUid(dom.uid, mboxIv);
//		Optional<ILiveStream> subtreeStream = avail.stream().filter(ls -> ls.id().equals(subtree)).findAny();
//
//		ItemValue<Server> imap = clonedTopology.get(mboxIv.value.dataLocation);
//		CyrusPartition partition = CyrusPartition.forServerAndDomain(imap, dom.uid);
//
//		Map<String, ItemValue<MailboxReplica>> ordered = new LinkedHashMap<>();
//		IResumeToken resToken = subtreeStream.map(sts -> {
//			ValueReader<ItemValue<MailboxReplica>> mrReader = JsonUtils
//					.reader(new TypeReference<ItemValue<MailboxReplica>>() {
//					});
//			return sts.subscribe(cloneState.forTopic(sts), de -> {
//				if (de.payload.length == 0) {
//					return;
//				}
//				ItemValue<MailboxReplica> replica = mrReader.read(new String(de.payload));
//				ordered.put(replica.uid, replica);
//				foldersMapping.put(replica, mboxIv, dom, partition);
//			});
//		}).orElse(null);
//
//		mon.log("Connecting with sync protocol for creating mailboxes on " + partition);
//
//		try (SyncClientOIO sc = new SyncClientOIO(imap.value.address(), 2502)) {
//			sc.authenticate("admin0", Token.admin0());
//			for (ItemValue<MailboxReplica> toCreate : ordered.values()) {
//				String box = cyrusMbox(dom, mboxIv, toCreate);
//				String cmd = "APPLY MAILBOX %(UNIQUEID " + toCreate.uid + " MBOXNAME \"" + box + "\" ";
//				cmd += "SYNC_CRC 0 SYNC_CRC_ANNOT 0 LAST_UID 0 HIGHESTMODSEQ " + toCreate.value.highestModSeq
//						+ " RECENTUID 0 ";
//				cmd += "RECENTTIME 0 LAST_APPENDDATE 0 POP3_LAST_LOGIN 0 POP3_SHOW_AFTER 0 UIDVALIDITY "
//						+ toCreate.value.uidValidity + " ";
//				cmd += "PARTITION " + partition.name + " ";
//				cmd += "ACL \"" + mboxIv.value.name + "@" + dom.uid + " lrswipkxtecdan \" ";
//				cmd += "OPTIONS PS RECORD ())\r\n";
//				String syncRes = sc.run(cmd);
//				logger.info("APPLY MAILBOX {} aka {} => {}", toCreate.uid, syncRes);
//			}
//			subtreeStream.ifPresent(sts -> {
//				cloneState.record(sts.fullName(), resToken).save();
//			});
//		} catch (Exception e) {
//			e.printStackTrace();
//			mon.log("ERROR: " + e.getMessage());
//		}
//		return ordered;
//	}
//
//	private String cyrusMbox(ItemValue<Domain> dom, ItemValue<Mailbox> box, ItemValue<MailboxReplica> repl) {
//		String fn = repl.value.fullName;
//		if (box.value.type.sharedNs) {
//			if (fn.equals(box.value.name)) {
//				fn = "";
//			} else {
//				fn = "." + UTF7Converter.encode(fn.replace('.', '^').replace('/', '.'));
//			}
//		} else {
//			if (fn.equals("INBOX")) {
//				fn = "";
//			} else {
//				fn = "." + UTF7Converter.encode(fn.replace('.', '^').replace('/', '.'));
//			}
//		}
//		return dom.uid + "!" + box.value.type.nsPrefix + box.value.name.replace('.', '^') + fn;
//	}

}
