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
package net.bluemind.ui.common.client.forms.acl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface AclConstants extends Messages {

	public static final AclConstants INST = GWT.create(AclConstants.class);

	String publicAddress();

	String privateAddress();

	String externalSharing();

	String allowPublicAddress();

	String publicAddressDesc();

	String allowPrivateAddress();

	String privateAddressDesc();

	String resetPrivateAddress();

	String resetPublicAddress();

	String aclAccess();

	String aclSendOnBehalf();

	String aclRead();

	String aclWrite();

	String aclAdmin();

	String aclCalendarAccess();

	String aclCalendarRead();

	String aclCalendarReadOnly();

	String aclCalendarWrite();

	String aclCalendarAdmin();

	String aclDomainCalendarRead();

	String aclDomainCalendarWrite();

	String aclDomainCalendarAdmin();

	String aclAllowPublic();

	String aclPublic();

	String aclSpecific();

	String aclPlaceHolder();

	String noSharing();

	String aclEntityToManage();

	String aclResourceCalendarAccess();

	String aclResourceCalendarRead();

	String aclResourceCalendarWrite();

	String aclResourceCalendarAdmin();

	String contacts();

	String sharedBy();

	String share();

	String aclBookRead();

	String aclBookWrite();

	String aclBookAdmin();

	String aclFreebusyRead();

	String aclFreebusyAdmin();

	String aclMailSendOnBehalf();

	String aclMailRead();

	String aclMailWrite();

	String aclMailAdmin();
}
