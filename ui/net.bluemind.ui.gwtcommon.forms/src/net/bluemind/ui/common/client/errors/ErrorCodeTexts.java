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
package net.bluemind.ui.common.client.errors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.ConstantsWithLookup;

public interface ErrorCodeTexts extends ConstantsWithLookup {

	public static final ErrorCodeTexts INST = GWT.create(ErrorCodeTexts.class);

	String UNKNOWN();

	String AUTHENTICATION_FAIL();

	String SQL_ERROR();

	String NOT_FOUND();

	String FORBIDDEN();

	String INVALID_QUERY();

	String INVALID_XML_RECEIVED();

	String INVALID_PARAMETER();

	String CHILD_GROUP_MUST_EXIST();

	String INVALID_ID();

	String CANT_DELETE_MYSELF();

	String DELEGATION_RESTRICTION();

	String INVALID_PASSWORD();

	String LOGIN_ALREADY_USED();

	String INVALID_LOGIN();

	String EMPTY_LASTNAME();

	String EMPTY_VACATION_MESSAGE();

	String VACATION_WITHOUT_MAILBOX();

	String FORWARDING_WITHOUT_MAILBOX();

	String FORWARDING_NOT_ALLOWED();

	String FORWARDING_INVALID_EMAIL();

	String FORWARDING_INVALID_LOCAL_COPY();

	String INVALID_MAIL_SERVER();

	String INVALID_GROUP_NAME();

	String INVALID_RESOURCE_NAME();

	String FORWARDING_TO_OWN_MAILBOX();

	String SAME_PARENT_AND_CHILD_GROUP();

	String INCLUSION_GROUP_LOOP();

	String GROUPS_PRIVACY();

	String EMAIL_ALREADY_USED();

	String INVALID_EMAIL();

	String MAIL_QUOTA_OVER_DOMAIN_LIMIT();

	String NOT_GLOBAL_ADMIN();

	String DOMAIN_NAME_ALREADY_USED();

	String INVALID_MAILSHARE_NAME();

	String OLD_PASSWORD_WRONG();

	String OLD_PASSWORD_SAME_AS_NEW();

	String IP_ADDRESS_ALREADY_USED();

	String MBOX_RENAME_FAILED();

	String INVALID_HOST_NAME();

	String EMPTY_EVENT_TITLE();

	String NO_DURATION_EVENT();

	String NO_EVENT_DATE();

	String NO_EVENT_TYPE();

	String EVENT_PRIVACY_INVALID();

	String EVENT_ERROR();

	String CALENDAR_AUTHORISATION_ERROR();

	String CONTACT_DOMAIN_FORBIDDEN();

	String INVALID_DOMAIN_NAME();

	String GROUP_NAME_ALREADY_USED();

	String INVALID_VACATION_RANGE();

	String USER_SPLIT_REQUIRES_SPLIT_DOMAIN();

	String SLAVE_RELAY_REQUIRES_SPLIT_DOMAIN();

	String SEC_GROUP_RM();

	String NOT_IN_GLOBAL_DOMAIN();

	String NO_BACKUP_SERVER_FOUND();

	String OPERATION_ALREADY_RUNNING();

	String FAILURE();

	String INVALID_AD_HOST_NAME();

	String JOB_FINISHED();

	String INVALID_LICENSE();

	String HSM_MISSING_POLICY();

	String INCOMPATIBLE_SMTP_TAGS();

	String TAG_ALREADY_EXIST();

	String PERMISSION_DENIED();

	String LOGIN_NOT_DEFINED();

}
