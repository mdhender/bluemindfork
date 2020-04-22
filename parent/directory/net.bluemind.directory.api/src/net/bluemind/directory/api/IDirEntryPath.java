package net.bluemind.directory.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.directory.api.BaseDirEntry.Kind;

@BMApi(version = "3")
@Path("/directory/path")
public interface IDirEntryPath {

	@GET
	@Path("{domain}/{uid}")
	public default String getPath(@PathParam("domain") String domainUid, @PathParam("uid") String entryUid,
			@QueryParam("kind") Kind kind) {
		return path(domainUid, entryUid, kind);
	}

	static String path(String domainUid, String entryUid, Kind kind) {
		String path = "";
		switch (kind) {
		case ADDRESSBOOK:
			path = domainUid + "/addressbooks/";
			break;
		case CALENDAR:
			path = domainUid + "/calendars/";
			break;
		case EXTERNALUSER:
			path = domainUid + "/externalusers/";
			break;
		case GROUP:
			path = domainUid + "/groups/";
			break;
		case MAILSHARE:
			path = domainUid + "/mailshares/";
			break;
		case ORG_UNIT:
			path = domainUid + "/ous/";
			break;
		case RESOURCE:
			path = domainUid + "/resources/";
			break;
		case USER:
			path = domainUid + "/users/";
			break;
		case DOMAIN:
			break;
		}
		return path + entryUid;
	}

	public static String getDomain(String path) {
		return path.substring(0, path.indexOf('/'));
	}

	public static String getEntryUid(String path) {
		return path.substring(path.lastIndexOf('/') + 1);
	}

}
