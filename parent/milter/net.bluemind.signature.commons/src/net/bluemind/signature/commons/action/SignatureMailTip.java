/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.signature.commons.action;

public class SignatureMailTip {

	public final String html;
	public final String text;
	public final String uid;
	public final boolean isDisclaimer;
	public final boolean usePlaceholder;
	
	public SignatureMailTip(String html, String text, String uid, boolean isDisclaimer, boolean usePlaceholder) {
		this.html = html;
		this.text = text;
		this.uid = uid;
		this.isDisclaimer = isDisclaimer;
		this.usePlaceholder = usePlaceholder;
	}
	
}
