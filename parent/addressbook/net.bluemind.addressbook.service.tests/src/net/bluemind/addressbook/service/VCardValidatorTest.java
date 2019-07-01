package net.bluemind.addressbook.service;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.addressbook.api.VCard.Organizational.Member;
import net.bluemind.addressbook.service.internal.VCardValidator;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;

public class VCardValidatorTest {

	private VCardValidator validator = spy(new VCardValidator(null));

	@Before
	public void setup() {
		VCard card = new VCard();
		card.kind = Kind.individual;
		ItemValue<VCard> iv = ItemValue.create(new Item(), card);
		doReturn(iv).when(validator).getMemberVCard(Matchers.<String>any(), Matchers.matches("auser\\d{1}"));

		VCard cardGroup = new VCard();
		cardGroup.kind = Kind.group;
		ItemValue<VCard> ivGroup = ItemValue.create(new Item(), cardGroup);
		doReturn(ivGroup).when(validator).getMemberVCard(Matchers.<String>any(), Matchers.matches("agroup\\d{1}"));
	}

	@Test
	public void validateEmails() throws ServerFault {
		VCard card = new VCard();

		card.identification = new VCard.Identification();

		card.identification.formatedName = VCard.Identification.FormatedName.create("default",
				Arrays.<VCard.Parameter>asList());

		validator.validate(card, Optional.empty());

		card.communications.emails = Arrays.asList(Email.create("ok.mail@bm.com", Arrays.<VCard.Parameter>asList()));

		validator.validate(card, Optional.empty());

		// invalid email
		String invalidEmail = "ok.failure@.com";
		card.communications.emails = Arrays.asList(Email.create(invalidEmail, Arrays.<VCard.Parameter>asList()));
		try {
			validator.validate(card, null);
			fail(invalidEmail + " mail should be invalid");
		} catch (ServerFault e) {
		}

	}

	@Test
	public void validateKind() throws ServerFault {
		VCard card = new VCard();

		card.identification = new VCard.Identification();

		card.identification.formatedName = VCard.Identification.FormatedName.create("default",
				Arrays.<VCard.Parameter>asList());

		validator.validate(card, Optional.empty());
		card.kind = null;

		try {
			validator.validate(card, null);
			fail("kind == null should throw ServerFault");
		} catch (ServerFault e) {
		}

	}

	@Test
	public void validateFormatedName() {
		VCard card = new VCard();
		card.identification = new VCard.Identification();

		try {
			validator.validate(card, null);
			fail("formatedName should not be empty");
		} catch (ServerFault e) {
		}
		card.identification.formatedName = VCard.Identification.FormatedName.create("default",
				Arrays.<VCard.Parameter>asList());

		validator.validate(card, Optional.empty());
	}

	@Test
	public void validateDlistFromSameContainer() throws ServerFault {
		String containerUid = UUID.randomUUID().toString();

		VCard card = new VCard();
		card.identification.formatedName = VCard.Identification.FormatedName.create("default",
				Arrays.<VCard.Parameter>asList());

		Member dlistIntern1 = createDList(containerUid, "agroup1");
		card.organizational.member = Arrays.asList(new Member[] { dlistIntern1 });
		validator.validate(card, Optional.ofNullable(containerUid));

		Member dlistIntern2 = createDList(containerUid, "agroup2");
		card.organizational.member = Arrays.asList(new Member[] { dlistIntern2 });
		validator.validate(card, Optional.ofNullable(containerUid));

		Member dlistExtern1 = createDList("external", "agroup3");
		card.organizational.member = Arrays.asList(new Member[] { dlistExtern1 });
		try {
			validator.validate(card, Optional.ofNullable(containerUid));
			fail("external dlist should provoke exception");
		} catch (Exception e) {
		}

		Member dlistExtern2 = createDList("external", "agroup4");
		card.organizational.member = Arrays.asList(new Member[] { dlistExtern2 });
		try {
			validator.validate(card, Optional.ofNullable(containerUid));
			fail("external dlist should provoke exception");
		} catch (Exception e) {
		}

		Member userInternal = createDList(containerUid, "auser1");
		card.organizational.member = Arrays.asList(new Member[] { userInternal });
		validator.validate(card, Optional.ofNullable(containerUid));

		Member userExternal = createDList("external", "auser2");
		card.organizational.member = Arrays.asList(new Member[] { userExternal });
		validator.validate(card, Optional.ofNullable(containerUid));
	}

	private Member createDList(String containerUid, String itemUid) {
		Member member = new Member();
		member.itemUid = itemUid;
		member.commonName = itemUid;
		member.containerUid = containerUid;
		return member;
	}

}
