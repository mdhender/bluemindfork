/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.backend.cyrus.integrity.check;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.node.api.ProcessHandler;
import net.bluemind.node.shared.ExecRequest;
import net.bluemind.server.api.Server;

/**
 * This class checks Cyrus filesystem for extra-directories (not matching a
 * mailbox known by BlueMind) that might create replication protocol issues as
 * we would not know what to do with this data.
 *
 */
public class CyrusFilesystemCheck {

	private static final Logger logger = LoggerFactory.getLogger(CyrusFilesystemCheck.class);

	private BmContext ctx;

	private List<ItemValue<Domain>> domains;

	public CyrusFilesystemCheck(BmContext ctx, List<ItemValue<Domain>> domains) {
		this.ctx = ctx;
		this.domains = domains;
	}

	/**
	 * Check the remote server's filesystem directory tree and locate directories
	 * that should not be there.
	 * 
	 * @param backend the cyrus server to check
	 * @return a future holding a list of directories that should not be there
	 */
	public CompletableFuture<List<String>> check(ItemValue<Server> backend) {
		List<CyrusPartition> parts = domains.stream().map(ivd -> CyrusPartition.forServerAndDomain(backend, ivd.uid))
				.collect(Collectors.toList());

		List<MailboxEntry> entries = new LinkedList<>();
		for (ItemValue<Domain> dom : domains) {
			long time = System.currentTimeMillis();
			IMailboxes mboxApi = ctx.provider().instance(IMailboxes.class, dom.uid);
			List<String> mboxesUids = mboxApi.listUids();
			List<MailboxEntry> backMboxes = Lists.partition(mboxesUids, 100).stream()//
					.flatMap(slice -> mboxApi.multipleGet(slice).stream())//
					.filter(mi -> backend.uid.equals(mi.value.dataLocation))//
					.map(mi -> new MailboxEntry(mi.value.type, mi.value.name, dom.uid))//
					.collect(Collectors.toList());
			entries.addAll(backMboxes);
			time = System.currentTimeMillis() - time;
			logger.info("Loaded {} mailbox(es) for backend {} and domain {} in {}ms.", backMboxes.size(),
					backend.value.address(), dom.uid, time);
		}

		SpoolDirectoryValidator spv = SpoolDirectoryValidator.builder()//
				.backendEntries(entries)//
				.validPartitions(parts)//
				.build();

		INodeClient node = NodeActivator.get(backend.value.address());
		try {
			deploySpoolTreeScript(node);
		} catch (IOException e) {
			CompletableFuture<List<String>> ret = new CompletableFuture<>();
			ret.completeExceptionally(e);
			return ret;
		}

		CompletableFuture<Integer> exit = new CompletableFuture<>();
		ExecRequest spoolTreeCommand = ExecRequest.anonymous("/usr/share/bm-node/spool_tree.sh");
		List<String> notVerified = new LinkedList<>();
		node.asyncExecute(spoolTreeCommand, new ProcessHandler() {

			@Override
			public void log(String spoolDirectory) {
				boolean verified = spv.verify(spoolDirectory);
				if (!verified) {
					notVerified.add(spoolDirectory);
				}
			}

			@Override
			public void completed(int exitCode) {
				exit.complete(exitCode);
			}

			@Override
			public void starting(String taskRef) {
				logger.info("Starting {}", taskRef);
			}

		});

		return exit.thenApply(code -> notVerified);

	}

	private void deploySpoolTreeScript(INodeClient node) throws IOException {
		String path = "/usr/share/bm-node/spool_tree.sh";
		byte[] scriptContent = node.read(path);
		if (scriptContent == null || scriptContent.length == 0) {
			logger.info("Deploying {} to backend {}", path, node);
			try (InputStream in = CyrusFilesystemCheck.class.getClassLoader()
					.getResourceAsStream("scripts/spool_tree.sh")) {
				node.writeFile(path, in);
				node.executeCommand("chmod +x " + path);
			}
			// deploy script
		}
	}

}
