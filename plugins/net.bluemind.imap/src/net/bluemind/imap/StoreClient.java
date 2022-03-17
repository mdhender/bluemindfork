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
package net.bluemind.imap;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.imap.impl.ClientHandler;
import net.bluemind.imap.impl.ClientSupport;
import net.bluemind.imap.impl.MailThread;
import net.bluemind.imap.impl.StoreClientCallback;
import net.bluemind.imap.impl.TagProducer;
import net.bluemind.imap.mime.MimeTree;

/**
 * IMAP client entry point
 * 
 * 
 */
public class StoreClient implements AutoCloseable {

	private String password;
	private String login;
	private int port;
	private String hostname;

	private Logger logger = LoggerFactory.getLogger(getClass());
	private ClientSupport cs;

	private static final SocketConnector connector;

	static {
		connector = new NioSocketConnector();
		connector.getSessionConfig().setTcpNoDelay(true);
		ClientHandler handler = new ClientHandler();
		connector.setHandler(handler);
	}

	public StoreClient(String hostname, int port, String login, String password, int timeoutSecs) {
		this(new TagProducer(), hostname, port, login, password, timeoutSecs);
	}

	public StoreClient(String hostname, int port, String login, String password) {
		this(new TagProducer(), hostname, port, login, password);
	}

	public StoreClient(ITagProducer tp, String hostname, int port, String login, String password) {
		this(tp, hostname, port, login, password, 60 * 60);
	}

	public StoreClient(ITagProducer tp, String hostname, int port, String login, String password, int timeoutSecs) {
		if (tp == null) {
			throw new NullPointerException("tag producer cannot be null");
		}
		this.hostname = hostname;
		this.port = port;
		this.login = login;
		this.password = password;

		cs = new ClientSupport(tp, StoreClientCallback::new, timeoutSecs);
	}

	/**
	 * Logs into the IMAP store. Defaults to no-TLS
	 * 
	 * @return true if login is successful
	 * @throws IMAPException
	 */
	public boolean login() {
		return login(false);
	}

	/**
	 * Logs into the IMAP store
	 * 
	 * @return true if login is successful
	 * @throws IMAPException
	 */
	public boolean login(Boolean activateTLS) {
		if (logger.isDebugEnabled()) {
			logger.debug("login attempt to {}:{} for {} / {}", hostname, port, login, password);
		}
		SocketAddress sa = new InetSocketAddress(hostname, port);
		boolean ret = false;
		if (cs.login(login, password, connector, sa, activateTLS)) {
			ret = true;
		}
		return ret;
	}

	/**
	 * Logs out & disconnect from the IMAP server. The underlying network connection
	 * is released.
	 */
	public void logout() {
		if (logger.isDebugEnabled()) {
			logger.debug("logout attempt for {}", login);
		}
		try {
			cs.logout();
		} catch (RuntimeException e) {
			logger.warn("Exception occured during connection logout: {}", e.getMessage());
		}
	}

	/**
	 * Opens the given IMAP folder. Only one folder can be active at a time.
	 * 
	 * @param mailbox utf8 mailbox name.
	 * @throws IMAPException
	 */
	public boolean select(String mailbox) throws IMAPException {
		return cs.select(mailbox);
	}

	public boolean create(String mailbox) throws IMAPException {
		return cs.create(mailbox, null);
	}

	public boolean create(String mailbox, String specialUse) {
		return cs.create(mailbox, specialUse);
	}

	public CreateMailboxResult createMailbox(String mailbox, String partition) throws IMAPException {
		return cs.createMailbox(mailbox, partition);
	}

	public boolean setAcl(String mailbox, String consumer, Acl acl) throws IMAPException {
		return cs.setAcl(mailbox, consumer, acl);
	}

	public boolean setMailboxAcl(String mailbox, String consumer, Acl acl) throws IMAPException {
		String mboxTree = mailbox.substring(0, mailbox.indexOf('@')) + "/*" + mailbox.substring(mailbox.indexOf('@'));
		ListResult mailboxes = cs.listMailbox(mboxTree);

		for (ListInfo mb : mailboxes) {
			if (!setAcl(mb.getName(), consumer, acl)) {
				return false;
			}
		}

		return setAcl(mailbox, consumer, acl);
	}

