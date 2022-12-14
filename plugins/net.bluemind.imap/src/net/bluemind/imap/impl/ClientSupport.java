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
package net.bluemind.imap.impl;

import java.io.InputStream;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.transport.socket.SocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.imap.Acl;
import net.bluemind.imap.AnnotationList;
import net.bluemind.imap.CreateMailboxResult;
import net.bluemind.imap.Envelope;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPByteSource;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.IMAPHeaders;
import net.bluemind.imap.IMAPRuntimeException;
import net.bluemind.imap.ITagProducer;
import net.bluemind.imap.InternalDate;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.MailboxChanges;
import net.bluemind.imap.NameSpaceInfo;
import net.bluemind.imap.QuotaInfo;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.Summary;
import net.bluemind.imap.SyncData;
import net.bluemind.imap.SyncStatus;
import net.bluemind.imap.TaggedResult;
import net.bluemind.imap.command.AppendCommand;
import net.bluemind.imap.command.CapabilityCommand;
import net.bluemind.imap.command.CreateCommand;
import net.bluemind.imap.command.CreateMailboxCommand;
import net.bluemind.imap.command.DeleteAclCommand;
import net.bluemind.imap.command.DeleteMailboxCommand;
import net.bluemind.imap.command.DeletedUIDCommand;
import net.bluemind.imap.command.EnableCommand;
import net.bluemind.imap.command.ExpungeCommand;
import net.bluemind.imap.command.FetchFirstUidCommand;
import net.bluemind.imap.command.GetAnnotationCommand;
import net.bluemind.imap.command.ICommand;
import net.bluemind.imap.command.ListAclCommand;
import net.bluemind.imap.command.ListCommand;
import net.bluemind.imap.command.LoginCommand;
import net.bluemind.imap.command.LsubCommand;
import net.bluemind.imap.command.NamespaceCommand;
import net.bluemind.imap.command.NoopCommand;
import net.bluemind.imap.command.QuotaRootCommand;
import net.bluemind.imap.command.RenameCommand;
import net.bluemind.imap.command.SelectCommand;
import net.bluemind.imap.command.SetAclCommand;
import net.bluemind.imap.command.SetMailboxAnnotationCommand;
import net.bluemind.imap.command.SetMessageAnnotationCommand;
import net.bluemind.imap.command.SetQuotaCommand;
import net.bluemind.imap.command.SubscribeCommand;
import net.bluemind.imap.command.SyncCommand;
import net.bluemind.imap.command.TaggedCommand;
import net.bluemind.imap.command.UIDCopyCommand;
import net.bluemind.imap.command.UIDExpungeCommand;
import net.bluemind.imap.command.UIDFetchBodyStructureCommand;
import net.bluemind.imap.command.UIDFetchEnvelopeCommand;
import net.bluemind.imap.command.UIDFetchFlagsCommand;
import net.bluemind.imap.command.UIDFetchHeadersCommand;
import net.bluemind.imap.command.UIDFetchInternalDateCommand;
import net.bluemind.imap.command.UIDFetchMessageCommand;
import net.bluemind.imap.command.UIDFetchPartCommand;
import net.bluemind.imap.command.UIDFetchSummaryCommand;
import net.bluemind.imap.command.UIDSearchCommand;
import net.bluemind.imap.command.UIDStoreCommand;
import net.bluemind.imap.command.UIDThreadCommand;
import net.bluemind.imap.command.UidValidityCommand;
import net.bluemind.imap.command.UidnextStatusCommand;
import net.bluemind.imap.command.UnSubscribeCommand;
import net.bluemind.imap.command.UnseenStatusCommand;
import net.bluemind.imap.command.XferCommand;
import net.bluemind.imap.mime.MimeTree;
import net.bluemind.imap.tls.MinigTLSFilter;

public final class ClientSupport {

	private IoSession session;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Semaphore lock;
	private final ITagProducer tagsProducer;
	private MinigTLSFilter sslFilter;
	private final ICallbackFactory icf;
	private final int commandTimeoutSecs;

