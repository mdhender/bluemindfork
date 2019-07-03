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
package net.bluemind.ui.push.client.internal;

public class HTML5Notification {
	private static final HTML5Notification instance;

	static {
		instance = new HTML5Notification();
	}

	public HTML5Notification() {

	}

	public static HTML5Notification getInstance() {
		return instance;
	}

	/**
	 * @param id
	 * @param title
	 * @param pic
	 * @param body
	 * @param url
	 */
	public native void show(String id, String title, String icon, String body, String url) /*-{
		//BM-3460
		var appVersion = $wnd.navigator.appVersion;
		if (appVersion.indexOf("Chrome/21") > -1
				|| appVersion.indexOf("Chrome/22") > -1
				|| appVersion.indexOf("Chrome/23") > -1) {
			console.log('unsupported');
		} else {

			try {
				var notification = new Notification(title, {
					body : body,
					icon : icon,
					tag : id
				});

				notification.onclick = function() {
					notification.close();
					if (url != null) {
						$wnd.location.href = url;
						$wnd.focus();
					}
				};

				setTimeout(function() {
					notification.close();
				}, '4000');

			} catch (e) {
			}
		}
	}-*/;

	/**
	 * @param id
	 * @param title
	 * @param icon
	 * @param body
	 * @param url
	 */
	public native void showAndOpenInAPopup(String id, String title, String icon, String body,
			String url) /*-{

		//BM-3460
		var appVersion = $wnd.navigator.appVersion;
		if (appVersion.indexOf("Chrome/21") > -1
				|| appVersion.indexOf("Chrome/22") > -1
				|| appVersion.indexOf("Chrome/23") > -1) {
			console.log('unsupported');
		} else {
			try {
				var notification = new Notification(title, {
					body : body,
					icon : icon,
					tag : id
				});

				notification.onclick = function() {
					notification.close();
					if (url != null) {

						var p = window
								.open(
										'',
										id,
										'height=500, width=700, top=100, left=100, toolbar=no, menubar=no, location=no, resizable=yes, scrollbars=no, status=no');
						if (p.location == 'about:blank') {
							p.location.href = url;
							p.focus();
						} else {
							p.focus();
						}
					}
				};

				setTimeout(function() {
					notification.close();
				}, '4000');

			} catch (e) {
			}
		}
	}-*/;

}
