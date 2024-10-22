package net.bluemind.dav.server.store;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ContainerDescriptor;

public final class BookUtils {

	private BookUtils() {
	}

	private static final Logger logger = LoggerFactory.getLogger(BookUtils.class);

	public static final ContainerDescriptor addressbook(LoggedCore lc, DavResource dr) {
		ResType rt = dr.getResType();
		Matcher m = rt.matcher(dr.getPath());
		m.find();
		String bookUid = m.group(2);
		try {
			bookUid = URLDecoder.decode(bookUid, "utf-8");
		} catch (UnsupportedEncodingException e) {
			// utf-8 is always here
			logger.error(e.getMessage(), e);
		}
		ContainerDescriptor ret = lc.getBooks().get(bookUid);
		logger.info("Lookup of '{}' => {}", bookUid, ret);
		return ret;
	}

}
