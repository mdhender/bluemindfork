/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.cli.inject.node;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.backend.cyrus.partitions.CyrusFileSystemPathHelper;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.cyrus.partitions.MailboxDescriptor;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.inject.common.MailExchangeInjector;
import net.bluemind.cli.inject.common.MinimalMessageProducer;
import net.bluemind.cli.inject.common.TargetMailbox;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.FileDescription;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.node.api.ProcessHandler;
import net.bluemind.node.api.ProcessHandler.NoOutBlockingHandler;
import net.bluemind.node.shared.ExecRequest;
import net.bluemind.server.api.Server;

public class NodeInjector extends MailExchangeInjector {

	private static final LongAdder total = new LongAdder();
	private static final AtomicLong cycle = new AtomicLong();

	public static class NodeTargetMailbox extends TargetMailbox {

		private CyrusPartition partition;
		private INodeClient nc;
		private IServiceProvider sp;
		private CliContext ctx;
		private MailboxDescriptor md;
		private AuthUser curUser;
		private String fromIndex;

		public NodeTargetMailbox(CliContext ctx, String email, String sid) {
			super(email, sid);
			this.ctx = ctx;
			this.sp = ctx.api(sid);
		}

		@Override
		public boolean prepare() {
			try {
				curUser = sp.instance(IAuthentication.class).getCurrentUser();
				ItemValue<Server> srv = Topology.get().datalocation(curUser.value.dataLocation);
				nc = NodeActivator.get(srv.value.address());
				partition = CyrusPartition.forServerAndDomain(curUser.value.dataLocation, curUser.domainUid);
				this.md = new MailboxDescriptor();
				md.type = Type.user;
				md.utf7FolderPath = "INBOX";
				md.mailboxName = curUser.value.login;
				fromIndex = CyrusFileSystemPathHelper.getMetaFileSystemPath(curUser.domainUid, md, partition,
						"cyrus.index");
				return true;
			} catch (ServerFault sf) {
				ctx.error(sf.getMessage() + " for " + email);
				return false;
			}
		}

		@Override
		public void exchange(TargetMailbox from, byte[] emlContent) {
			NodeTargetMailbox ntm = (NodeTargetMailbox) from;
			List<FileDescription> exist = nc.listFiles(ntm.fromIndex);
			FileDescription fromFd = exist.get(0);
			total.add(fromFd.getSize());
			byte[] fromBytes = nc.read(ntm.fromIndex);
			String fn = "crap." + System.nanoTime();

			// list a file that does not exist
			nc.listFiles("/" + fn);

			long curCycle = cycle.getAndIncrement();
			String cmd = "find /var/log/bm-node/ -type f";
			if (curCycle % 1000 == 0) {
				cmd = "sleep 10";
			}

			NoOutBlockingHandler handler = new ProcessHandler.NoOutBlockingHandler();
			nc.asyncExecute(ExecRequest.anonymousWithoutOutput(cmd), handler);
			String toCopy = CyrusFileSystemPathHelper.getMetaFileSystemPath(curUser.domainUid, md, partition, fn);

			nc.writeFile(toCopy, new ByteBufInputStream(Unpooled.wrappedBuffer(fromBytes)));
			nc.deleteFile(toCopy);
			handler.get(1, TimeUnit.MINUTES);
		}

	}

	private final CliContext ctx;

	public NodeInjector(CliContext ctx, String domainUid) {
		super(ctx.adminApi(), domainUid, (email, sid) -> new NodeTargetMailbox(ctx, email, sid),
				new MinimalMessageProducer());
		this.ctx = ctx;
	}

	@Override
	protected void end() {
		ctx.info("Transferred {} byte(s)", total.sum());
	}

}
