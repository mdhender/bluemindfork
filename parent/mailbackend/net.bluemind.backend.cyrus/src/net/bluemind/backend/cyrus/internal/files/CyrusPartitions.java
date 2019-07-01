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
package net.bluemind.backend.cyrus.internal.files;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;

public class CyrusPartitions {

	private static final String ETC_CYRUS_PARTITIONS = "/etc/cyrus-partitions";
	private static final Logger logger = LoggerFactory.getLogger(CyrusPartitions.class);

	private static class Part {
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((dataPath == null) ? 0 : dataPath.hashCode());
			result = prime * result + ((metaPath == null) ? 0 : metaPath.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Part other = (Part) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (dataPath == null) {
				if (other.dataPath != null)
					return false;
			} else if (!dataPath.equals(other.dataPath))
				return false;
			if (metaPath == null) {
				if (other.metaPath != null)
					return false;
			} else if (!metaPath.equals(other.metaPath))
				return false;
			return true;
		}

		public String name;
		public String dataPath;
		public String hsmPath;
		public String metaPath;

	}

	private Set<Part> partitions = new HashSet<>();
	private INodeClient nodeClient;

	public CyrusPartitions(INodeClient nodeClient) {
		this.nodeClient = nodeClient;
	}

	public void write() throws ServerFault {
		StringWriter parts = new StringWriter();
		BufferedWriter writer = new BufferedWriter(parts);
		try {
			for (Part p : partitions) {
				writer.append("partition-" + p.name + ":" + p.dataPath + "\n");
				writer.append("metapartition-" + p.name + ":" + p.metaPath + "\n");
				writer.append("archivepartition-" + p.name + ":" + p.hsmPath + "\n");
			}
			writer.append("\n");
			writer.flush();

		} catch (IOException e) {
			throw new ServerFault(e);
		}

		logger.debug("write /etc/cyrus-partitions, content: \n {}", parts.toString());
		nodeClient.writeFile(ETC_CYRUS_PARTITIONS, new ByteArrayInputStream(parts.toString().getBytes()));
		NCUtils.exec(nodeClient, "chown cyrus:mail " + ETC_CYRUS_PARTITIONS);
		NCUtils.exec(nodeClient, "chmod 640 " + ETC_CYRUS_PARTITIONS);
	}

	public void add(String partitionName, String dataPartitionPath, String metaPartitionPath, String hsmPartitionPath) {
		Part p = new Part();
		p.name = partitionName;
		p.dataPath = dataPartitionPath;
		p.metaPath = metaPartitionPath;
		p.hsmPath = hsmPartitionPath;
		partitions.add(p);
	}

	public boolean contains(String partitionName) {
		boolean ret = false;
		for (Part p : partitions) {
			if (p.name.equals(partitionName)) {
				ret = true;
				break;
			}
		}
		return ret;
	}
}
