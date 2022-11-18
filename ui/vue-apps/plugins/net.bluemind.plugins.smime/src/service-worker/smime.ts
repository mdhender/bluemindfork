import {
    MailboxItemsClient,
    ItemValue,
    MailboxItem,
    MessageBody,
    MessageBodyRecipientKind
} from "@bluemind/backend.mail.api";
import { MimeParser } from "@bluemind/mime";
import {
    CRYPTO_HEADERS,
    ENCRYPTED_HEADER_NAME,
    MULTIPART_SIGNED_MIME,
    PKCS7_MIMES,
    SIGNED_HEADER_NAME
} from "../lib/constants";
import session from "./environnment/session";
import { RecipientNotFoundError, SmimeErrors } from "./exceptions";
import extractSignedData from "./signedDataParser";
import pkcs7 from "./pkcs7/";
import { checkCertificateValidity, getMyCertificate, getMyPrivateKey } from "./pki";
import { logger } from "./environnment/logger";

export function isEncrypted(item: ItemValue<MailboxItem>): boolean {
    return PKCS7_MIMES.includes(item.value.body.structure.mime);
}
export function isSigned(item: ItemValue<MailboxItem>): boolean {
    return item.value.body.structure.mime === MULTIPART_SIGNED_MIME;
}

export async function decrypt(folderUid: string, item: ItemValue<MailboxItem>): Promise<ItemValue<MailboxItem>> {
    //TODO: Add a cache based on body guid
    try {
        const sid = await session.sid;
        const client = new MailboxItemsClient(sid, folderUid);
        const part = item.value.body.structure;
        const key = await getMyPrivateKey();
        const certificate = await getMyCertificate();
        // FIXME: use correct date instead of internalDate
        checkCertificateValidity(certificate, new Date(item.value.internalDate));
        const data = await client.fetch(item.value.imapUid, part.address, part.encoding, part.mime);
        const content = await pkcs7.decrypt(data, key, certificate);
        if (content) {
            const parser = await new MimeParser(part.address).parse(content);
            const parts = parser.getParts();
            for (const part of parts) {
                const content = parser.getPartContent(part.address);
                savePart(folderUid, item.value.imapUid, part, content);
            }
            item.value.body.structure = parser.structure as MessageBody.Part;
        }
        setHeader(item, ENCRYPTED_HEADER_NAME, CRYPTO_HEADERS.DECRYPTED);
    } catch (error: unknown) {
        const errorName = error instanceof SmimeErrors ? error.name : CRYPTO_HEADERS.UNKNOWN;
        setHeader(item, ENCRYPTED_HEADER_NAME, errorName);
    }
    return item;
}

export function encrypt() {
    return null;
}

export async function verify(folderUid: string, item: ItemValue<MailboxItem>): Promise<ItemValue<MailboxItem>> {
    try {
        const client = new MailboxItemsClient(await session.sid, folderUid);
        const eml = await client.fetchComplete(item.value.imapUid).then(eml => eml.text());
        const { toDigest, pkcs7Part } = extractSignedData(eml);
        await pkcs7.verify(pkcs7Part, toDigest, getSenderAddress(item));
        setHeader(item, SIGNED_HEADER_NAME, CRYPTO_HEADERS.VERIFIED);
    } catch (error) {
        logger.error(error);
        const errorName = error instanceof SmimeErrors ? error.name : CRYPTO_HEADERS.UNKNOWN;
        setHeader(item, SIGNED_HEADER_NAME, errorName);
    }
    return item;
}

export function sign() {
    return null;
}

function getSenderAddress(item: ItemValue<MailboxItem>): string {
    const from = item.value.body.recipients.find(recipient => recipient.kind === MessageBodyRecipientKind.Originator);
    if (!from) {
        throw new RecipientNotFoundError();
    }
    return from.address;
}

//FIXME: This should be imported from a third party package
async function savePart(uid: string, imap: string, part: MessageBody.Part, content: ArrayBuffer | undefined) {
    const cache = await caches.open("part-cache");
    const { address, charset, mime, encoding, fileName } = part;
    let queryStrings = `encoding=${encoding}&mime=${encodeURIComponent(mime)}&charset=${charset}`;
    if (fileName) {
        queryStrings += `&filename=${fileName}`;
    }
    const request = new Request(`/api/mail_items/${uid}/part/${imap}/${address}?${queryStrings}`);
    cache.put(request.url, new Response(content));
}

function setHeader(item: ItemValue<MailboxItem>, headerName: string, headerValue: string) {
    const index = item.value.body.headers.findIndex(({ name }) => name === headerName);
    if (index === -1) {
        item.value.body.headers.push({ name: headerName, values: [headerValue] });
    } else {
        item.value.body.headers[index].values.push(headerValue);
    }
}

export default {
    isEncrypted,
    decrypt,
    encrypt,
    verify,
    sign
};
