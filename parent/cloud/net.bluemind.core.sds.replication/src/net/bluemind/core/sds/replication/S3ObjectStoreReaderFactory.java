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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import net.bluemind.backend.mail.replica.service.sds.IObjectStoreReader;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;

public class S3ObjectStoreReaderFactory implements IObjectStoreReader.Factory {

	private static final Logger logger = LoggerFactory.getLogger(S3ObjectStoreReader.class);

	public S3ObjectStoreReaderFactory() {
	}

	@Override
	public String handledObjectStoreKind() {
		return "s3";
	}

	@Override
	public IObjectStoreReader create(SystemConf conf) {
		String endpoint = conf.stringValue(SysConfKeys.sds_s3_endpoint.name());
		String accessKey = conf.stringValue(SysConfKeys.sds_s3_access_key.name());
		String secretKey = conf.stringValue(SysConfKeys.sds_s3_secret_key.name());
		String bucket = conf.stringValue(SysConfKeys.sds_s3_bucket.name());
		logger.info("Creating S3 reader for {} {}", endpoint, bucket);
		AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
		builder.setEndpointConfiguration(new EndpointConfiguration(endpoint, ""));
		builder.setCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)));
		AmazonS3 client = builder.build();
		return new S3ObjectStoreReader(client, bucket);
	}

}
