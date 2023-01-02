import { pki } from "node-forge";
import { AddressBooksClient, AddressBookClient, VCardInfo } from "@bluemind/addressbook.api";
import { searchVCardsHelper } from "@bluemind/contact";
import { ItemContainerValue } from "@bluemind/core.container.api";
import { PKIStatus } from "../../lib/constants";
import session from "../environnment/session";
import {
    ExpiredCertificateError,
    InvalidKeyError,
    InvalidCertificateError,
    CertificateRecipientNotFoundError
} from "../exceptions";
import db from "./SMimeDB";

export async function getCertificate(email: string): Promise<pki.Certificate> {
    const sid = await session.sid;
    const contactsInfo = await new AddressBooksClient(sid).search(searchVCardsHelper(email));
    if (contactsInfo.total === 0) {
        throw new CertificateRecipientNotFoundError(email);
    }
    const byContainers = groupByContainer(contactsInfo.values);

    let pem: string | undefined;
    for (const [containerUid, uids] of Object.entries(byContainers)) {
        const contacts = await new AddressBookClient(sid, containerUid).multipleGet(uids);
        const contact = contacts.find(contact => contact.value.security?.key?.value);
        pem = contact?.value?.security?.key?.value;
        if (pem) {
            break;
        }
    }
    if (!pem) {
        throw new CertificateRecipientNotFoundError(email);
    }
    try {
        return pki.certificateFromPem(pem);
    } catch (error) {
        throw new InvalidCertificateError(error);
    }
}

export function checkCertificateValidity(certificate: pki.Certificate, sendingDate: Date) {
    // TODO ? check if certificate is not revoked
    // TODO ? check chain of certificate (see pki.verifyCertificateChain)
    const isExpired = certificate.validity.notBefore > sendingDate || certificate.validity.notAfter < sendingDate;
    if (isExpired) {
        throw new ExpiredCertificateError();
    }
}

interface Cache {
    CERTIFICATE: pki.Certificate | null;
    PRIVATE_KEY: pki.rsa.PrivateKey | null;
}

const cache: Cache = {
    CERTIFICATE: null,
    PRIVATE_KEY: null
};
export async function getMyStatus() {
    return await db.getPKIStatus();
}
export async function clearMyCryptoFiles() {
    await db.clearPKI();
    cache.CERTIFICATE = null;
    cache.PRIVATE_KEY = null;
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
            throw new InvalidKeyError();
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
            try {
                const cert = await ((await db.getCertificate()) as Blob).text();
                cache.CERTIFICATE = pki.certificateFromPem(cert);
            } catch (error) {
                throw new InvalidCertificateError(error);
            }
        } else {
            throw new InvalidCertificateError();
        }
    }
    return <pki.Certificate>cache.CERTIFICATE;
}

export async function setMyCertificate(blob: Blob) {
    await db.setCertificate(blob);
    cache.CERTIFICATE = pki.certificateFromPem(await blob.text());
}

function groupByContainer(contactsInfo: ItemContainerValue<VCardInfo>[]) {
    const byContainers: { [container: string]: string[] } = contactsInfo.reduce(
        (byContainer: { [containerUid: string]: string[] }, contactInfo: ItemContainerValue<VCardInfo>) => {
            if (!byContainer[contactInfo.containerUid]) {
                byContainer[contactInfo.containerUid] = [];
            }
            byContainer[contactInfo.containerUid].push(contactInfo.uid!);
            return byContainer;
        },
        {}
    );
    return byContainers;
}
