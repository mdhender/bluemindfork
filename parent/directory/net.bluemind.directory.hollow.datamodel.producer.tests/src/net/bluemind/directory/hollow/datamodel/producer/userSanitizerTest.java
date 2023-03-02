/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.directory.hollow.datamodel.producer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.junit.Test;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Parameter;
import net.bluemind.addressbook.api.VCard.Security;
import net.bluemind.addressbook.api.VCard.Security.Key;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.hollow.datamodel.producer.DirEntrySerializer.Property;
import net.bluemind.user.api.User;

public class userSanitizerTest {
	@Test
	public void userCertificatePem() throws IOException {
		String certificate = "-----BEGIN CERTIFICATE-----\n" //
				+ "MIIFwzCCA6ugAwIBAgIUVTSFATfec/mVyk95Yu8jhQJjEhcwDQYJKoZIhvcNAQEL\n" //
				+ "BQAwcTELMAkGA1UEBhMCRlIxDzANBgNVBAgMBkZyYW5jZTERMA8GA1UEBwwIVG91\n" //
				+ "bG91c2UxETAPBgNVBAoMCEJsdWVNaW5kMQ8wDQYDVQQLDAZKVW5pdHMxGjAYBgNV\n" //
				+ "BAMMEWxkYXBhZC5pbXBvcnQudGxkMB4XDTIzMDMwMzE1MjMxOFoXDTI0MDMwMjE1\n" //
				+ "MjMxOFowcTELMAkGA1UEBhMCRlIxDzANBgNVBAgMBkZyYW5jZTERMA8GA1UEBwwI\n" //
				+ "VG91bG91c2UxETAPBgNVBAoMCEJsdWVNaW5kMQ8wDQYDVQQLDAZKVW5pdHMxGjAY\n" //
				+ "BgNVBAMMEWxkYXBhZC5pbXBvcnQudGxkMIICIjANBgkqhkiG9w0BAQEFAAOCAg8A\n" //
				+ "MIICCgKCAgEA3SqvSmLU+mnqo11RAYExZ2hT61pJ0vBjGSJ+gIOVgve2Vw8QHWgW\n" //
				+ "s3C/ff8kGiD6F3c/+qzkUpd65ZcOBMwcnPwDk2rGRbchVCrTwjePyGhWxoC7Mi/R\n" //
				+ "lpRTkc1Q84v0vZ3KthzsCXIMSgRDRnZ4cmwuj90EN+7tb0BS5HRBdeG921OeIK02\n" //
				+ "DJaO3uqRfC9mnR8Urd1hwqy0nLP7AMOOSE5264+slXPeyeQg5uTwQFkAV2vZCsjE\n" //
				+ "KS7id82UCQc2BWp+6sMlCZAFXmU1ue2rzohKbAMmfqQZLX5/rTVY4p4UO+KA8RKa\n" //
				+ "ekURt0s7iqOJ/7ANILwdmKEYxNBWuXOLJ8rINl7AI61IOY2tX79jGHacZ/h8dkn1\n" //
				+ "4RC9DKn2w1l8iFQc5tl76MDqaq4KFp6jz6BHCbCfcpziMZGFCK9dcvL+QEflck7i\n" //
				+ "AOd1Gcnj5Az19AxNa4lL+5VXMOblV6SHz2WyxxlxD9RDa9Opr44rpPUOPsfumS5J\n" //
				+ "bTk4YbwIszi2wFioN+s8EcO/lAh6ysOTcotdxMg3Bp1VBPkpf4UFJpY6rIdSyHhR\n" //
				+ "t/ymVDx7ohQhfJ1sfSqbNGWVCI+Mk5c4zBXMjPRl05J9jUuz+JOrMVfaAy71ZF6s\n" //
				+ "ZKiQLmeo3w4WEnxX6hDtBhbURjTP2AEdqfN1Y8rlvffWmumFKJyLnGcCAwEAAaNT\n" //
				+ "MFEwHQYDVR0OBBYEFBicOubB3xEds8WI8DPLrSwxm4P+MB8GA1UdIwQYMBaAFBic\n" //
				+ "OubB3xEds8WI8DPLrSwxm4P+MA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQEL\n" //
				+ "BQADggIBAAN6mJtKIW2vaRlh9Fwa6g2XIi81YjGO7jti2jotaXFuh0lkxs/IEMfQ\n" //
				+ "d+WRjjoHRJmWV30t5abW8weMFaxUDHAzA9SL5zjlKl5D99F7wC4gy82yOLnhQ1jP\n" //
				+ "5m7XrqbFEQT/AukLnrbawG1kgwVsp+w7JqdzPnWDBmd36mmUF5ebIF6dtgvN2L7P\n" //
				+ "FtYVKr/SEa55D4Gdo8i0Jle5/EmYX0IuxLyUmJiUhX03LexiuAix96TFWLl3lhFg\n" //
				+ "A3VdtPVqebHibuGHojnLh59d851TM4CB/EuLBgw1/ZM2Gx3ipccuxSZQeHUHWq6F\n" //
				+ "iGmCukw7k5S+XOGVZN5cddhV2b04IKDDIMR18uMuUAa0nLOKouDG+0ml/5dmI/tj\n" //
				+ "tYPlF5jTLQ8hG7bT3LIoXtnyXG1H7hca6YvhOtrlXxShJRp3/CKin/lzrorcp1u1\n" //
				+ "nEwukSFbJJeTVbJ/pU4fZNkfJrFfdVuthCb4TgrpYMXkHmdivWMxdoE0HwQTYxXo\n" //
				+ "DjqSVYLuFxnjBNw1JTrQn7ak62d9AKkRLC7/kw2WCrFoUptC7/kT50htFOCEcXBV\n" //
				+ "Gar9YeV1M8LWDLmOQMSjSBO2RYKmGKZHZ5XVvEcFQTyvWdOlQ32UB2v/lXHXgday\n" //
				+ "jcszlR/N8xJTZ6ylMgeLA5Jpz8dvGPdk+T0HJiN/zC5jBP8u0qBy\n" //
				+ "-----END CERTIFICATE-----";

		User user = new User();
		user.contactInfos = new VCard();
		user.contactInfos.security = Security
				.create(Key.create(certificate, Arrays.asList(Parameter.create("TYPE", "pem"))));

		Value property = new UserSerializer(ItemValue.create(Item.create(UUID.randomUUID().toString(), 0), user), null,
				null).get(Property.UserX509Certificate);

		assertNotNull(property);
		// http://www.java2s.com/example/java-api/org/bouncycastle/asn1/cms/contentinfo/getinstance-1-0.html
		assertTrue(new ASN1InputStream(new ByteArrayInputStream(property.toByteArray()))
				.readObject() instanceof ASN1Sequence);
	}

