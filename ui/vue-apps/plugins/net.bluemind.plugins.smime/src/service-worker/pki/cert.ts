import { pki } from "node-forge";
import { RevocationResult, SmimeRevocationClient } from "@bluemind/smime.cacerts.api";
import session from "../environnment/session";
import { SMIME_CERT_USAGE } from "../../lib/constants";
import { UntrustedCertificateEmailNotFoundError } from "../../lib/exceptions";

export async function checkRevoked(serialNumber: string) {
    const revokedList = await new SmimeRevocationClient(await session.sid, await session.domain).isRevoked([
        serialNumber
    ]);
    const revoked = revokedList[0];
    if (revoked.status === RevocationResult.RevocationStatus.REVOKED) {
        if (revoked.reason?.toLowerCase() === "certificatehold") {
            // no cache for those one
        }
        throw "Revoked certificate";
    }
}

export function checkBasicConstraints(certificate: pki.Certificate) {
    const basicConstraints = <pki.BasicConstraintsExtension>certificate.getExtension("basicConstraints");
    if (basicConstraints && basicConstraints.cA === true) {
        throw "CA certificate cannot be used to sign or encrypt S/MIME message";
    }
}

export function checkExtendedKeyUsage(certificate: pki.Certificate) {
    const anyExtendedKeyUsageOid = "2.5.29.37.0";
    const extendedKeyUsage = <pki.ExtendedKeyUsageExtension>certificate.getExtension("extKeyUsage");
    if (extendedKeyUsage && !extendedKeyUsage.emailProtection && !extendedKeyUsage[anyExtendedKeyUsageOid]) {
        throw "extendedKeyUsage is defined but its value is neither emailProtection nor anyExtendedKeyUsage";
    }
}

export function checkRecipientEmail(certificate: pki.Certificate, expectedEmail: string) {
    const subjectAltName = <pki.SubjectAltNameExtension>certificate.getExtension("subjectAltName");

    const subjectAltNameMatch =
        subjectAltName &&
        subjectAltName.altNames.find(
            altName => altName.type === 1 && altName.value.toLowerCase() === expectedEmail.toLowerCase()
        );
    const subjectEmailAddressMatch =
        certificate.subject.getField({ name: "emailAddress" })?.value.toLowerCase() === expectedEmail.toLowerCase();
    if (!subjectAltNameMatch && !subjectEmailAddressMatch) {
        throw new UntrustedCertificateEmailNotFoundError(expectedEmail);
    }
}

export function checkSmimeUsage(certificate: pki.Certificate, smimeUsage: SMIME_CERT_USAGE) {
    const keyUsage = <pki.KeyUsageExtension>certificate.getExtension("keyUsage");
    if (smimeUsage === SMIME_CERT_USAGE.SIGN && keyUsage && !keyUsage.nonRepudiation && !keyUsage.digitalSignature) {
        throw "this certificate can't be used to verify or sign message, keyUsage does not allow it (neither digitalSignature or nonRepudiation are set).";
    }
    if (smimeUsage === SMIME_CERT_USAGE.ENCRYPT && keyUsage && !keyUsage.keyEncipherment) {
        throw "this certificate can't be used to encrypt message, keyUsage does not allow it (keyEncipherment is not set).";
    }
}
