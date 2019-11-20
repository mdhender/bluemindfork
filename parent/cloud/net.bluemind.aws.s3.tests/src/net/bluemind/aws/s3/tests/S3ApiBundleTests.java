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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.aws.s3.tests;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;

import net.bluemind.dockerclient.DockerEnv;

public class S3ApiBundleTests {

	@Test
	public void testS3ApiLoads() {
		String s3Address = DockerEnv.getIp("bluemind/s3");
		System.err.println("S3 lives at " + s3Address);
		AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
		builder.setEndpointConfiguration(new EndpointConfiguration("http://" + s3Address + ":8000", ""));
		builder.setCredentials(
				new AWSStaticCredentialsProvider(new BasicAWSCredentials("accessKey1", "verySecretKey1")));
		AmazonS3 client = builder.build();
		assertNotNull(client);
		Bucket buck = client.createBucket("test" + System.currentTimeMillis());
		assertNotNull(buck);
		System.err.println("bucket: " + buck);
		client.deleteBucket(buck.getName());
	}

}
