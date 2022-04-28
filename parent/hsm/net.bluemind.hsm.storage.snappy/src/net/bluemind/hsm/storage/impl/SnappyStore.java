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
package net.bluemind.hsm.storage.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.iq80.snappy.SnappyInputStream;
import org.iq80.snappy.SnappyOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;
import com.google.common.io.CountingInputStream;
import com.google.common.io.CountingOutputStream;

import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.utils.FBOSInput;
import net.bluemind.hsm.storage.IHSMStorage;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;

public class SnappyStore implements IHSMStorage {

	private static final Logger logger = LoggerFactory.getLogger(SnappyStore.class);

	private INodeClient nc;

	public SnappyStore() {
	}

	@Override
	public void open(INodeClient nc) {
		this.nc = nc;
	}

	@Override
	public void close() throws IOException {
		nc = null;
		logger.debug("closed (nc: {})", nc);
	}

	@Override
	public String store(String domainUid, String mailboxUid, InputStream mailContent) throws IOException {
		String hsmId = UUID.randomUUID().toString();
		String path = hashDir(domainUid, mailboxUid, hsmId);
		String filePath = path + "/" + hsmId;
		try {
			NCUtils.execOrFail(nc, "mkdir -p " + path);
			InputStream toStore = compress(mailContent);
			nc.writeFile(filePath, toStore);
			return hsmId;
		} catch (ServerFault e) {
			throw new IOException(e);
		}
	}

	@Override
	public InputStream peek(String domainUid, String mailboxUid, String hsmId, Integer maxMessageSize)
			throws IOException {
		String path = hashDir(domainUid, mailboxUid, hsmId);
		String filePath = path + "/" + hsmId;
		FileBackedOutputStream copy = new FileBackedOutputStream(32768, "snappy-store");
		SnappyInputStream sis = null;

		try {
			InputStream compressed = nc.openStream(filePath);
			sis = new SnappyInputStream(compressed);
			long bytesRead = ByteStreams.copy(sis, copy);

			if (maxMessageSize != null && bytesRead > maxMessageSize) {
				logger.error("Message {} too big {}/{}", filePath, bytesRead, maxMessageSize);
				throw new IOException("Message too big " + bytesRead);
			}

			return FBOSInput.from(copy);
		} catch (Exception e) {
			logger.error("Fail to fetch {}", filePath);
			throw new IOException(e);
		} finally {
			if (sis != null) {
				sis.close();
			}
			copy.close();
		}

	}

	@Override
	public void delete(String domainUid, String mailboxUid, String hsmId) throws IOException {
		String path = hashDir(domainUid, mailboxUid, hsmId);
		String filePath = path + "/" + hsmId;
		try {
			NCUtils.execNoOut(nc, "rm  " + filePath);
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
			throw new IOException(e);
		}

	}

	@Override
	public void copy(String domainUid, String sourceMailboxUid, String destMailboxUid, String hsmId)
			throws IOException {
		String source = hashDir(domainUid, sourceMailboxUid, hsmId);
		String dest = hashDir(domainUid, destMailboxUid, hsmId);
		String sourceFile = source + "/" + hsmId;
		String destFile = dest + "/" + hsmId;
		try {
			NCUtils.execNoOut(nc, "mkdir -p " + dest);
			NCUtils.execNoOut(nc, "cp " + sourceFile + " " + destFile);
		} catch (ServerFault e) {
			throw new IOException(e);
		}

	}

	private String hashDir(String domainUid, String mailboxUid, String hsmId) {
		StringBuilder path = new StringBuilder(256);
		path.append("/var/spool/bm-hsm/snappy/");

		// FIXME : mailshare ??
		path.append("user/");

		path.append(domainUid);
		path.append("/");
		path.append(mailboxUid);
		path.append("/");
		path.append(hsmId.charAt(0));
		path.append('/');
		path.append(hsmId.charAt(1));

		return path.toString();
	}

	private InputStream compress(InputStream mailContent) throws IOException {
		long time = System.currentTimeMillis();
		CountingInputStream orig = new CountingInputStream(mailContent);
		FileBackedOutputStream os = new FileBackedOutputStream(32768, "snappy-compress");
		CountingOutputStream compressed = new CountingOutputStream(os);
		SnappyOutputStream sos = new SnappyOutputStream(compressed);
		ByteStreams.copy(orig, sos);
		sos.close();
		compressed.close();
		os.close();
		time = System.currentTimeMillis() - time;
		if (time > 10) {
			logger.info("compressed " + orig.getCount() + " to " + compressed.getCount() + " in " + time + "ms.");
		}
		return FBOSInput.from(os);
	}

	@Override
	public int getPriority() {
		return 1;
	}
}
