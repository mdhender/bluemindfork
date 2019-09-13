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

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class S3ClientFactory {

	private S3ClientFactory() {
	}

	public static AmazonS3 create(S3Configuration s3Configuration) {
		AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
		builder.setEndpointConfiguration(
				new EndpointConfiguration(s3Configuration.getEndpoint(), s3Configuration.getRegion()));
		builder.setCredentials(new AWSStaticCredentialsProvider(
				new BasicAWSCredentials(s3Configuration.getAccessKey(), s3Configuration.getSecretKey())));
		return builder.build();
	}
}
