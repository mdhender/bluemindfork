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
package net.bluemind.system.api;

public enum SysConfKeys {
	eas_max_heartbeat, eas_min_heartbeat, eas_sync_unknown, message_size_limit, mynetworks, sw_password, relayhost, //
	auth_type, krb_ad_domain, krb_ad_ip, krb_domain, cas_url, cas_domain, krb_keytab, fwAdditionalIPs, dpBackupSkipTags, //
	imap_max_child, nginx_worker_connections,

	/**
	 * Archive kind could be: none, cyrus, or s3
	 */
	archive_kind, archive_days, archive_size_threshold,

	/**
	 * S3 HTTP address (https://my-storage.com:9876)
	 * 
	 */
	sds_s3_endpoint,

	/**
	 * S3 access key
	 */
	sds_s3_access_key,

	/**
	 * S3 secret key
	 */
	sds_s3_secret_key,

	/**
	 * S3 bucket name
	 */
	sds_s3_bucket,

	/**
	 * 
	 */
	subscription_contacts, cyrus_expunged_retention_time;

	public static boolean isSysConfigKey(String key) {
		for (SysConfKeys k : SysConfKeys.values()) {
			if (k.name().equals(key)) {
				return true;
			}
		}
		return false;
	}

}
