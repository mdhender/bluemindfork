import { pki } from "node-forge";
import { PKIStatus } from "../lib/constants";
import { ExpiredCertificateError, InvalidKeyError, InvalidCertificateError } from "./exceptions";

import db from "./SMimeDB";

let PRIVATE_KEY: pki.rsa.PrivateKey;
let CERTIFICATE: pki.Certificate;

export async function getMyPrivateKey(): Promise<pki.rsa.PrivateKey> {
    if (!PRIVATE_KEY) {
        await load();
    }
    return PRIVATE_KEY;
}
export async function getMyCertificate(): Promise<pki.Certificate> {
    if (!CERTIFICATE) {
        await load();
    }
    return CERTIFICATE;
}

export async function getCertificate(email: string): Promise<pki.Certificate | undefined> {
    // TODO
    return Promise.resolve(undefined);
}

export function checkCertificateValidity(certificate: pki.Certificate, sendingDate: Date) {
    // TODO ? check if certificate is not revoked
    // TODO ? check chain of certificate (see pki.verifyCertificateChain)
    const isExpired = certificate.validity.notBefore > sendingDate || certificate.validity.notAfter < sendingDate;
    if (isExpired) {
        throw new ExpiredCertificateError();
    }
}

async function load(): Promise<void> {
    const pkiStatus = await db.getPKIStatus();
    if ((pkiStatus & PKIStatus.OK) === PKIStatus.OK) {
        try {
            const key = await ((await db.getPrivateKey()) as Blob).text();
            PRIVATE_KEY = pki.privateKeyFromPem(key);
        } catch (error) {
            throw new InvalidKeyError(error);
        }
        try {
            CERTIFICATE = pki.certificateFromPem(await ((await db.getCertificate()) as Blob).text());
        } catch (error) {
            throw new InvalidCertificateError(error);
        }
    } else if (pkiStatus & PKIStatus.PRIVATE_KEY_OK) {
        throw new InvalidCertificateError();
    } else {
        throw new InvalidKeyError();
    }
}
