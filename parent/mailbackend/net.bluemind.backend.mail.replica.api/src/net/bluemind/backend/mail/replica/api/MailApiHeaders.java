/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.api;

public final class MailApiHeaders {

	/**
	 * Store the internal id we want to assign in a header
	 */
	public static final String X_BM_INTERNAL_ID = "X-Bm-Internal-Id";

	/**
	 * The body guid of a message we want to replace
	 */
	public static final String X_BM_PREVIOUS_BODY = "X-Bm-Previous-Body";

	public static class OutlookProps {

		/**
		 * undefined, followupComplete, followupFlagged
		 */
		public static final String FLAG_STATUS = "X-Bm-Otlk-Flag-Status";

		public static final String FLAG_COLOR = "X-Bm-Otlk-Flag-Color";

		public static final String TASK_DUE_DATE = "X-Bm-Otlk-Task-Due-Date";

		public static final String TASK_ORDINAL_DATE = "X-Bm-Otlk-Task-Ordinal-Date";

		public static final String COMMON_START = "X-Bm-Otlk-Common-Start";

		public static final String REMINDER_DATE = "X-Bm-Otlk-Reminder-Date";

		public static final String REMINDER_SET = "X-Bm-Otlk-Reminder-Set";

		public static final String SHARING_CAPABILITIES = "X-Bm-Otlk-Sharing-Capabilities";

		public static final String SHARING_FLAVOR = "X-Bm-Otlk-Sharing-Flavor";

		public static final String SHARING_INITIATOR_ENTRYID = "X-Bm-Otlk-Sharing-Initiator-EntryId";

		public static final String SHARING_LOCAL_TYPE = "X-Bm-Otlk-Sharing-Local-Type";

		public static final String SHARING_REMOTE_NAME = "X-Bm-Otlk-Sharing-Remote-Name";

		public static final String SHARING_REMOTE_STORE_UID = "X-Bm-Otlk-Sharing-Remote-Store-Uid";

		public static final String SHARING_REMOTE_UID = "X-Bm-Otlk-Sharing-Remote-Uid";

		public static final String SHARING_RESPONSE_TIME = "X-Bm-Otlk-Sharing-Response-Time";

		public static final String SHARING_RESPONSE_TYPE = "X-Bm-Otlk-Sharing-Response-Type";
	}

	public static final OutlookProps Otlk = new OutlookProps();

	public static final String[] ALL = new String[] { X_BM_INTERNAL_ID, X_BM_PREVIOUS_BODY, //
			OutlookProps.FLAG_STATUS, OutlookProps.FLAG_COLOR, //
			OutlookProps.TASK_DUE_DATE, OutlookProps.TASK_ORDINAL_DATE, OutlookProps.COMMON_START, //
			OutlookProps.REMINDER_DATE, OutlookProps.REMINDER_SET, //
			OutlookProps.SHARING_CAPABILITIES, OutlookProps.SHARING_FLAVOR, OutlookProps.SHARING_INITIATOR_ENTRYID,
			OutlookProps.SHARING_LOCAL_TYPE, OutlookProps.SHARING_REMOTE_NAME, OutlookProps.SHARING_REMOTE_STORE_UID,
			OutlookProps.SHARING_REMOTE_UID, OutlookProps.SHARING_RESPONSE_TIME, OutlookProps.SHARING_RESPONSE_TYPE

	};

}
