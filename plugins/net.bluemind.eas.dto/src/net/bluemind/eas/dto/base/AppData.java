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

import net.bluemind.eas.dto.calendar.CalendarResponse;
import net.bluemind.eas.dto.contact.ContactResponse;
import net.bluemind.eas.dto.documentlibrary.DocumentLibraryResponse;
import net.bluemind.eas.dto.email.AttachmentResponse;
import net.bluemind.eas.dto.email.EmailResponse;
import net.bluemind.eas.dto.itemoperations.ItemOperationsRequest.Fetch.Options;
import net.bluemind.eas.dto.notes.NotesResponse;
import net.bluemind.eas.dto.tasks.TasksResponse;
import net.bluemind.eas.dto.type.ItemDataType;

public class AppData {

	public static class MetaData {
		public EmailResponse email;
		public ContactResponse contact;
		public CalendarResponse event;
		public TasksResponse task;
		public NotesResponse note;
		public DocumentLibraryResponse document;
		public AttachmentResponse attachment;
	}

	public MetaData metadata;
	public ItemDataType type;
	public LazyLoaded<BodyOptions, AirSyncBaseResponse> body;
	public Options options;

	public static AppData of(EmailResponse email, LazyLoaded<BodyOptions, AirSyncBaseResponse> bodyLoader) {
		AppData data = new AppData();
		data.metadata = new MetaData();
		data.metadata.email = email;
		data.type = ItemDataType.EMAIL;
		data.body = bodyLoader;
		return data;
	}

	public static AppData of(AttachmentResponse attach, LazyLoaded<BodyOptions, AirSyncBaseResponse> bodyLoader,
			Options options) {
		AppData data = new AppData();
		data.metadata = new MetaData();
		data.metadata.attachment = attach;
		data.type = ItemDataType.EMAIL; // FIXME ?
		data.body = bodyLoader;
		data.options = options;
		return data;
	}

	public static AppData of(ContactResponse contact) {
		AppData data = new AppData();
		data.metadata = new MetaData();
		data.metadata.contact = contact;
		data.type = ItemDataType.CONTACTS;
		data.body = LazyLoaded.NOOP;
		return data;
	}

	public static AppData of(CalendarResponse event) {
		AppData data = new AppData();
		data.metadata = new MetaData();
		data.metadata.event = event;
		data.type = ItemDataType.CALENDAR;
		data.body = LazyLoaded.NOOP;
		return data;
	}

	public static AppData of(TasksResponse tr) {
		AppData data = new AppData();
		data.metadata = new MetaData();
		data.metadata.task = tr;
		data.type = ItemDataType.TASKS;
		data.body = LazyLoaded.NOOP;
		return data;
	}

}
