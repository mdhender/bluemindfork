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
package net.bluemind.aws.s3.utils;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;

public class S3ClientFactory {

	static {
//		System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "");
//		System.setProperty(SkipMd5CheckStrategy.DISABLE_GET_OBJECT_MD5_VALIDATION_PROPERTY, "");
//		System.setProperty(SkipMd5CheckStrategy.DISABLE_PUT_OBJECT_MD5_VALIDATION_PROPERTY, "");

	}

	@SuppressWarnings("serial")
	private static class S3ConfigException extends RuntimeException {

		public S3ConfigException(Exception e) {
			super(e);
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(S3ClientFactory.class);

	private S3ClientFactory() {
	}

	public static S3AsyncClient create(S3Configuration s3Configuration) {
		try {
			S3AsyncClientBuilder builder = S3AsyncClient.builder();
			builder.httpClientBuilder(NettyNioAsyncHttpClient.builder());
			builder.credentialsProvider(StaticCredentialsProvider.create(
					AwsBasicCredentials.create(s3Configuration.getAccessKey(), s3Configuration.getSecretKey())));
			if (!Strings.isNullOrEmpty(s3Configuration.getRegion())) {
				logger.info("Setting AWS Region to '{}'", s3Configuration.getRegion());
				builder.region(Region.of(s3Configuration.getRegion()));
			} else {
				builder.region(Region.AWS_GLOBAL);
			}
			builder.endpointOverride(new URI(s3Configuration.getEndpoint()));
			builder.serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder()//
					.pathStyleAccessEnabled(true).build());

			return builder.build();
		} catch (Exception e) {
			throw new S3ConfigException(e);
		}
	}
}
