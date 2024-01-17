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
package net.bluemind.eas.dto.search;

import net.bluemind.eas.dto.base.AirSyncBaseResponse;
import net.bluemind.eas.dto.calendar.CalendarResponse;
import net.bluemind.eas.dto.contact.ContactResponse;
import net.bluemind.eas.dto.documentlibrary.DocumentLibraryResponse;
import net.bluemind.eas.dto.email.EmailResponse;
import net.bluemind.eas.dto.notes.NotesResponse;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.tasks.TasksResponse;

public class SearchResult {

	public static final class SearchProperties {

		public AirSyncBaseResponse airSyncBase;
		public CalendarResponse calendar;
		public ContactResponse contact;
		public DocumentLibraryResponse documentLibrary;
		public EmailResponse email;
		public NotesResponse notes;
		public TasksResponse tasks;
		public GAL gal;
	}

	public String clazz;
	public Long longId;
	public CollectionId collectionId;
	public SearchProperties searchProperties = new SearchProperties();

}
