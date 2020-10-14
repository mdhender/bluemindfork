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
package net.bluemind.core.api.fault;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public enum ErrorCode {

	/**
	 * GENERAL PURPOSE ERRORS
	 */

	UNKNOWN,

	PERMISSION_DENIED,

	FAILURE,

	INVALID_ID,

	INVALID_PARAMETER,

	AUTHENTICATION_FAIL,

	SQL_ERROR,

	NOT_FOUND,

	ALREADY_EXISTS,

	FORBIDDEN,

	ENTITY_TOO_LARGE,

	/**
	 * SPECIFIC ERROR. TODO: Should be removed and replace par general purpose
	 * errors ?
	 */

	/**
	 * Invalid query
	 */
	INVALID_QUERY,

	/**
	 * XML received from client is either invalid or not as expected
	 */
	INVALID_XML_RECEIVED,

	/**
	 * Child group must exist
	 */
	CHILD_GROUP_MUST_EXIST,

	/**
	 * BJR(4)
	 */
	CANT_DELETE_MYSELF,

	/**
	 * BJR(5)
	 */
	DELEGATION_RESTRICTION,

	/**
	 * BJR(6)
	 */
	INVALID_PASSWORD,

	/**
	 * BJR(7)
	 */
	LOGIN_ALREADY_USED,

	/**
	 * BJR(8)
	 */
	INVALID_LOGIN,

	/**
	 * BJR(9)
	 */
	EMPTY_LASTNAME,

	/**
	 * BJR(10)
	 */
	EMPTY_VACATION_MESSAGE,

	/**
	 * BJR(11)
	 */
	VACATION_WITHOUT_MAILBOX,

	/**
	 * BJR(12)
	 */
	FORWARDING_WITHOUT_MAILBOX,

	/**
	 * BJR(14)
	 */
	FORWARDING_INVALID_EMAIL,

	/**
	 * BJR(16)
	 */
	INVALID_MAIL_SERVER,

	/**
	 * BJR(17)
	 */
	INVALID_GROUP_NAME,

	/**
	 * BJR(19)
	 */
	FORWARDING_TO_OWN_MAILBOX,

	/**
	 * BJR(20)
	 */
	SAME_PARENT_AND_CHILD_GROUP,

	/**
	 * BJR(21)
	 */
	INCLUSION_GROUP_LOOP,

	/**
	 * BJR(23)
	 */
	EMAIL_ALREADY_USED,

	/**
	 * BJR(24)
	 */
	INVALID_EMAIL,

	/**
	 * BJR(25)
	 */
	MAIL_QUOTA_OVER_DOMAIN_LIMIT,

	/**
	 * BJR(29)
	 */
	NOT_GLOBAL_ADMIN,

	/**
	 * BJR(30)
	 */
	DOMAIN_NAME_ALREADY_USED,

	/**
	 * BJR(31)
	 */
	INVALID_MAILSHARE_NAME,

	/**
	 * BJR(74)
	 */
	INVALID_RESOURCE_NAME,

	/**
	 * BJR(33)
	 */
	OLD_PASSWORD_WRONG,

	/**
	 * BJR(34)
	 */
	OLD_PASSWORD_SAME_AS_NEW,

	/**
	 * BJR(38)
	 */
	IP_ADDRESS_ALREADY_USED,

	/**
	 * Failure on Cyrus rename command
	 */
	MBOX_RENAME_FAILED,

	/**
	 * BJR(40)
	 */
	INVALID_HOST_NAME,

	/**
	 * BJR(45)
	 */
	EMPTY_EVENT_TITLE,

	/**
	 * BJR(46)
	 */
	NO_DURATION_EVENT,

	/**
	 * BJR(46)
	 */
	NO_EVENT_DATE,

	/**
	 * BJR(46)
	 */
	NO_EVENT_TYPE,

	/**
	 * BJR(46)
	 */
	EVENT_PRIVACY_INVALID,

	/**
	 * BJR(xx) Autres erreurs improbables (default values not null set to null)
	 */
	EVENT_ERROR,

	/**
	 * BJR(50)
	 */
	CALENDAR_AUTHORISATION_ERROR,

	/**
	 * BJR(51)
	 */
	CONTACT_DOMAIN_FORBIDDEN,

	/**
	 * BJR(53)
	 */
	EVENT_ENDREPEAT_PRIOR_TO_EVENT_DATE,

	/**
	 * 
	 */
	INVALID_DOMAIN_NAME,

	/**
	 * 
	 */
	GROUP_NAME_ALREADY_USED,

	/**
	 * 
	 */
	INVALID_VACATION_RANGE,

	/**
	 * 
	 */
	EMPTY_DLIST_LABEL,

	/**
	 * 
	 */
	EMPTY_DLIST_FOLDER,

	/**
	 *
	 */
	DLIST_LOOP,

	/**
	 *
	 */
	CANT_UPDATE_ACL_IMPLICIT_RIGHTS,

	/**
	 * 
	 */
	USER_SPLIT_REQUIRES_SPLIT_DOMAIN,

	/**
	 * 
	 */
	SLAVE_RELAY_REQUIRES_SPLIT_DOMAIN,

	/**
	 * 
	 */
	INVALID_USER_LDAP_FILTER,

	/**
	 * 
	 */
	SEC_GROUP_RM,

	/**
	 * 
	 */
	NOT_IN_GLOBAL_DOMAIN,

	/**
	 * 
	 */
	NO_BACKUP_SERVER_FOUND,

	/**
	 * 
	 */
	OPERATION_ALREADY_RUNNING,

	/**
	 * 
	 */
	INVALID_AD_HOST_NAME,

	/**
	 * Sent when the live logs of a finished jobs are requested
	 */
	JOB_FINISHED,

	/**
	 * 
	 */
	INVALID_LICENSE,

	/**
	 * No policy is defined on the queried entity
	 */
	HSM_MISSING_POLICY,

	/**
	 * Thrown when trying to make an internal server a relay, or the other way
	 * around.
	 */
	INCOMPATIBLE_SMTP_TAGS,

	/**
	 * 
	 */
	INVALID_MAILBOX_NAME,

	/**
	 * 
	 */
	TAG_ALREADY_EXIST,

	/**
	 * Thrown when linkin a mailshare to a group is not allowed
	 */
	INVALID_LINKED_MAILSHARE,

	/**
	 * A group is linked to a mailshare
	 */
	MAILSHARE_GROUP_LINKED,

	/**
	 * 
	 */
	SERVER_NOT_FOUND, DEPRECATED,

	/**
	 * 
	 */
	INVALID_GROUP_MEMBER, EMPTY_EVENT_ATTACHMENT_VALUE,

	/**
	 * 
	 */
	TIMEOUT,

	/**
	 * sent when listing ids in a container with the wrong container version
	 */
	VERSION_HAS_CHANGED;

}
