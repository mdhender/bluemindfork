package net.bluemind.addressbook.service;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Security.Key;
import net.bluemind.addressbook.service.internal.VCardValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;

public class VCardValidatorTests {

	@Before
	public void setup() {
	}

	@Test
	public void validateEmails() throws ServerFault {
		VCard card = new VCard();

		card.identification = new VCard.Identification();
		card.identification.formatedName = VCard.Identification.FormatedName.create("default",
				Arrays.<VCard.Parameter>asList());

		card.security.key = Key.create("invalid", Collections.emptyList());

		try {
			new VCardValidator(null).validate(card, Optional.empty());
		} catch (ServerFault e) {
			assertTrue(e.getCode() == ErrorCode.INVALID_PEM_CERTIFICATE);
		}

		String pem = "-----BEGIN CERTIFICATE-----\n"
				+ "MIICUTCCAfugAwIBAgIBADANBgkqhkiG9w0BAQQFADBXMQswCQYDVQQGEwJDTjEL\n"
				+ "MAkGA1UECBMCUE4xCzAJBgNVBAcTAkNOMQswCQYDVQQKEwJPTjELMAkGA1UECxMC\n"
				+ "VU4xFDASBgNVBAMTC0hlcm9uZyBZYW5nMB4XDTA1MDcxNTIxMTk0N1oXDTA1MDgx\n"
				+ "NDIxMTk0N1owVzELMAkGA1UEBhMCQ04xCzAJBgNVBAgTAlBOMQswCQYDVQQHEwJD\n"
				+ "TjELMAkGA1UEChMCT04xCzAJBgNVBAsTAlVOMRQwEgYDVQQDEwtIZXJvbmcgWWFu\n"
				+ "ZzBcMA0GCSqGSIb3DQEBAQUAA0sAMEgCQQCp5hnG7ogBhtlynpOS21cBewKE/B7j\n"
				+ "V14qeyslnr26xZUsSVko36ZnhiaO/zbMOoRcKK9vEcgMtcLFuQTWDl3RAgMBAAGj\n"
				+ "gbEwga4wHQYDVR0OBBYEFFXI70krXeQDxZgbaCQoR4jUDncEMH8GA1UdIwR4MHaA\n"
				+ "FFXI70krXeQDxZgbaCQoR4jUDncEoVukWTBXMQswCQYDVQQGEwJDTjELMAkGA1UE\n"
				+ "CBMCUE4xCzAJBgNVBAcTAkNOMQswCQYDVQQKEwJPTjELMAkGA1UECxMCVU4xFDAS\n"
				+ "BgNVBAMTC0hlcm9uZyBZYW5nggEAMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEE\n"
				+ "BQADQQA/ugzBrjjK9jcWnDVfGHlk3icNRq0oV7Ri32z/+HQX67aRfgZu7KWdI+Ju\n"
				+ "Wm7DCfrPNGVwFWUQOmsPue9rZBgO\n" + "-----END CERTIFICATE-----";

		card.security.key = Key.create(pem, Collections.emptyList());

		new VCardValidator(null).validate(card, Optional.empty());
	}

}
