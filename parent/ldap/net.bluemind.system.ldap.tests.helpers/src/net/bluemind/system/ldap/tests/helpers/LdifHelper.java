package net.bluemind.system.ldap.tests.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdifReader;

public class LdifHelper {
	public static List<Entry> loadLdif(Class<?> testClass, String ldifFileName) {
		String resourceName = ldifFileName;
		InputStream ldifIS = testClass.getResourceAsStream(resourceName);
		if (ldifIS == null) {
			return Collections.emptyList();
		}

		try (LdifReader ldifReader = new LdifReader(ldifIS)) {
			return StreamSupport.stream(ldifReader.spliterator(), false).map(ldifEntry -> ldifEntry.getEntry())
					.collect(Collectors.toList());
		} catch (IOException | LdapException e) {
		}

		return Collections.emptyList();
	}
}
