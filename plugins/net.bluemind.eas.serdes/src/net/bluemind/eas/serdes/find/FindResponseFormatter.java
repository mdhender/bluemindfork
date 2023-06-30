/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.eas.serdes.find;

import com.google.common.base.Strings;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.find.FindResponse;
import net.bluemind.eas.dto.find.FindResponse.Response.Result.Properties;
import net.bluemind.eas.serdes.FastDateTimeFormat;
import net.bluemind.eas.serdes.IEasResponseFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;

public class FindResponseFormatter implements IEasResponseFormatter<FindResponse> {

	@Override
	public void format(IResponseBuilder builder, double protocolVersion, FindResponse response,
			Callback<Void> completion) {
		builder.start(NamespaceMapping.FIND).text("Status", response.status.xmlValue());

		builder.container(NamespaceMapping.FIND, "Response");
		builder.text(NamespaceMapping.ITEM_OPERATIONS, "Store", response.response.store);
		builder.text(NamespaceMapping.FIND, "Status", response.response.status.xmlValue());

		if (response.response.results.isEmpty()) {
			builder.endContainer(); // Response
			builder.end(completion);
			return;
		}

		response.response.results.forEach(result -> {
			builder.container(NamespaceMapping.FIND, "Result");
			builder.text(NamespaceMapping.SYNC, "Class", result.airsyncClass);
			builder.text(NamespaceMapping.SYNC, "ServerId", result.serverId);
			builder.text(NamespaceMapping.SYNC, "CollectionId", result.collectionId);

			builder.container(NamespaceMapping.FIND, "Properties");

			Properties properties = result.properties;

			if (!Strings.isNullOrEmpty(properties.subject)) {
				builder.text(NamespaceMapping.EMAIL, "Subject", properties.subject);
			}

			if (properties.dateReceived != null) {
				builder.text(NamespaceMapping.EMAIL, "DateReceived",
						FastDateTimeFormat.format(properties.dateReceived));
			}

			if (!Strings.isNullOrEmpty(properties.displayTo)) {
				builder.text(NamespaceMapping.EMAIL, "DisplayTo", properties.displayTo);
			}

			if (!Strings.isNullOrEmpty(properties.displayCc)) {
				builder.text(NamespaceMapping.FIND, "DisplayCc", properties.displayCc);
			}

			if (!Strings.isNullOrEmpty(properties.displayBcc)) {
				builder.text(NamespaceMapping.FIND, "DisplayBcc", properties.displayBcc);
			}

			if (properties.importance != null) {
				builder.text(NamespaceMapping.EMAIL, "Importance", properties.importance.xmlValue());
			}

			builder.text(NamespaceMapping.EMAIL, "Read", properties.read ? "1" : "0");

			builder.text(NamespaceMapping.EMAIL_2, "IsDraft", properties.isDraft ? "1" : "0");

			if (!Strings.isNullOrEmpty(properties.preview)) {
				builder.text(NamespaceMapping.FIND, "Preview", properties.preview);
			}

			if (properties.hasAttachments) {
				builder.text(NamespaceMapping.FIND, "HasAttachments", "1");
			}

			if (!Strings.isNullOrEmpty(properties.from)) {
				builder.text(NamespaceMapping.EMAIL, "From", properties.from);
			}

			if (!Strings.isNullOrEmpty(properties.displayName)) {
				builder.text(NamespaceMapping.GAL, "DisplayName", properties.displayName);
			}

			if (!Strings.isNullOrEmpty(properties.phone)) {
				builder.text(NamespaceMapping.GAL, "Phone", properties.phone);
			}

			if (!Strings.isNullOrEmpty(properties.office)) {
				builder.text(NamespaceMapping.GAL, "Office", properties.office);
			}

			if (!Strings.isNullOrEmpty(properties.title)) {
				builder.text(NamespaceMapping.GAL, "Title", properties.title);
			}

			if (!Strings.isNullOrEmpty(properties.company)) {
				builder.text(NamespaceMapping.GAL, "Company", properties.company);
			}

			if (!Strings.isNullOrEmpty(properties.alias)) {
				builder.text(NamespaceMapping.GAL, "Alias", properties.alias);
			}

			if (!Strings.isNullOrEmpty(properties.firstName)) {
				builder.text(NamespaceMapping.GAL, "FirstName", properties.firstName);
			}

			if (!Strings.isNullOrEmpty(properties.lastName)) {
				builder.text(NamespaceMapping.GAL, "LastName", properties.lastName);
			}

			if (!Strings.isNullOrEmpty(properties.homePhone)) {
				builder.text(NamespaceMapping.GAL, "HomePhone", properties.homePhone);
			}

			if (!Strings.isNullOrEmpty(properties.mobilePhone)) {
				builder.text(NamespaceMapping.GAL, "MobilePhone", properties.mobilePhone);
			}

			if (!Strings.isNullOrEmpty(properties.emailAddress)) {
				builder.text(NamespaceMapping.GAL, "EmailAddress", properties.emailAddress);
			}

			if (properties.picture != null) {
				builder.container(NamespaceMapping.GAL, "Picture");
				if (properties.picture.status != null) {
					builder.text("Status", properties.picture.status.xmlValue());
				}
				if (properties.picture.data != null) {
					builder.text("Data", properties.picture.data);
				}
				builder.endContainer();
			}

			builder.endContainer(); // Properties
			builder.endContainer(); // Result

		});

		builder.text(NamespaceMapping.FIND, "Range", response.response.range.min + "-" + response.response.range.max);
		builder.text(NamespaceMapping.FIND, "Total", response.response.total.toString());

		builder.endContainer(); // Response

		builder.end(completion);
	}

}
