package net.bluemind.dav.server.proto.mkcalendar;

import net.bluemind.dav.server.proto.DavQuery;
import net.bluemind.dav.server.store.DavResource;

public class MkCalQuery extends DavQuery {

	public enum Kind {

		VTODO("todolist"), VEVENT("calendar");

		public final String containerType;

		private Kind(String ct) {
			this.containerType = ct;
		}
	}

	public Kind kind;
	public String displayName;

	public MkCalQuery(DavResource dr) {
		super(dr);
	}

}
