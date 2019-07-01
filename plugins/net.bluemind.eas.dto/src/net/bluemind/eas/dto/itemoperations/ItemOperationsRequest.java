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
package net.bluemind.eas.dto.itemoperations;

import java.util.List;

import org.w3c.dom.Element;

import net.bluemind.eas.dto.base.BodyOptions;

public class ItemOperationsRequest {

	public ResponseStyle style = ResponseStyle.Inline;
	public boolean gzip = false;

	public List<ItemOperation> itemOperations;

	public static interface ItemOperation {

	}

	public static class EmptyFolderContents implements ItemOperation {
		public String collectionId;
		public Options options;

		public static class Options {
			public boolean deleteSubFolders;
		}

	}

	public static class Fetch implements ItemOperation {
		public String store;
		public String serverId;
		public String collectionId;
		public String linkId;
		public String longId;
		public String fileReference;
		public Options options = new Options();

		public static class Options {
			// schema
			public Element schema;
			// range
			public Range range;

			// username
			public String userName;
			// password
			public String password;

			public BodyOptions bodyOptions = new BodyOptions();

			//
			// rightsmanagement:RightsManagementSupport

			public static class Range {
				public int start;
				public int end;
			}

		}

		// "rightsmanagement:RemoveRightsManagementProtection"minOccurs="0"maxOccurs="1"/></xs:all></xs:complexType>
	}

	public static class Move implements ItemOperation {
		public String conversationId;
		public String dstFldId;
		public Options options = new Options();

		public static class Options {
			public boolean moveAlways;

		}
	}
}
