import { pki } from "node-forge";
import { ItemValue } from "@bluemind/core.container.api";
import { RevocationResult, SmimeCacert, SmimeCACertClient, SmimeRevocationClient } from "@bluemind/smime.cacerts.api";
import db from "./SMimePkiDB";
import session from "../environnment/session";
import { SMIME_CERT_USAGE } from "../../lib/constants";
import { UntrustedCertificateEmailNotFoundError } from "../../lib/exceptions";

export async function checkRevoked(certificate: pki.Certificate, date?: Date) {
    const revocationResult = await getRevocation(certificate);
    if (
        revocationResult.status === RevocationResult.RevocationStatus.REVOKED &&
        (!date || date.getTime() > revocationResult.revocation.revocationDate!)
    ) {
        throw "Revoked certificate";
    }
}

async function getRevocation(certificate: pki.Certificate): Promise<RevocationResult> {
    const cached = await db.getRevocation(certificate.serialNumber, certificate.issuer.hash);
    if (cached && cached.cacheValidity > new Date()) {
        return cached;
    }
    const revocationList = await new SmimeRevocationClient(await session.sid, await session.domain).areRevoked([
        {
            serialNumber: certificate.serialNumber,
            issuer: formatToRFC2253(certificate.issuer)
        }
    ]);
    const revocationResult = revocationList[0];
    const cacheValidity = getRevocationCacheValidity(revocationResult);
    await db.setRevocation({ ...revocationResult, cacheValidity }, certificate.issuer.hash);
    return revocationResult;
}

function formatToRFC2253(issuer: pki.Certificate["issuer"]): string {
    return Object.values(issuer.attributes)
        .map(attr => attr.type + "=" + attr.value)
        .join(",");
}

// exported only for tests
export function getRevocationCacheValidity(revocationResult: RevocationResult): Date {
    if (revocationResult.status === RevocationResult.RevocationStatus.REVOKED) {
        const reason = revocationResult.revocation.revocationReason;
        if (reason && reason.toLowerCase() === "certificatehold") {
            const expiration = new Date();
            expiration.setDate(expiration.getDate() + 1);
            return expiration;
        }
        const MAX_TIMESTAMP = 8640000000000000;
        return new Date(MAX_TIMESTAMP);
    }
    const expiration = new Date();
    expiration.setDate(expiration.getDate() + 7);
    return expiration;
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

let caCerts: ItemValue<SmimeCacert>[];
// FIXME: sync them ? todo via https://forge.bluemind.net/jira/browse/FEATWEBML-2107
export async function getCaCerts(): Promise<ItemValue<SmimeCacert>[]> {
    if (!caCerts) {
        const domain = await session.domain;
        const sid = await session.sid;
        const client = new SmimeCACertClient(sid, "smime_cacerts:domain_" + domain);
        caCerts = await client.all();
    }
    return caCerts;
}
