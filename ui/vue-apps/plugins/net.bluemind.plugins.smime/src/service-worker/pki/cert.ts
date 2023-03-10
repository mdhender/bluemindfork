import { pki } from "node-forge";
import { RevocationResult, SmimeRevocationClient } from "@bluemind/smime.cacerts.api";
import session from "../environnment/session";
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
    const basicConstraints = certificate.getExtension("basicConstraints");
    if (basicConstraints && (<pki.BasicConstraintsExtension>basicConstraints).cA === true) {
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

export function checkRecipientEmail(certificate: pki.Certificate, recipientEmail: string) {
    const subjectAltName = <pki.SubjectAltNameExtension>certificate.getExtension("subjectAltName");

    const subjectAltNameMatch =
        subjectAltName &&
        subjectAltName.altNames.find(
            altName => altName.type === 1 && altName.value.toLowerCase() === recipientEmail.toLowerCase()
        );
    const subjectEmailAddressMatch =
        certificate.subject.getField({ name: "emailAddress" })?.value.toLowerCase() === recipientEmail.toLowerCase();
    if (!subjectAltNameMatch && !subjectEmailAddressMatch) {
        throw new UntrustedCertificateEmailNotFoundError(recipientEmail);
    }
}
