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
package net.bluemind.mailbox.identity.api;

import net.bluemind.core.api.BMApi;
import net.bluemind.mailbox.api.Mailbox;

/**
 * A mail identity that can endorsed by a user when sending an email
 * 
 */
@BMApi(version = "3")
public class Identity {

	/**
	 * the email that will be used to send the e-mail when using this identity.
	 * This e-mail must :
	 * <ul>
	 * <li>Exist</li>
	 * <li>Be associated to the {@link Mailbox} owner of the identity</li>
	 * </ul>
	 */
	public String email;

	/**
	 * the signature mime type. The signature can either be
	 * {@link SignatureFormat.PLAIN} or {@link SignatureFormat.HTML}.
	 * 
	 */
	public SignatureFormat format = SignatureFormat.HTML;

	/**
	 * the mail signature associated to this identity. The signature will be
	 * added at the end of the e-mail when using this identity. Depending on
	 * {@link #format} the signature will be interpreted as text/plain or as
	 * text/html.
	 */
	public String signature;

	/**
	 * the identity display name. The identity display name is an additional
	 * name used to describe the identity. This name will not be sent along with
	 * the email, and is only used to identity identities with same
	 * {@link #email} and same {@Link #name}.
	 */
	public String displayname;

	/**
	 * the identity name that will be used in the from header of the mail
	 * alongside with {@link #email}.
	 */
	public String name;

	/**
	 * if this identity is the default one. There is only one default identity
	 */
	public boolean isDefault = false;

	/**
	 * the folder path to store mail when sending a mail using this identity.
	 * The path is relative to {@link Mailbox}
	 */
	public String sentFolder;

}
