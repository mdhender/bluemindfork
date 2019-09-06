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
package net.bluemind.sds.proxy.store.s3;

import org.vertx.java.core.json.JsonObject;

public class S3Configuration {

	private String endpoint;
	private String region = "";
	private String accessKey = "accessKey1";
	private String secretKey = "verySecretKey1";
	private String bucket;

	private S3Configuration() {

	}

	public static S3Configuration withEndpointAndBucket(String endpoint, String bucket) {
		S3Configuration sc = new S3Configuration();
		sc.endpoint = endpoint;
		sc.bucket = bucket;
		return sc;
	}

	public JsonObject asJson() {

		return new JsonObject()//
				.putString("storeType", S3BackingStoreFactory.NAME)//
				.putString("endpoint", endpoint)//
				.putString("region", region)//
				.putString("accessKey", accessKey)//
				.putString("secretKey", secretKey)//
				.putString("bucket", bucket)//
		;
	}

	public static S3Configuration from(JsonObject configuration) {
		S3Configuration conf = new S3Configuration();
		conf.endpoint = configuration.getString("endpoint");
		conf.region = configuration.getString("region");
		conf.accessKey = configuration.getString("accessKey");
		conf.secretKey = configuration.getString("secretKey");
		conf.bucket = configuration.getString("bucket");
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

}
