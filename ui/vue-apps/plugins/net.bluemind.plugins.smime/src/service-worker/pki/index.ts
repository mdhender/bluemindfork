import { pki } from "node-forge";
import checkCertificate from "./checkCertificate";
import getCertificate from "./getCertificate";
import { PKIStatus } from "../../lib/constants";
import { InvalidKeyError, KeyNotFoundError, MyCertificateNotFoundError } from "../../lib/exceptions";
import db from "./SMimePkiDB";

export { checkCertificate, getCertificate };

interface Cache {
    CERTIFICATE: string | null;
    PRIVATE_KEY: pki.rsa.PrivateKey | null;
}

const cache: Cache = {
    CERTIFICATE: null,
    PRIVATE_KEY: null
};
export async function getMyStatus() {
    return await db.getPKIStatus();
}
export async function clear() {
    await db.clearMyCertAndKey();
    cache.CERTIFICATE = null;
    cache.PRIVATE_KEY = null;
    await db.clearRevocations();
}
export async function getMyPrivateKey(): Promise<pki.rsa.PrivateKey> {
    if (!cache.PRIVATE_KEY) {
        const pkiStatus = await db.getPKIStatus();
        if (pkiStatus & PKIStatus.OK || pkiStatus & PKIStatus.PRIVATE_KEY_OK) {
            try {
                const key = await ((await db.getPrivateKey()) as Blob).text();
                cache.PRIVATE_KEY = pki.privateKeyFromPem(key);
            } catch (error) {
                throw new InvalidKeyError(error);
            }
        } else {
            throw new KeyNotFoundError();
        }
    }
    return <pki.rsa.PrivateKey>cache.PRIVATE_KEY;
}

export async function setMyPrivateKey(blob: Blob) {
    await db.setPrivateKey(blob);
    cache.PRIVATE_KEY = pki.privateKeyFromPem(await blob.text());
}

export async function getMyCertificate() {
    if (!cache.CERTIFICATE) {
        const pkiStatus = await db.getPKIStatus();
        if (pkiStatus & PKIStatus.OK || pkiStatus & PKIStatus.CERTIFICATE_OK) {
            const cert = await (<Blob>await db.getCertificate()).text();
            cache.CERTIFICATE = cert;
        } else {
            throw new MyCertificateNotFoundError();
        }
    }
    return cache.CERTIFICATE;
}

export async function setMyCertificate(blob: Blob) {
    await db.setCertificate(blob);
    cache.CERTIFICATE = await blob.text();
}
