package net.bluemind.utils;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

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

	private CertificateUtils() {
	}

	public static String getCertCN(byte[] certFile) throws ServerFault {
		CertificateFactory cf;
		try {
			cf = CertificateFactory.getInstance("X.509");
			X509Certificate ca = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certFile));
			LdapName ldapDN = new LdapName(ca.getSubjectDN().getName());
			return ldapDN.getRdn(ldapDN.size() - 1).getValue().toString();
		} catch (CertificateException | InvalidNameException e) {
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
}
