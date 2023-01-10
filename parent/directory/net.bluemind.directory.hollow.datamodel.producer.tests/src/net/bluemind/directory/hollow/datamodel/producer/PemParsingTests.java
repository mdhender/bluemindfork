/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

import net.bluemind.directory.hollow.datamodel.utils.Pem;

public class PemParsingTests {

	@Test
	public void testPem() {
		String test = "-----BEGIN CERTIFICATE-----\n"
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

		Optional<byte[]> pem = new Pem(test).toPcks7();
		assertTrue(pem.isPresent());
		pem = new Pem(test).toDer();
		assertTrue(pem.isPresent());
		assertFalse(new String(pem.get()).contains("-----BEGIN CERTIFICATE-----"));
		assertFalse(new String(pem.get()).contains("-----END CERTIFICATE-----"));
	}

	@Test
	public void testOpenSSlPem() throws Exception {
		String test = "-----BEGIN TRUSTED CERTIFICATE-----\n"
				+ "MIIFwDCCA6gCAQEwDQYJKoZIhvcNAQELBQAwga0xCzAJBgNVBAYTAkZSMRIwEAYD\n"
				+ "VQQIDAlPY2NpdGFuaWUxETAPBgNVBAcMCFRvdWxvdXNlMSQwIgYDVQQKDBtXZWJt\n"
				+ "YWlsLWRhdGEgcHJpdmF0ZSBWTSBvcmcxEDAOBgNVBAsMB1dELnRlc3QxGTAXBgNV\n"
				+ "BAMMEHdlYm1haWwtZGF0YS5sb2MxJDAiBgkqhkiG9w0BCQEWFXRlc3RAd2VibWFp\n"
				+ "bC1kYXRhLmxvYzAeFw0yMjA4MTgxNDUyNTFaFw0yNTA4MTcxNDUyNTFaMIGdMQsw\n"
				+ "CQYDVQQGEwJGUjESMBAGA1UECAwJT2NjaXRhbmllMREwDwYDVQQHDAhUb3Vsb3Vz\n"
				+ "ZTEUMBIGA1UECgwLV00tZGF0YS5sb2MxEDAOBgNVBAsMB1dNLWRhdGExGTAXBgNV\n"
				+ "BAMMEHdlYm1haWwtZGF0YS5sb2MxJDAiBgkqhkiG9w0BCQEWFXRlc3RAd2VibWFp\n"
				+ "bC1kYXRhLmxvYzCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAM7gXyNb\n"
				+ "MAQqnspP+gEzGQrej8aXCPZoA6PKFX0cngKOFL//uzT0Bl7vfd3QAIGLjdpcXaiS\n"
				+ "3xDYAY3SPwoYz5rS4cpxArXsgqXYhU2chFzV3c78vAZtpZyRJ2DfbDhIY1SfvBvN\n"
				+ "smXSSvpu9k9HmtC95yFj4azYneh1UZv2gyaQL8tZ++4k3dm/kF9nSGCvOPqMmCOP\n"
				+ "LKRhBIWzDfZ3IdGTvYuspwS8VcszUB0e4D8E0jWTOXIrJjdZP6pVimX8Fw2SzzqB\n"
				+ "Eh7gpypgXdVWfkObTLS7sxHTCCu+/lZ8mMoqtgktvPaE+c5VuB1XPbImCoj/1E2D\n"
				+ "BCM6qGNSwkYqqlgX3f26BPEwaGlGmxFhNUWrkijlWq/pS/a6cRKenNXi4jKFsByO\n"
				+ "zk3ntlpM0ppj46UifjaafDHx1WpMIqKIg/1dONm2zJLkbG4lCg2uasMgeX8JT5bS\n"
				+ "gbInsHWE1wHkZjBechSNsWimtdd6989yg2QTxeNhwlwsmKF4xoMqOpW4nkGQnIFm\n"
				+ "5Mcuf/GeQQXxr9c2zVs/Ij3K1JK12uQiqJlDIvLnPPxAAUkFpEbuP/fLLL7HWy2y\n"
				+ "fz1pHCiHxH0rJPR721CLdWcOvWHQYWWFNI319B6tQg8k+fTbSMVFghwy8Qxi7MIq\n"
				+ "KNFXbUMyHfWbzhjdD4heo7n6ZqUgJhQRgaVvAgMBAAEwDQYJKoZIhvcNAQELBQAD\n"
				+ "ggIBAAOTUpgP3NPsUjuqznCdEIwhcd4DsdOk53R0HWqdufefzCqMyg90n3og+CnP\n"
				+ "fJoQ1Gk5AMjAi9m30tkjnwHpsHGl/Of50mWvdRr9t2PcJKOw1iSvtIM+TNUxU5TT\n"
				+ "sYGIygbpecQYaQMuqKib/5zKt4CMtxzJUJQ0xbFn8XekJeKkveBRQzmr8CKX30NO\n"
				+ "mW3cuM2JRCqjubYLrHw+rDxMaJ3IeEiZmxA0zCUV1rsrCWP1igUIb0SlQJGYyEgb\n"
				+ "h3ERpPesLWRaF8G8dTqdKBW90mKQa+5qx2AjY6w1hmhHLePHVqdpGD+Q6pRlOgQr\n"
				+ "xcCbMQhEYpgYO+VzbHOW2sTD4bX6a2tpTA7AY2VNSn41mVVHm/UyEtOdBfZHec9y\n"
				+ "JOnrblUBP+UOck2tSrcXWeBQ6EXCqGAN/TjrZsvWEoDTY5a7nUBUlRC3/gAQ368V\n"
				+ "dGHZBNbfoKifxAz/5eCLII8i4HmREoWuNcDmVGRMbIhDHUDFhelAyZf+DbZDbmI9\n"
				+ "MkQo7UzsF2Mwv8fSGcGdBgSwaqHsVvvB7V0yEa2bmX3KHYa9a1k56zUBKOSWCEei\n"
				+ "fgRuaNTzLgDx85V3w0RBI5riWoc/yKkkYxNuQlrWHVJoGU9tpSuoMJD5cGbOdx4u\n"
				+ "HNaLHJxcN/ZrPmd71tkUxN0FTQ7QOMhlA0qC8Jysqvp7npbxMEUwCgYIKwYBBQUH\n"
				+ "AwSgFAYIKwYBBQUHAwIGCCsGAQUFBwMBDCFIdW1ibGUgQ29kZXIncyBFLU1haWwg\n" + "Q2VydGlmaWNhdGU=\n"
				+ "-----END TRUSTED CERTIFICATE-----";

		Optional<byte[]> pem = new Pem(test).toPcks7();
		assertTrue(pem.isPresent());
		pem = new Pem(test).toDer();
		assertTrue(pem.isPresent());
		assertFalse(new String(pem.get()).contains("-----BEGIN TRUSTED CERTIFICATE-----"));
		assertFalse(new String(pem.get()).contains("-----END TRUSTED CERTIFICATE-----"));
	}

}
