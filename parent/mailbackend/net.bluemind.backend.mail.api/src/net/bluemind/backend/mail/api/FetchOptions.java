package net.bluemind.backend.mail.api;

import net.bluemind.core.api.BMApi;

/**
 * Optional parameters when fetching a {@link MailboxItem}
 */
@BMApi(version = "3")
public class FetchOptions {

	/**
	 * set null to fetch pristine part
	 */
	public String encoding;

	/**
	 * override the mime type of the response
	 */
	public String mime;

	/**
	 * override the charset of the response
	 */
	public String charset;

	/**
	 * set a part name (useful for download purpose)
	 */
	public String filename;

	public static FetchOptions of(String encoding, String mime, String charset, String filename) {
		FetchOptions options = new FetchOptions();
		options.encoding = encoding;
		options.mime = mime;
		options.charset = charset;
		options.filename = filename;
		return options;
	}

	public static FetchOptions decoded(String encoding) {
		FetchOptions options = new FetchOptions();
		options.encoding = encoding;
		return options;
	}

	public static FetchOptions pristine() {
		return new FetchOptions();
	}

}