	public boolean deleteAcl(String mailbox, String consumer) throws IMAPException {
		return cs.deleteAcl(mailbox, consumer);
	}

	public boolean deleteMailboxAcl(String mailbox, String consumer) throws IMAPException {
		String mboxTree = mailbox.substring(0, mailbox.indexOf('@')) + "/*" + mailbox.substring(mailbox.indexOf('@'));
		ListResult mailboxes = cs.listMailbox(mboxTree);

		for (ListInfo mb : mailboxes) {
			if (!deleteAcl(mb.getName(), consumer)) {
				return false;
			}
		}

		return deleteAcl(mailbox, consumer);
	}

	public boolean deleteAllConsumerAcls(String consumer) throws IMAPException {
		ListResult mailboxes = cs.listMailbox("*" + consumer.substring(consumer.indexOf('@')));

		for (ListInfo mb : mailboxes) {
			logger.info(mb.getName());
			if (!deleteAcl(mb.getName(), consumer)) {
				return false;
			}
		}

		return true;
	}

	public Map<String, Acl> listAcl(String mailbox) throws IMAPException {
		return cs.listAcl(mailbox);
	}

	public boolean subscribe(String mailbox) throws IMAPException {
		return cs.subscribe(mailbox);
	}

	public boolean unsubscribe(String mailbox) throws IMAPException {
		return cs.unsubscribe(mailbox);
	}

	public CreateMailboxResult deleteMailbox(String mailbox) throws IMAPException {
		return cs.deleteMailbox(mailbox);
	}

	public CreateMailboxResult deleteMailboxHierarchy(String mbox) throws IMAPException {
		String mboxTree = mbox.substring(0, mbox.indexOf('@')) + "/*" + mbox.substring(mbox.indexOf('@'));
		ListResult mailboxes = cs.listMailbox(mboxTree);

		for (ListInfo mailbox : mailboxes) {
			CreateMailboxResult result = deleteMailbox(mailbox.getName());
			if (!result.isOk()) {
				return result;
			}
		}

		return deleteMailbox(mbox);
	}

	public boolean rename(String mailbox, String newMailbox) throws IMAPException {
		return cs.rename(mailbox, newMailbox);
	}

	/**
	 * Issues the CAPABILITY command to the IMAP server
	 * 
	 * @return
	 * @throws IMAPException
	 */
	public Set<String> capabilities() throws IMAPException {
		return cs.capabilities();
	}

	public boolean noop() {
		return cs.noop();
	}

	public ListResult listSubscribed() {
		return cs.listSubscribed();
	}

	public ListResult listAll() {
		return cs.listAll();
	}

	public ListResult listAllDomain(String virtDomain) {
		return cs.listMailbox("*@" + virtDomain);
	}

	public ListResult listSubFoldersMailbox(String mailbox) {
		String mboxTree = mailbox.substring(0, mailbox.indexOf('@')) + "/*" + mailbox.substring(mailbox.indexOf('@'));
		return cs.listMailbox(mboxTree);
	}

	public int append(String mailbox, InputStream in, FlagsList fl) {
		return cs.append(mailbox, in, fl);
	}

	public int append(String mailbox, InputStream in, FlagsList fl, Date delivery) {
		return cs.append(mailbox, in, fl, delivery);
	}

	public TaggedResult tagged(String imapCommand) {
		return cs.tagged(imapCommand);
	}

	public void expunge() {
		cs.expunge();
	}

	public void uidExpunge(Collection<Integer> uids) {
		cs.uidExpunge(uids);
	}

	public QuotaInfo quota(String mailbox) {
		return cs.quota(mailbox);
	}

	/**
	 * @param mailbox user/admin@buffy.kvm
	 * @param quota   unit is KB, 0 removes the quota
	 * @return
	 */
	public boolean setQuota(String mailbox, int quota) {
		return cs.setQuota(mailbox, quota);
	}

	public IMAPByteSource uidFetchMessage(Integer uid) {
		return cs.uidFetchMessage(uid);
	}

	public Collection<Integer> uidSearch(SearchQuery sq) {
		return cs.uidSearch(sq);
	}

	public Collection<Integer> uidSearchDeleted() {
		return cs.uidSearchDeleted();
	}

