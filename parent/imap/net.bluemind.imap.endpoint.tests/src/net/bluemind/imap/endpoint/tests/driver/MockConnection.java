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
package net.bluemind.imap.endpoint.tests.driver;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.vertx.core.streams.WriteStream;
import net.bluemind.imap.endpoint.driver.Acl;
import net.bluemind.imap.endpoint.driver.CopyResult;
import net.bluemind.imap.endpoint.driver.FetchedItem;
import net.bluemind.imap.endpoint.driver.IdleToken;
import net.bluemind.imap.endpoint.driver.ListNode;
import net.bluemind.imap.endpoint.driver.MailPart;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.NamespaceInfos;
import net.bluemind.imap.endpoint.driver.QuotaRoot;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.imap.endpoint.driver.UpdateMode;

public class MockConnection implements MailboxConnection {

	private static final Logger logger = LoggerFactory.getLogger(MockConnection.class);

	private String ak;
	private String sk;
	private MockModel model;

	public MockConnection(MockModel model, String ak, String sk) {
		this.model = model;
		this.ak = ak;
		this.sk = sk;
		logger.info("Created for {}/{}", this.ak, this.sk);
	}

	@Override
	public SelectedFolder select(String fName) {
		return model.byName(fName);
	}

	@Override
	public List<ListNode> list(String reference, String mailboxPattern) {
		return model.folders.values().stream().map(sf -> sf.folder).sorted((f1, f2) -> {
			if (f1.value.fullName.equals("INBOX")) {
				return -1;
			}
			if (f2.value.fullName.equals("INBOX")) {
				return 1;
			}
			return f1.value.fullName.compareTo(f2.value.fullName);
		}).map(f -> {
			ListNode ln = new ListNode();
			ln.imapMountPoint = f.value.fullName;
			ln.hasChildren = false;
			ln.specialUse = Collections.emptyList();
			return ln;
		}).collect(Collectors.toList());
	}

	@Override
	public CompletableFuture<Void> fetch(SelectedFolder selected, String idset, List<MailPart> fetchSpec,
			WriteStream<FetchedItem> output) {
		System.err.println("Fetch " + selected + " " + idset);
		output.end();
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public QuotaRoot quota(SelectedFolder sf) {
		QuotaRoot qr = new QuotaRoot();
		qr.rootName = "INBOX";
		return qr;
	}

	@Override
	public void idleMonitor(SelectedFolder selected, WriteStream<IdleToken> ctx) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notIdle() {
		// TODO Auto-generated method stub

	}

	@Override
	public long append(String folder, List<String> flags, Date deliveryDate, ByteBuf buffer) {
		return 42L;
	}

	@Override
	public void updateFlags(SelectedFolder sf, String idset, UpdateMode mode, List<String> flags) {
		logger.info("[{}] Should update flags of {}", sf.folder.displayName, idset);
	}

	@Override
	public void updateFlags(SelectedFolder sf, List<Long> uids, UpdateMode mode, List<String> flags) {
		logger.info("[{}] Should update flags of {}", sf.folder.displayName, uids);
	}

	@Override
	public int maxLiteralSize() {
		return 1024 * 1024;
	}

	@Override
	public CopyResult copyTo(SelectedFolder source, String folder, String idset) {
		return new CopyResult(idset, 42L, 42L, 123456);
	}

	@Override
	public List<Long> uids(SelectedFolder sel, String query) {
		return Collections.emptyList();
	}

	@Override
	public String create(String fName) {
		model.registerFolder(UUID.randomUUID(), fName);
		return fName;
	}

	@Override
	public boolean delete(String fName) {
		SelectedFolder selectedFolder = model.byName(fName);
		if (selectedFolder == null) {
			return false;
		} else {
			model.remove(selectedFolder);
			return true;
		}
	}

	@Override
	public boolean subscribe(String fName) {
		return true;
	}

	@Override
	public boolean unsubscribe(String fName) {
		return true;
	}

	@Override
	public String rename(String fName, String newName) {
		SelectedFolder selectedFolder = model.byName(fName);
		if (selectedFolder == null) {
			return null;
		}
		return model.rename(selectedFolder, newName);
	}

	@Override
	public NamespaceInfos namespaces() {
		return new NamespaceInfos("Other guys", "Shared stuff");
	}

	@Override
	public String imapAcl(SelectedFolder selected) {
		return Acl.ALL;
	}
}
