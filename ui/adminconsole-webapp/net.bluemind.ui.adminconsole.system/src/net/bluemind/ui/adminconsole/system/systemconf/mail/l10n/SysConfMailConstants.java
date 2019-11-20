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
package net.bluemind.ui.adminconsole.system.systemconf.mail.l10n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface SysConfMailConstants extends Messages {

	public static final SysConfMailConstants INST = GWT.create(SysConfMailConstants.class);

	String mail();

	String postfix();

	String mynetworks();

	String relayhost();

	String messagesizelimit();

	String invalidMessageSizeLimit();

	String detachedAttachments();

	String cyrus();

	String cyrusMaxChild();

	String cyrusRetention();

	String archive();

	String archiveEnabled();

	String archiveDays();

	String archiveSizeThreshold();

	String s3EndpointAddress();

	String s3AccessKey();

	String s3SecretKey();

	String s3BucketName();

	String archiveKindNone();

	String archiveKindCyrus();

	String archiveKindS3();
}