	@Test
	public void userCertificatePkcs7() throws IOException {
		String pkcs7 = "-----BEGIN PKCS7-----\n" //
				+ "MIIF8gYJKoZIhvcNAQcCoIIF4zCCBd8CAQExADALBgkqhkiG9w0BBwGgggXHMIIF\n" //
				+ "wzCCA6ugAwIBAgIUVTSFATfec/mVyk95Yu8jhQJjEhcwDQYJKoZIhvcNAQELBQAw\n" //
				+ "cTELMAkGA1UEBhMCRlIxDzANBgNVBAgMBkZyYW5jZTERMA8GA1UEBwwIVG91bG91\n" //
				+ "c2UxETAPBgNVBAoMCEJsdWVNaW5kMQ8wDQYDVQQLDAZKVW5pdHMxGjAYBgNVBAMM\n" //
				+ "EWxkYXBhZC5pbXBvcnQudGxkMB4XDTIzMDMwMzE1MjMxOFoXDTI0MDMwMjE1MjMx\n" //
				+ "OFowcTELMAkGA1UEBhMCRlIxDzANBgNVBAgMBkZyYW5jZTERMA8GA1UEBwwIVG91\n" //
				+ "bG91c2UxETAPBgNVBAoMCEJsdWVNaW5kMQ8wDQYDVQQLDAZKVW5pdHMxGjAYBgNV\n" //
				+ "BAMMEWxkYXBhZC5pbXBvcnQudGxkMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIIC\n" //
				+ "CgKCAgEA3SqvSmLU+mnqo11RAYExZ2hT61pJ0vBjGSJ+gIOVgve2Vw8QHWgWs3C/\n" //
				+ "ff8kGiD6F3c/+qzkUpd65ZcOBMwcnPwDk2rGRbchVCrTwjePyGhWxoC7Mi/RlpRT\n" //
				+ "kc1Q84v0vZ3KthzsCXIMSgRDRnZ4cmwuj90EN+7tb0BS5HRBdeG921OeIK02DJaO\n" //
				+ "3uqRfC9mnR8Urd1hwqy0nLP7AMOOSE5264+slXPeyeQg5uTwQFkAV2vZCsjEKS7i\n" //
				+ "d82UCQc2BWp+6sMlCZAFXmU1ue2rzohKbAMmfqQZLX5/rTVY4p4UO+KA8RKaekUR\n" //
				+ "t0s7iqOJ/7ANILwdmKEYxNBWuXOLJ8rINl7AI61IOY2tX79jGHacZ/h8dkn14RC9\n" //
				+ "DKn2w1l8iFQc5tl76MDqaq4KFp6jz6BHCbCfcpziMZGFCK9dcvL+QEflck7iAOd1\n" //
				+ "Gcnj5Az19AxNa4lL+5VXMOblV6SHz2WyxxlxD9RDa9Opr44rpPUOPsfumS5JbTk4\n" //
				+ "YbwIszi2wFioN+s8EcO/lAh6ysOTcotdxMg3Bp1VBPkpf4UFJpY6rIdSyHhRt/ym\n" //
				+ "VDx7ohQhfJ1sfSqbNGWVCI+Mk5c4zBXMjPRl05J9jUuz+JOrMVfaAy71ZF6sZKiQ\n" //
				+ "Lmeo3w4WEnxX6hDtBhbURjTP2AEdqfN1Y8rlvffWmumFKJyLnGcCAwEAAaNTMFEw\n" //
				+ "HQYDVR0OBBYEFBicOubB3xEds8WI8DPLrSwxm4P+MB8GA1UdIwQYMBaAFBicOubB\n" //
				+ "3xEds8WI8DPLrSwxm4P+MA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQAD\n" //
				+ "ggIBAAN6mJtKIW2vaRlh9Fwa6g2XIi81YjGO7jti2jotaXFuh0lkxs/IEMfQd+WR\n" //
				+ "jjoHRJmWV30t5abW8weMFaxUDHAzA9SL5zjlKl5D99F7wC4gy82yOLnhQ1jP5m7X\n" //
				+ "rqbFEQT/AukLnrbawG1kgwVsp+w7JqdzPnWDBmd36mmUF5ebIF6dtgvN2L7PFtYV\n" //
				+ "Kr/SEa55D4Gdo8i0Jle5/EmYX0IuxLyUmJiUhX03LexiuAix96TFWLl3lhFgA3Vd\n" //
				+ "tPVqebHibuGHojnLh59d851TM4CB/EuLBgw1/ZM2Gx3ipccuxSZQeHUHWq6FiGmC\n" //
				+ "ukw7k5S+XOGVZN5cddhV2b04IKDDIMR18uMuUAa0nLOKouDG+0ml/5dmI/tjtYPl\n" //
				+ "F5jTLQ8hG7bT3LIoXtnyXG1H7hca6YvhOtrlXxShJRp3/CKin/lzrorcp1u1nEwu\n" //
				+ "kSFbJJeTVbJ/pU4fZNkfJrFfdVuthCb4TgrpYMXkHmdivWMxdoE0HwQTYxXoDjqS\n" //
				+ "VYLuFxnjBNw1JTrQn7ak62d9AKkRLC7/kw2WCrFoUptC7/kT50htFOCEcXBVGar9\n" //
				+ "YeV1M8LWDLmOQMSjSBO2RYKmGKZHZ5XVvEcFQTyvWdOlQ32UB2v/lXHXgdayjcsz\n" //
				+ "lR/N8xJTZ6ylMgeLA5Jpz8dvGPdk+T0HJiN/zC5jBP8u0qByMQA=\n" //
				+ "-----END PKCS7-----";

		User user = new User();
		user.contactInfos = new VCard();
		user.contactInfos.security = Security
				.create(Key.create(pkcs7, Arrays.asList(Parameter.create("TYPE", "pkcs7"))));

		Value property = new UserSerializer(ItemValue.create(Item.create(UUID.randomUUID().toString(), 0), user), null,
				null).get(Property.UserX509Certificate);

		assertNotNull(property);
		System.out.println(property);
		// http://www.java2s.com/example/java-api/org/bouncycastle/asn1/cms/contentinfo/getinstance-1-0.html
		assertTrue(new ASN1InputStream(new ByteArrayInputStream(property.toByteArray()))
				.readObject() instanceof ASN1Sequence);
	}
}
