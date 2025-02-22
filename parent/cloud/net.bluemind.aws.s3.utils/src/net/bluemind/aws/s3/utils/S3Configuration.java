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

import io.vertx.core.json.JsonObject;

public class S3Configuration {

	public String endpoint;
	public String region;
	public String accessKey;
	public String secretKey;
	public String bucket;
	// Trust all tls certificates
	public Boolean insecure;

	public S3Configuration() {
		insecure = false;
	}

	public static S3Configuration withEndpointAndBucket(String endpoint, String bucket) {
		return withEndpointBucketKeys(endpoint, bucket, "accessKey1", "verySecretKey1");
	}

	public static S3Configuration withEndpointBucketKeys(String endpoint, String bucket, String ak, String sk) {
		return withEndpointBucketKeys(endpoint, bucket, ak, sk, "", false);
	}

	public static S3Configuration withEndpointBucketKeys(String endpoint, String bucket, String ak, String sk,
			String region, Boolean insecure) {
		S3Configuration sc = new S3Configuration();
		sc.endpoint = endpoint;
		sc.bucket = bucket;
		sc.accessKey = ak;
		sc.secretKey = sk;
		sc.region = region;
		sc.insecure = insecure;
		return sc;
	}

	public JsonObject asJson() {

		return new JsonObject()//
				.put("storeType", "s3")//
				.put("endpoint", endpoint)//
				.put("region", region)//
				.put("accessKey", accessKey)//
				.put("secretKey", secretKey)//
				.put("bucket", bucket)//
				.put("insecure", insecure)//
		;
	}

	public static S3Configuration from(JsonObject configuration) {
		S3Configuration conf = new S3Configuration();
		conf.endpoint = configuration.getString("endpoint");
		conf.region = configuration.getString("region");
		conf.accessKey = configuration.getString("accessKey");
		conf.secretKey = configuration.getString("secretKey");
		conf.bucket = configuration.getString("bucket");
		conf.insecure = configuration.getBoolean("insecure", false);
		return conf;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public String getRegion() {
		return region;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public String getBucket() {
		return bucket;
	}

	public Boolean isInsecure() {
		return insecure;
	}

}
