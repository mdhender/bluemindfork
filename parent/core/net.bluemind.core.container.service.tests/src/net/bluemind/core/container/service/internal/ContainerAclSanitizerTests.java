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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.container.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.acl.ContainerAcl;
import net.bluemind.core.container.service.acl.MailshareAclSanitize;

public class ContainerAclSanitizerTests {

	@Test
	public void testHook_readToWrite() {
		String subject1 = "subject-test";
		AccessControlEntry old2 = AccessControlEntry.create(subject1, Verb.Read);
		AccessControlEntry new2 = AccessControlEntry.create(subject1, Verb.Write);
		AccessControlEntry new22 = AccessControlEntry.create(subject1, Verb.SendAs);

		List<AccessControlEntry> previous = Arrays.asList(old2);
		List<AccessControlEntry> current = Arrays.asList(new2, new22);

		ContainerAcl currentContainerAcl = new ContainerAcl(new HashSet<>(current));
		new MailshareAclSanitize(new ContainerAcl(new HashSet<>(previous)), currentContainerAcl).sanitize();

		assertEquals(2, currentContainerAcl.acl().size());
		assertTrue(currentContainerAcl.acl().stream()
				.anyMatch(acl -> acl.verb == Verb.SendAs && acl.subject.equals(subject1)));
		assertTrue(currentContainerAcl.acl().stream()
				.anyMatch(acl -> acl.verb == Verb.Write && acl.subject.equals(subject1)));
	}

	@Test
	public void testHook_writeToRead() {
		String subject1 = "subject-test";
		AccessControlEntry old2 = AccessControlEntry.create(subject1, Verb.Write);
		AccessControlEntry old22 = AccessControlEntry.create(subject1, Verb.SendAs);
		AccessControlEntry new2 = AccessControlEntry.create(subject1, Verb.Read);

		List<AccessControlEntry> previous = Arrays.asList(old2, old22);
		List<AccessControlEntry> current = Arrays.asList(new2, old22);

		ContainerAcl currentContainerAcl = new ContainerAcl(new HashSet<>(current));
		new MailshareAclSanitize(new ContainerAcl(new HashSet<>(previous)), currentContainerAcl).sanitize();

		assertEquals(1, currentContainerAcl.acl().size());
		assertTrue(currentContainerAcl.acl().stream()
				.noneMatch(acl -> acl.verb == Verb.SendAs && acl.subject.equals(subject1)));
		assertTrue(currentContainerAcl.acl().stream()
				.noneMatch(acl -> acl.verb == Verb.Write && acl.subject.equals(subject1)));
		assertTrue(currentContainerAcl.acl().stream()
				.anyMatch(acl -> acl.verb == Verb.Read && acl.subject.equals(subject1)));
	}

	@Test
	public void testHook_addSendAs() {
		// NONE -> WRITE
		String subject1 = "subject-test1";
		AccessControlEntry new1 = AccessControlEntry.create(subject1, Verb.Write);
		// READ -> WRITE
		String subject2 = "subject-test2";
		AccessControlEntry old2 = AccessControlEntry.create(subject2, Verb.Read);
		AccessControlEntry new2 = AccessControlEntry.create(subject2, Verb.Write);
		// NONE -> READ
		String subject3 = "subject-test3";
		AccessControlEntry new3 = AccessControlEntry.create(subject3, Verb.Read);

		List<AccessControlEntry> previous = Arrays.asList(old2);
		List<AccessControlEntry> current = Arrays.asList(new1, new2, new3);

		ContainerAcl currentContainerAcl = new ContainerAcl(new HashSet<>(current));
		new MailshareAclSanitize(new ContainerAcl(new HashSet<>(previous)), currentContainerAcl).sanitize();

		assertEquals(5, currentContainerAcl.acl().size());
		assertTrue(currentContainerAcl.acl().stream()
				.anyMatch(acl -> acl.verb == Verb.SendAs && acl.subject.equals(subject1)));
		assertTrue(currentContainerAcl.acl().stream()
				.anyMatch(acl -> acl.verb == Verb.SendAs && acl.subject.equals(subject2)));
		assertTrue(currentContainerAcl.acl().stream()
				.noneMatch(acl -> acl.verb == Verb.SendAs && acl.subject.equals(subject3)));
	}

	@Test
	public void testHook_removeSendAs() {
		// WRITE -> NONE
		String subject1 = "subject-test1";
		AccessControlEntry old1 = AccessControlEntry.create(subject1, Verb.Write);
		// WRITE -> READ
		String subject2 = "subject-test2";
		AccessControlEntry old2 = AccessControlEntry.create(subject2, Verb.Write);
		AccessControlEntry old22 = AccessControlEntry.create(subject2, Verb.SendAs);
		AccessControlEntry new2 = AccessControlEntry.create(subject2, Verb.Read);
		AccessControlEntry new22 = AccessControlEntry.create(subject2, Verb.SendAs);
		// READ -> NONE
		String subject3 = "subject-test3";
		AccessControlEntry old3 = AccessControlEntry.create(subject3, Verb.Read);

		List<AccessControlEntry> previous = Arrays.asList(old1, old2, old22, old3);
		List<AccessControlEntry> current = Arrays.asList(new2, new22);

		ContainerAcl currentContainerAcl = new ContainerAcl(new HashSet<>(current));
		new MailshareAclSanitize(new ContainerAcl(new HashSet<>(previous)), currentContainerAcl).sanitize();

		assertEquals(1, currentContainerAcl.acl().size());
		assertTrue(currentContainerAcl.acl().stream().noneMatch(acl -> acl.verb == Verb.SendAs));
		assertTrue(currentContainerAcl.acl().stream()
				.filter(acl -> acl.verb == Verb.Read && acl.subject.equals(subject2)).findAny().isPresent());
	}

}