	public ClientSupport(ITagProducer tp, ICallbackFactory icf, int commandTimeoutSecs) {
		this.icf = icf;
		this.lock = new Semaphore(1);
		this.tagsProducer = tp;
		this.commandTimeoutSecs = commandTimeoutSecs;
	}

	private void lock(long timeoutInSec) {
		try {
			// FIXME handle long operations using a task
			if (!lock.tryAcquire(timeoutInSec, TimeUnit.SECONDS)) {
				throw new RuntimeException("timeout ");
			}

		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException("InterruptedException !!");
		}
	}

	private void lock() {
		lock(commandTimeoutSecs);
	}

	public boolean login(String login, String password, SocketConnector connector, SocketAddress address,
			Boolean activateTLS) {
		if (session != null && session.isConnected()) {
			throw new IllegalStateException("Already connected. Disconnect first.");
		}

		try {
			lock(5); // waits 5 seconds for "* OK IMAP4rev1 server...
			final Throwable allocation = new Throwable("allocation");
			allocation.fillInStackTrace();
			ConnectFuture cf = connector.connect(address, new IoSessionInitializer<ConnectFuture>() {

				@Override
				public void initializeSession(IoSession session, ConnectFuture future) {
					IResponseCallback cb = icf.create();
					cb.setClient(ClientSupport.this);
					session.setAttribute("callback", cb);
					session.setAttribute("allocation", allocation);
					session.setAttribute("imapLogin", login);
				}
			});
			cf.awaitUninterruptibly(20, TimeUnit.SECONDS);
			if (!cf.isConnected()) {
				lock.release();
				throw new IMAPException("[" + login + "] Connection to IMAP " + address + " failed or timed-out");
			}
			session = cf.getSession();
			logger.debug("Connection established");
			if (activateTLS) {
				boolean tlsActivated = run(new StartTLSCommand());
				if (tlsActivated) {
					activateSSL();
				} else {
					logger.warn("TLS not supported by IMAP server.");
				}
			}
			logger.debug("Sending {} credentials to IMAP server.", login);
			return run(new LoginCommand(login, password));
		} catch (Exception e) {
			// if run fails, like when building the LoginCommand, we must release
			// the lock
			lock.release();
			logger.error("login error", e);
			return false;
		}
	}

	private void activateSSL() {
		try {
			sslFilter = new MinigTLSFilter();
			session.getFilterChain().addFirst("tls", sslFilter);
			logger.debug("Network traffic with IMAP server will be encrypted. ");
		} catch (Exception t) {
			logger.error("Error starting ssl", t);
		}
	}

