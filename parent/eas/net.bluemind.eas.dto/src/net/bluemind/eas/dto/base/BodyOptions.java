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
package net.bluemind.eas.dto.base;

import java.util.Collections;
import java.util.List;

public class BodyOptions {

	public static enum MIMESupport {
		Never(0), //
		SMimeOnly(1), //
		Always(2);

		private int xmlValue;

		private MIMESupport(int xml) {
			this.xmlValue = xml;
		}

		public static MIMESupport fromXml(String txt) {
			int fromXml = Integer.parseInt(txt);
			return MIMESupport.values()[fromXml];
		}

		public String xmlValue() {
			return Integer.toString(xmlValue);
		}

		public String toString() {
			return super.toString() + ", (xmlValue: " + xmlValue + ")";
		}
	}

	public static enum MIMETruncation {
		TRUNCATE_ALL(0, 0), //
		TRUNCATE_4096(1, 0), //
		TRUNCATE_5120(2, 0), //
		TRUNCATE_7168(3, 0), //
		TRUNCATE_10240(4, 0), //
		TRUNCATE_20480(5, 0), //
		TRUNCATE_51200(6, 0), //
		TRUNCATE_102400(7, 102400), //
		TRUNCATE_NOTHING(8, Integer.MAX_VALUE);

		private int xmlValue;
		private int charsToKeep;

		private MIMETruncation(int xml, int chars) {
			xmlValue = xml;
			charsToKeep = chars;
		}

		public static MIMETruncation fromXml(String txt) {
			int fromXml = Integer.parseInt(txt);
			return MIMETruncation.values()[fromXml];
		}

		public int charsToKeep() {
			return charsToKeep;
		}

		public String xmlValue() {
			return Integer.toString(xmlValue);
		}

		public String toString() {
			return super.toString() + ", (xmlValue: " + xmlValue + ")";
		}

	}

	public static enum DataClass {
		Tasks, Email, Calendar, Contacts, SMS, Notes;
	}

	public DataClass dataClass;
	public List<AirSyncBaseRequest.BodyPreference> bodyPrefs = Collections.emptyList();
	public List<AirSyncBaseRequest.BodyPartPreference> bodyPartPrefs = Collections.emptyList();
	public MIMESupport mimeSupport;
	public MIMETruncation mimeTruncation;

	public String toString() {
		return "MIMESupport: " + mimeSupport + ", MIMETruncation: " + mimeTruncation + ", dataClass: " + dataClass
				+ ", prefs: " + bodyPrefs;
	}

}
