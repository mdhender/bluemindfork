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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import net.bluemind.dockerclient.DockerEnv;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;

public class S3ApiBundleTests {

	@Test
	public void testS3ApiLoads() throws InterruptedException, ExecutionException, TimeoutException, URISyntaxException {
		String s3Address = DockerEnv.getIp("bluemind/s3");
		System.err.println("S3 lives at " + s3Address);

		S3AsyncClientBuilder builder = S3AsyncClient.builder();
		builder.httpClientBuilder(NettyNioAsyncHttpClient.builder());
		builder.credentialsProvider(
				StaticCredentialsProvider.create(AwsBasicCredentials.create("accessKey1", "verySecretKey1")));

		builder.region(Region.AWS_GLOBAL);

		builder.endpointOverride(new URI("http://" + s3Address + ":8000"));
		builder.serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder()//
				.pathStyleAccessEnabled(true).build());

		S3AsyncClient client = builder.build();
		String buck = "test" + System.currentTimeMillis();
		client.createBucket(CreateBucketRequest.builder().bucket(buck)
				.createBucketConfiguration(
						CreateBucketConfiguration.builder().locationConstraint(BucketLocationConstraint.EU).build())
				.build()).thenCompose(created -> {
					System.err.println(Thread.currentThread().getName() + " created " + created);
					return client.deleteBucket(DeleteBucketRequest.builder().bucket(buck).build());
				}).get(10, TimeUnit.SECONDS);
	}

}