	public void logout() {
		if (session != null) {
			session.closeNow();
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T run(ICommand<T> cmd) {
		if (logger.isDebugEnabled()) {
			logger.debug(Integer.toHexString(hashCode()) + " CMD: " + cmd.getClass().getName() + " Permits: "
					+ lock.availablePermits());
		}
		// grab lock, this one should be ok, except on first call
		// where we might wait for cyrus welcome text.
		lock();
		String sentTag = cmd.execute(session, tagsProducer, lock);
		lock(); // this one should wait until this.setResponses is called
		List<IMAPResponse> responses = (List<IMAPResponse>) session.removeAttribute("lastResponses");
		if (!session.isConnected()) {
			lock.release();
			throw new IMAPRuntimeException("Not connected to server.");
		}
		if (responses == null) {
			lock.release();
			throw new IMAPRuntimeException("null responses to " + sentTag);
		}
		try {
			String receivedTag = cmd.taggedResponseReceived(responses);
			if (!sentTag.equals(receivedTag)) {
				logger.error("TAG MISMATCH, C: {}, S: {}", sentTag, receivedTag);
				// when everything fails
				// System.exit(1);
			}
		} catch (Exception t) {
			logger.error("receiving/parsing imap response to cmd " + cmd.getClass().getSimpleName(), t);
		} finally {
			lock.release();
		}

		return cmd.getReceivedData();
	}

	/**
	 * Called by MINA on message received
	 * 
	 * @param rs
	 */
	void setResponses(List<IMAPResponse> rs) {
		if (logger.isDebugEnabled()) {
			for (IMAPResponse ir : rs) {
				logger.debug("S: " + ir.getPayload());
			}
		}

		if (session != null) {
			int len = rs.size();
			List<IMAPResponse> lr = new ArrayList<IMAPResponse>(len);
			lr.addAll(rs);
			if (logger.isDebugEnabled()) {
				IMAPResponse lastRep = lr.get(len - 1);
				logger.debug("S: {}", lastRep.getPayload());
			}
			session.setAttribute("lastResponses", lr);
		}
		lock.release();
	}

	public boolean select(String mailbox) {
		return run(new SelectCommand(mailbox));
	}

	public ListResult listSubscribed() {
		return run(new LsubCommand());
	}

	public ListResult listAll() {
		return run(new ListCommand());
	}

	public ListResult listMailbox(String mailbox) {
		return run(new ListCommand(mailbox));
	}

	public Set<String> capabilities() {
		return run(new CapabilityCommand());
	}

	public boolean noop() {
		return run(new NoopCommand());
	}

	public boolean create(String mailbox, String specialUse) {
		if (Strings.isNullOrEmpty(specialUse)) {
			return run(new CreateCommand(mailbox));
		}

		return run(new CreateCommand(mailbox, specialUse));
	}

	public CreateMailboxResult createMailbox(String mailbox, String partition) {
		return run(new CreateMailboxCommand(mailbox, partition));
	}

	public CreateMailboxResult deleteMailbox(String mailbox) {
		return run(new DeleteMailboxCommand(mailbox));
	}

	public boolean rename(String mailbox, String newMailbox) {
		return run(new RenameCommand(mailbox, newMailbox));
	}

	public boolean subscribe(String mailbox) {
		return run(new SubscribeCommand(mailbox));
	}

	public boolean unsubscribe(String mailbox) {
		return run(new UnSubscribeCommand(mailbox));
	}

	public int append(String mailbox, InputStream in, FlagsList fl) {
		return run(new AppendCommand(mailbox, in, fl));
	}

	public int append(String mailbox, InputStream in, FlagsList fl, Date delivery) {
		return run(new AppendCommand(mailbox, in, fl, delivery));
	}

	public void expunge() {
		run(new ExpungeCommand());
	}

	public void uidExpunge(Collection<Integer> uids) {
		run(new UIDExpungeCommand(uids));
	}

	public QuotaInfo quota(String mailbox) {
		return run(new QuotaRootCommand(mailbox));
	}

	public boolean setQuota(String mailbox, int quota) {
		return run(new SetQuotaCommand(mailbox, quota));
	}

	public IMAPByteSource uidFetchMessage(Integer uid) {
		return run(new UIDFetchMessageCommand(uid));
	}

	public Collection<Integer> uidSearch(SearchQuery sq) {
		return run(new UIDSearchCommand(sq));
	}

	public Collection<Integer> uidSearchDeleted() {
		return run(new DeletedUIDCommand());
	}

	public Collection<MimeTree> uidFetchBodyStructure(Collection<Integer> uid) {
		return run(new UIDFetchBodyStructureCommand(uid));
	}

	public Collection<IMAPHeaders> uidFetchHeaders(Collection<Integer> uids, String[] headers) {
		return run(new UIDFetchHeadersCommand(uids, headers));
	}

	public Collection<Envelope> uidFetchEnvelope(Collection<Integer> uids) {
		return run(new UIDFetchEnvelopeCommand(uids));
	}

	public Collection<FlagsList> uidFetchFlags(Collection<Integer> uids) {
		return run(new UIDFetchFlagsCommand(uids));
	}

	public Collection<FlagsList> uidFetchFlags(String uidSet) {
		return run(new UIDFetchFlagsCommand(uidSet));
	}

	public InternalDate[] uidFetchInternalDate(Collection<Integer> uids) {
		return run(new UIDFetchInternalDateCommand(uids));
	}

	public InternalDate[] uidFetchInternalDate(String uidSet) {
		return run(new UIDFetchInternalDateCommand(uidSet));
	}

	public Map<Integer, Integer> uidCopy(Collection<Integer> uids, String destMailbox) {
		return run(new UIDCopyCommand(uids, destMailbox));
	}

	public Map<Integer, Integer> uidCopy(String uidSet, String destMailbox) {
		return run(new UIDCopyCommand(uidSet, destMailbox));
	}

	public boolean uidStore(Collection<Integer> uids, FlagsList fl, boolean set) {
		return run(new UIDStoreCommand(uids, fl, set));
	}

	public boolean uidStore(String uidSet, FlagsList fl, boolean set) {
		return run(new UIDStoreCommand(uidSet, fl, set));
	}

	public IMAPByteSource uidFetchPart(Integer uid, String address) {
		return run(new UIDFetchPartCommand(uid, address));
	}

	public List<MailThread> uidThreads() {
		// UID THREAD REFERENCES UTF-8 NOT DELETED
		return run(new UIDThreadCommand());
	}

	public NameSpaceInfo namespace() {
		return run(new NamespaceCommand());
	}

	/**
	 * Sets an IMAP Acl on a mailbox
	 * 
	 * @param mailbox  user/toto@willow.vmw
	 * @param consumer admin0
	 * @param acl      all
	 * @return true if SETACL succeeds
	 */
	public boolean setAcl(String mailbox, String consumer, Acl acl) {
		return run(new SetAclCommand(mailbox, consumer, acl));
	}

	public boolean deleteAcl(String mailbox, String consumer) {
		return run(new DeleteAclCommand(mailbox, consumer));
	}

	public Map<String, Acl> listAcl(String mailbox) {
		return run(new ListAclCommand(mailbox));
	}

	public int getUnseen(String mailbox) {
		return run(new UnseenStatusCommand(mailbox));
	}

	public int getUidnext(String mailbox) {
		return run(new UidnextStatusCommand(mailbox));
	}

	public boolean xfer(String boxName, String serverName, String partition) {
		return run(new XferCommand(boxName, serverName, partition));
	}

	public Collection<Summary> uidFetchSummary(String uidSet) {
		return run(new UIDFetchSummaryCommand(uidSet));
	}

	public boolean setAnnotation(String mbox, String annotation, Map<String, String> kv) {
		return run(new SetMailboxAnnotationCommand(mbox, annotation, kv));
	}

	public boolean setMessageAnnotation(int uid, String annotation, String value) {
		return run(new SetMessageAnnotationCommand(uid, annotation, value));
	}

	public AnnotationList getAnnotation(String mailbox) {
		return run(new GetAnnotationCommand(mailbox));
	}

	public AnnotationList getAnnotation(String mailbox, String annotation) {
		return run(new GetAnnotationCommand(mailbox, annotation));
	}

	public TaggedResult tagged(String imapCommand) {
		return run(new TaggedCommand(imapCommand));
	}

	public boolean enable(String capability, String... otherCapabilities) {
		return run(new EnableCommand(capability, otherCapabilities));
	}

	public SyncStatus getUidValidity(String mailbox) {
		return run(new UidValidityCommand(mailbox));
	}

	public MailboxChanges sync(String mailbox, SyncData sd) {
		return run(new SyncCommand(mailbox, sd));
	}

	public long getFirstUid() {
		return run(new FetchFirstUidCommand());
	}

	/**
	 * @param cause
	 */
	public void throwError(IMAPException cause) {
		if (lock.availablePermits() == 0) {
			IMAPResponse ir = new IMAPResponse();
			ir.setTag("BMTAG");
			ir.setStatus("BAD");
			ir.setPayload("BMTAG BAD " + cause.getMessage());
			setResponses(Arrays.asList(ir));
		}
	}

	public boolean isClosed() {
		return session == null || !session.isConnected();
	}

}
