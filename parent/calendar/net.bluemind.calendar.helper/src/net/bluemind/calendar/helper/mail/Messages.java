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
package net.bluemind.calendar.helper.mail;

import java.util.Locale;
import java.util.ResourceBundle;

public class Messages {

	private Messages() {

	}

	public static ResourceBundle getEventDetailMessages(Locale locale) {
		return ResourceBundle.getBundle("eventDetail", locale);
	}

	public static ResourceBundle getEventAlertMessages(Locale locale) {
		return ResourceBundle.getBundle("eventAlert", locale);
	}

	public static ResourceBundle getEventCreateMessages(Locale locale) {
		return ResourceBundle.getBundle("eventCreate", locale);
	}

	public static ResourceBundle getEventDeleteMessages(Locale locale) {
		return ResourceBundle.getBundle("eventDelete", locale);
	}

	public static ResourceBundle getEventUpdateMessages(Locale locale) {
		return ResourceBundle.getBundle("eventUpdate", locale);
	}

	public static ResourceBundle getEventParitipactionUpdateMessages(Locale locale) {
		return ResourceBundle.getBundle("participation", locale);
	}

	public static ResourceBundle getExceptions(Locale locale) {
		return ResourceBundle.getBundle("exceptions", locale);
	}

	public static ResourceBundle getEventOrganizationMessages(Locale locale) {
		return ResourceBundle.getBundle("eventOrganization", locale);
	}

	public static ResourceBundle getResourceEventMessages(Locale locale) {
		return ResourceBundle.getBundle("resourceEvent", locale);
	}

	public static ResourceBundle getEventCounterMessages(Locale locale) {
		return ResourceBundle.getBundle("eventCounter", locale);
	}

}
