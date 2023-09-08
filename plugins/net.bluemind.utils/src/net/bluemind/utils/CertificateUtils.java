package net.bluemind.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.X509TrustedCertificateBlock;

import net.bluemind.core.api.fault.ServerFault;

/**
 * Crappy utils class to read certificate/ca/pk (we should use BountyCastle but
 * it is not available in common plugin)
 *
 */
public class CertificateUtils {

	private static final String PK_RSA_SIGNATURE_BEGIN = "-----BEGIN RSA PRIVATE KEY-----";
	private static final String PK_RSA_SIGNATURE_END = "-----END RSA PRIVATE KEY-----";

	private static final String PK_SIGNATURE_BEGIN = "-----BEGIN PRIVATE KEY-----";
	private static final String PK_SIGNATURE_END = "-----END PRIVATE KEY-----";

	public static final String X509 = "X.509";

	private CertificateUtils() {
	}

	public static X509Certificate getCertificate(byte[] certFile) throws ServerFault {
		try {
			PEMParser parser = new PEMParser(new StringReader(new String(certFile)));
			Object obj;
			while ((obj = parser.readObject()) != null) {
				if (obj instanceof X509CertificateHolder) {
					X509CertificateHolder certHolder = (X509CertificateHolder) obj;
					return getCertificateByHolder(certHolder);
				} else if (obj instanceof X509TrustedCertificateBlock) {
					X509TrustedCertificateBlock trustedBlock = (X509TrustedCertificateBlock) obj;
					return getCertificateByHolder(trustedBlock.getCertificateHolder());
				}
			}
			throw new IllegalArgumentException("no pem cert found");
		} catch (CertificateException | IOException e) {
			throw new ServerFault(e);
		}
	}

	private static X509Certificate getCertificateByHolder(X509CertificateHolder certHolder)
			throws CertificateException {
		return new JcaX509CertificateConverter().getCertificate(certHolder);
	}

	public static String getCertCN(byte[] certFile) throws ServerFault {
		try {
			X509Certificate ca = getCertificate(certFile);
			LdapName ldapDN = new LdapName(ca.getSubjectX500Principal().getName());
			return ldapDN.getRdn(ldapDN.size() - 1).getValue().toString();
		} catch (InvalidNameException e) {
			throw new ServerFault(e);
		}
	}

	public static byte[] readCert(byte[] certFile) {
		String cert = new String(certFile);
		int begin = cert.indexOf(PK_SIGNATURE_BEGIN);
		int end = cert.indexOf(PK_SIGNATURE_END) + PK_SIGNATURE_END.length();
		if (begin < 0) {
			begin = cert.indexOf(PK_RSA_SIGNATURE_BEGIN);
			end = cert.indexOf(PK_RSA_SIGNATURE_END) + PK_RSA_SIGNATURE_END.length();

		}

		if (begin < 0) {
			return certFile;
		}

		StringBuffer fcert = new StringBuffer();
		fcert.append(cert.substring(0, begin));
		if (end < cert.length()) {
			end++;
		}
		fcert.append(cert.substring(end));
		return fcert.toString().getBytes();
	}

	public static byte[] readPrivateKey(byte[] certFile) {
		String cert = new String(certFile);
		int begin = cert.indexOf(PK_SIGNATURE_BEGIN);
		int end = cert.indexOf(PK_SIGNATURE_END) + PK_SIGNATURE_END.length();
		if (begin < 0) {
			begin = cert.indexOf(PK_RSA_SIGNATURE_BEGIN);
			end = cert.indexOf(PK_RSA_SIGNATURE_END) + PK_RSA_SIGNATURE_END.length();

		}

		if (begin < 0) {
			return null;
		}
		return cert.substring(begin, end).getBytes();
	}

	public static Certificate generateX509Certificate(byte[] certFile) throws CertificateException {
		return CertificateFactory.getInstance(X509).generateCertificate(new ByteArrayInputStream(certFile));
	}

	public static Collection<? extends Certificate> generateX509Certificates(byte[] certFile)
			throws CertificateException {
		return CertificateFactory.getInstance(X509).generateCertificates(new ByteArrayInputStream(certFile));
	}

	/**
	 * Convert PKCS7 PEM to DER
	 * 
	 * @param pkcs7
	 * @return PKCS7 DER or empty optional if invalid
	 */
	public static Optional<byte[]> pkcs7PemToDer(String pkcs7) {
		try {
			Object obj;
			PEMParser pemParser = new PEMParser(new StringReader(pkcs7));
			while ((obj = pemParser.readObject()) != null) {
				if (obj instanceof ContentInfo) {
					return Optional.ofNullable(((ContentInfo) obj).getEncoded());
				}
			}
		} catch (IOException e) {
		}

		return Optional.empty();
	}

	public static CRL generateX509Crl(InputStream in) throws CRLException, CertificateException {
		return CertificateFactory.getInstance("X.509").generateCRL(in);

	}

	public static List<String> getCrlDistributionPoints(X509Certificate cert) throws IOException {
		byte[] crlDistributionPoint = cert.getExtensionValue(Extension.cRLDistributionPoints.getId());
		if (crlDistributionPoint == null) {
			return Collections.emptyList();
		}

		CRLDistPoint distPoint = CRLDistPoint
				.getInstance(JcaX509ExtensionUtils.parseExtensionValue(crlDistributionPoint));

		List<String> urls = new ArrayList<String>();
		for (DistributionPoint dp : distPoint.getDistributionPoints()) {
			DistributionPointName dpn = dp.getDistributionPoint();
			// Look for URIs in fullName
			if (dpn != null) {
				if (dpn.getType() == DistributionPointName.FULL_NAME) {
					GeneralName[] genNames = GeneralNames.getInstance(dpn.getName()).getNames();
					// Look for an URI
					for (int j = 0; j < genNames.length; j++) {
						if (genNames[j].getTagNo() == GeneralName.uniformResourceIdentifier) {
							String url = DERIA5String.getInstance(genNames[j].getName()).getString();
							urls.add(url);
						}
					}
				}
			}
		}

		return urls;
	}
}
