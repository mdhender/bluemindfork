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
package net.bluemind.core.sds.replication;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.common.io.ByteStreams;
import com.netflix.spectator.api.Registry;

import net.bluemind.backend.mail.replica.service.sds.IObjectStoreReader;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.metrics.registry.IdFactory;

public class S3ObjectStoreReader implements IObjectStoreReader {

	private static final Logger logger = LoggerFactory.getLogger(S3ObjectStoreReader.class);
	private final AmazonS3 client;
	private final String bucket;
	private Registry registry;
	private IdFactory idFactory;

	public S3ObjectStoreReader(AmazonS3 client, String bucket, Registry registry, IdFactory idFactory) {
		this.registry = registry;
		this.idFactory = idFactory;

		this.client = client;
		this.bucket = bucket;
	}

	@Override
	public boolean exist(String guid) {
		return client.doesObjectExist(bucket, guid);
	}

	@Override
	public Path read(String guid) {
		logger.info("Reader {}/{}", bucket, guid);
		Path target = null;
		try {
			target = Files.createTempFile(guid, ".s3");
		} catch (IOException e1) {
			throw new ServerFault(e1);
		}
		try (S3Object s3object = client.getObject(bucket, guid);
				S3ObjectInputStream stream = s3object.getObjectContent();
				OutputStream out = java.nio.file.Files.newOutputStream(target)) {
			ByteStreams.copy(stream, out);

			registry.distributionSummary(idFactory.name("read")).record(target.toFile().length());
		} catch (Exception e) {
			throw new ServerFault(e);
		}
		return target;
	}

}
