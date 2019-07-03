package net.bluemind.dav.server.store;

import java.util.regex.Matcher;

import net.bluemind.core.container.model.ContainerDescriptor;

public final class BookUtils {

	public static final ContainerDescriptor addressbook(LoggedCore lc, DavResource dr) {
		ResType rt = dr.getResType();
		Matcher m = rt.matcher(dr.getPath());
		m.find();
		String bookUid = m.group(2);
		return lc.getBooks().get(bookUid);
	}

}
