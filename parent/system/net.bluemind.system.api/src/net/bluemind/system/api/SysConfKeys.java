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
	/**
	 * external URL
	 */
	external_url,

	/**
	 * external protocol
	 */
	external_protocol,

	/**
	 * default domain key
	 */
	default_domain,

	eas_max_heartbeat, eas_min_heartbeat, eas_sync_unknown, message_size_limit, mynetworks, sw_password, relayhost, //
	auth_type, krb_ad_domain, krb_ad_ip, krb_domain, cas_url, cas_domain, krb_keytab, fwAdditionalIPs, dpBackupSkipTags, //
	imap_max_child, nginx_worker_connections,

	/**
	 * exchange autodiscover for split domain with exchange
	 */
	exchange_autodiscover_url,

	/**
	 * Archive kind could be: none, cyrus, s3 or scalityring
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
	 * S3 region
	 */
	sds_s3_region,

	/**
	 * SDS remove delay (backup retention), expressed in days
	 */
	sds_backup_rentention_days,

	/**
	 * SDS FileHosting settings
	 */
	/* can be null/empty, s3 or scality (ArchiveKind) */
	sds_filehosting_storetype, //

	sds_filehosting_endpoint, //

	// thoses are not used with "scality" driver
	sds_filehosting_s3_bucket, //

	sds_filehosting_s3_access_key, //

	sds_filehosting_s3_secret_key, //

	sds_filehosting_s3_region, //

	/**
	 * 
	 */
	subscription_contacts, cyrus_expunged_retention_time,

	/**
	 * HPS max session per users
	 */
	hps_max_sessions_per_user,

	/**
	 * Illustrates the release date of the initial installation version
	 */
	installation_release_date,

	/**
	 * HTTP proxy support enabled
	 */
	http_proxy_enabled,

	/**
	 * HTTP proxy host
	 */
	http_proxy_hostname,

	/**
	 * HTTP proxy port
	 */
	http_proxy_port,

	/**
	 * HTTP proxy login
	 */
	http_proxy_login,

	/**
	 * HTTP proxy password
	 */
	http_proxy_password,

	/**
	 * HTTP proxy exceptions
	 *
	 * @see {@link org.asynchttpclient.proxy.ProxyServer.isIgnoredForHost}
	 */
	http_proxy_exceptions,

	/**
	 * Allow to embed BlueMind into another web site (iFrame...)
	 */
	allow_bm_embed,

	/**
	 * Sentry endpoint. Disabled if not defined
	 */
	sentry_endpoint,

	/**
	 * Sentry (web) endpoint. Disabled if not defined
	 */
	sentry_web_endpoint,

	/**
	 * SSL certificate engine
	 */
	ssl_certif_engine,

	/**
	 * upgrade history
	 */
	upgrade_history;

	public static boolean isSysConfigKey(String key) {
		for (SysConfKeys k : SysConfKeys.values()) {
			if (k.name().equals(key)) {
				return true;
			}
		}
		return false;
	}

}