	public Collection<MimeTree> uidFetchBodyStructure(Collection<Integer> uids) {
		return cs.uidFetchBodyStructure(uids);
	}

	public Collection<IMAPHeaders> uidFetchHeaders(Collection<Integer> uids, String[] headers) {
		return cs.uidFetchHeaders(uids, headers);
	}

	public Collection<Envelope> uidFetchEnvelope(Collection<Integer> uids) {
		return cs.uidFetchEnvelope(uids);
	}

	public Collection<FlagsList> uidFetchFlags(Collection<Integer> uids) {
		return cs.uidFetchFlags(uids);
	}

	public Collection<FlagsList> uidFetchFlags(String uidSet) {
		return cs.uidFetchFlags(uidSet);
	}

	public InternalDate[] uidFetchInternalDate(Collection<Integer> uids) {
		return cs.uidFetchInternalDate(uids);
	}

	public InternalDate[] uidFetchInternalDate(String uidSet) {
		return cs.uidFetchInternalDate(uidSet);
	}

	public Map<Integer, Integer> uidCopy(Collection<Integer> uids, String destMailbox) {
		return cs.uidCopy(uids, destMailbox);
	}

	public Map<Integer, Integer> uidCopy(String uidSet, String destMailbox) {
		return cs.uidCopy(uidSet, destMailbox);
	}

	public boolean uidStore(Collection<Integer> uids, FlagsList fl, boolean set) {
		return cs.uidStore(uids, fl, set);
	}

	public boolean uidStore(String uidSet, FlagsList fl, boolean set) {
		return cs.uidStore(uidSet, fl, set);
	}

	public IMAPByteSource uidFetchPart(Integer uid, String address) {
		return cs.uidFetchPart(uid, address);
	}

	public List<MailThread> uidThreads() {
		return cs.uidThreads();
	}

	public NameSpaceInfo namespace() {
		return cs.namespace();
	}

	public int getUnseen(String mailbox) {
		return cs.getUnseen(mailbox);
	}

	public int getUidnext(String mailbox) {
		return cs.getUidnext(mailbox);
	}

	public Collection<Summary> uidFetchSummary(String uidSet) {
		return cs.uidFetchSummary(uidSet);
	}

	/**
	 * Transfer a mailbox to a new backend using XFER command
	 * 
	 * @param boxName
	 * @param serverName
	 * @return
	 */
	public boolean xfer(String boxName, String serverName, String partition) {
		return cs.xfer(boxName, serverName, partition);
	}

	public boolean setMailboxAnnotation(String mailbox, String annotationId, Map<String, String> keyValues) {
		return cs.setAnnotation(mailbox, annotationId, keyValues);
	}

	public AnnotationList getAnnotation(String mailbox) {
		return cs.getAnnotation(mailbox);
	}

	public AnnotationList getAnnotation(String mailbox, String annotation) {
		return cs.getAnnotation(mailbox, annotation);
	}

	/**
	 * Tells the server our client supports given capabilities, eg. QRESYNC
	 * 
	 * @param capability
	 * @param otherCapabilities
	 * @return
	 */
	public boolean enable(String capability, String... otherCapabilities) {
		return cs.enable(capability, otherCapabilities);
	}

	public SyncStatus getUidValidity(String mailbox) {
		return cs.getUidValidity(mailbox);
	}

	public long getFirstUid() {
		return cs.getFirstUid();
	}

	/**
	 * Uses QRESYNC to fetch changes to a folder.
	 * {@link StoreClient#enable(String, String...)} must be called first with
	 * QRESYNC capability.
	 * 
	 * The folder is selected by this command.
	 * 
	 * @param mailbox
	 * @param sd
	 * @return
	 */
	public MailboxChanges sync(String mailbox, SyncData sd) {
		return cs.sync(mailbox, sd);
	}

	public boolean isExist(String mbox) {
		return !cs.listMailbox(mbox).isEmpty();
	}

	public boolean isExistAndSelectable(String mbox) {
		ListResult box = cs.listMailbox(mbox);
		if (!box.isEmpty()) {
			return box.get(0).isSelectable();
		}
		return false;
	}

	@Override
	public void close() {
		logout();
	}

	public boolean isClosed() {
		return cs.isClosed();
	}
}
