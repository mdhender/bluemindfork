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
package net.bluemind.directory.hollow.datamodel.producer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.hollow.datamodel.producer.Value.BooleanValue;
import net.bluemind.directory.hollow.datamodel.producer.Value.ByteArrayValue;
import net.bluemind.directory.hollow.datamodel.producer.Value.DateValue;
import net.bluemind.directory.hollow.datamodel.producer.Value.ListValue;
import net.bluemind.directory.hollow.datamodel.producer.Value.NullValue;
import net.bluemind.directory.hollow.datamodel.producer.Value.StringValue;
import net.bluemind.directory.hollow.datamodel.utils.JPEGThumbnail;
import net.bluemind.externaluser.api.ExternalUser;
import net.bluemind.externaluser.api.IExternalUser;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.server.api.IServer;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public abstract class DirEntrySerializer {

	public enum Property {
		Email, SmtpAddress, DisplayName, Account, Surname, GivenName, AddressBookProxyAddresses, OfficeLocation,
		DisplayType, ObjectType, SendRichInfo, BusinessTelephoneNumber, StreetAddress, Locality, StateOrProvince,
		PostalCode, Country, Title, CompanyName, Assistant, DepartmentName, AddressBookTargetAddress,
		HomeTelephoneNumber, Business2TelephoneNumbers, PrimaryFaxNumber, MobileTelephoneNumber,
		AssistantTelephoneNumber, PagerTelephoneNumber, Comment, UserCertificate, UserX509Certificate,
		AddressBookX509Certificate, AddressBookHomeMessageDatabaseAscii, AddressBookDisplayNamePrintableAscii,
		ThumbnailPhoto, postOfficeBox, AddressBookManagerDistinguishedName, Kind, DataLocation, Created, Updated, Hidden

	}

	protected final ItemValue<DirEntry> dirEntry;
	protected final String domainUid;

	protected DirEntrySerializer(ItemValue<DirEntry> dirEntry, String domainUid) {
		this.dirEntry = dirEntry;
		this.domainUid = domainUid;
	}

	public static Optional<DirEntrySerializer> get(String domainUid, ItemValue<DirEntry> dirEntry) {
		DirEntrySerializer ret = null;
		switch (dirEntry.value.kind) {
		case USER:
			ItemValue<User> user = provider().instance(IUser.class, domainUid).getComplete(dirEntry.uid);
			if (user != null) {
				ret = new UserSerializer(user, dirEntry, domainUid);
			}
			break;
		case GROUP:
			ItemValue<Group> group = provider().instance(IGroup.class, domainUid).getComplete(dirEntry.uid);
			if (group != null) {
				ret = new GroupSerializer(group, dirEntry, domainUid);
			}
			break;
		case RESOURCE:
			ResourceDescriptor resource = provider().instance(IResources.class, domainUid).get(dirEntry.uid);
			if (resource != null) {
				ret = new ResourceSerializer(resource, dirEntry, domainUid);
			}
			break;
		case MAILSHARE:
			ItemValue<Mailshare> mailshare = provider().instance(IMailshare.class, domainUid).getComplete(dirEntry.uid);
			if (mailshare != null) {
				ret = new MailshareSerializer(mailshare, dirEntry, domainUid);
			}
			break;
		case EXTERNALUSER:
			ItemValue<ExternalUser> externalUser = provider().instance(IExternalUser.class, domainUid)
					.getComplete(dirEntry.uid);
			if (externalUser != null) {
				ItemValue<Mailbox> mbox = provider().instance(IMailboxes.class, domainUid)
						.byEmail(dirEntry.value.email);
				if (mbox == null) {
					ret = new ExternalUserSerializer(externalUser, dirEntry, domainUid);
				}
			}
			break;
		default:
			throw new IllegalArgumentException("DirEntry kind " + dirEntry.value.kind + " not supported");
		}
		return Optional.ofNullable(ret);
	}

	private static IServiceProvider provider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	public Value get(Property property) {
		switch (property) {
		case Created:
			return new DateValue(dirEntry.created);
		case Updated:
			return dirEntry.updated != null ? new DateValue(dirEntry.updated) : new NullValue();
		case Email:
			return getEmailAddress();
		case AddressBookManagerDistinguishedName:
			return new StringValue("/");
		case ThumbnailPhoto:
			return getPhoto();
		case Kind:
			return new StringValue(dirEntry.value.kind.name());
		case DataLocation:
			return new ListValue(getDataLocation(dirEntry));
		case Hidden:
			return new BooleanValue(dirEntry.value.hidden);
		default:
			return Value.NULL;
		}
	}

	private List<?> getDataLocation(ItemValue<DirEntry> dirEntry) {
		String server = dirEntry.value.dataLocation;
		if (null != server) {
			String address = provider().instance(IServer.class, "default").getComplete(server).value.address();
			return Arrays.asList(address, server);
		}
		return Collections.emptyList();
	}

	private Value getPhoto() {
		try {
			IDirectory directory = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IDirectory.class, domainUid);
			byte[] pngPhoto = directory.getEntryPhoto(dirEntry.value.entryUid);
			if (null != pngPhoto) {
				return new ByteArrayValue(JPEGThumbnail.scaleImage(pngPhoto, 120, 120));
			}
		} catch (Exception e) {
			// no pic
		}
		return Value.NULL;

	}

	private Value getEmailAddress() {
		return new StringValue(dirEntry.value.email);
	}

}
