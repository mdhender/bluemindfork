import { pki } from "node-forge";
import { DispositionType, MailboxItemsClient, MailboxItem, MessageBody } from "@bluemind/backend.mail.api";
import { ItemValue } from "@bluemind/core.container.api";
import { MimeParser, MimeBuilder } from "@bluemind/mime";
import UUIDGenerator from "@bluemind/uuid";
import {
    CRYPTO_HEADERS,
    ENCRYPTED_HEADER_NAME,
    MULTIPART_SIGNED_MIME,
    PKCS7_MIMES,
    SIGNATURE_MIME,
    SIGNED_HEADER_NAME,
    SMIME_ENCRYPTION_ERROR_PREFIX
} from "../lib/constants";
import session from "./environnment/session";
import { EncryptError, SmimeErrors } from "./exceptions";
import extractSignedData from "./signedDataParser";
import pkcs7 from "./pkcs7/";
import { checkCertificateValidity, getMyCertificate, getMyPrivateKey, getCertificate } from "./pki/";
import { addHeaderValue, resetHeader } from "../lib/helper";

export function isEncrypted(item: ItemValue<MailboxItem>): boolean {
    return PKCS7_MIMES.includes(item.value!.body!.structure!.mime!);
}
export function isSigned(item: ItemValue<MailboxItem>): boolean {
    return item.value!.body!.structure!.mime === MULTIPART_SIGNED_MIME;
}

export async function decrypt(folderUid: string, item: ItemValue<MailboxItem>): Promise<ItemValue<MailboxItem>> {
    item.value.body.headers = resetHeader(item.value.body.headers, ENCRYPTED_HEADER_NAME);
    //TODO: Add a cache based on body guid
    try {
        const sid = await session.sid;
        const client = new MailboxItemsClient(sid, folderUid);
        const part = item.value!.body!.structure!;
        const key = await getMyPrivateKey();
        const certificate = await getMyCertificate();
        // FIXME: use correct date instead of internalDate
        checkCertificateValidity(certificate, new Date(item.value!.body!.date!));
        const data = await client.fetch(item.value!.imapUid!, part.address!, part.encoding!, part.mime!);
        const content = await pkcs7.decrypt(data, key, certificate);
        if (content) {
            const parser = await new MimeParser(part.address).parse(content);
            const parts = parser.getParts();
            for (const p of parts) {
                const content = parser.getPartContent(p.address!);
                savePart(folderUid, item.value.imapUid!, p, content);
            }
            item.value.body.structure = parser.structure as MessageBody.Part;
        }
        item.value.body.headers = addHeaderValue(item.value.body?.headers, ENCRYPTED_HEADER_NAME, CRYPTO_HEADERS.OK);
    } catch (error: unknown) {
        const errorCode = error instanceof SmimeErrors ? error.code : CRYPTO_HEADERS.UNKNOWN;
        item.value.body.headers = addHeaderValue(item.value.body.headers, ENCRYPTED_HEADER_NAME, errorCode);
        throw error;
    }
    return item;
}

export async function encrypt(item: MailboxItem, folderUid: string): Promise<MailboxItem> {
    const encryptedItem = { ...item };
    const client = new MailboxItemsClient(await session.sid, folderUid);
    let mimeTree: string;

    try {
        const myCertificate = await getMyCertificate();
        const recipients = item.body.recipients || [];
        const promises: Promise<pki.Certificate>[] = recipients.flatMap(({ kind, address }) => {
            return address && kind !== MessageBody.RecipientKind.Originator ? getCertificate(address) : [];
        });
        const certificates: pki.Certificate[] = await Promise.all(promises);
        certificates.push(myCertificate);

        if (encryptedItem.body.structure && encryptedItem.imapUid) {
            try {
                mimeTree = await new MimeBuilder(getRemoteContentFn(client, item.imapUid!)).build(
                    encryptedItem.body.structure
                );
            } catch (error) {
                throw new EncryptError(error);
            }
            const encryptedPart = pkcs7.encrypt(mimeTree, certificates);
            const address = await client.uploadPart(encryptedPart);
            const part = { address, charset: "utf-8", encoding: "base64", mime: PKCS7_MIMES[0] };
            encryptedItem.body.structure = part;
        }
        encryptedItem.body.headers = addHeaderValue(item.body?.headers, ENCRYPTED_HEADER_NAME, CRYPTO_HEADERS.OK);
    } catch (error) {
        const errorCode = error instanceof SmimeErrors ? error.code : CRYPTO_HEADERS.UNKNOWN;
        throw `[${SMIME_ENCRYPTION_ERROR_PREFIX}:${errorCode}]`;
    }
    return encryptedItem;
}

export async function verify(folderUid: string, item: ItemValue<MailboxItem>): Promise<ItemValue<MailboxItem>> {
    item.value.body.headers = resetHeader(item.value.body.headers, SIGNED_HEADER_NAME);
    try {
        const client = new MailboxItemsClient(await session.sid, folderUid);
        const eml = await client.fetchComplete(item.value.imapUid!).then(eml => eml.text());
        const { toDigest, pkcs7Part } = extractSignedData(eml);
        await pkcs7.verify(pkcs7Part, toDigest);
        item.value.body.headers = addHeaderValue(item.value.body.headers, SIGNED_HEADER_NAME, CRYPTO_HEADERS.OK);
    } catch (error) {
        const errorCode = error instanceof SmimeErrors ? error.code : CRYPTO_HEADERS.UNKNOWN;
        item.value.body.headers = addHeaderValue(item.value.body.headers, SIGNED_HEADER_NAME, errorCode);
        throw error;
    }
    return item;
}

export async function sign(item: MailboxItem, folderUid: string): Promise<MailboxItem> {
    try {
        item.body.structure = removePreviousSignedPart(item.body.structure!);
        const client = new MailboxItemsClient(await session.sid, folderUid);

        const unsignedContent = await new MimeBuilder(getRemoteContentFn(client, item.imapUid!)).build(
            item.body.structure!
        );
        const { content: unsignedWithoutHeaders, headers } = splitHeadersAndContent(unsignedContent);
        const contentType = extractContentType(headers);

        const unsignedPartAddress = await client.uploadPart(unsignedWithoutHeaders);
        const unsignedPart = { ...item.body.structure, address: unsignedPartAddress, children: [] };
        if (!unsignedPart.headers) unsignedPart.headers = [];
        unsignedPart.headers.push({ name: "Content-Type", values: [contentType] });

        const signedContent = await pkcs7.sign(unsignedContent);
        const signedPartAddress = await client.uploadPart(signedContent);
        const signedPart = buildSignedPart(signedPartAddress);

        item.body.structure = buildMultipartSigned(UUIDGenerator.generate(), [unsignedPart, signedPart]);
    } catch (error) {
        const errorCode = error instanceof SmimeErrors ? error.code : CRYPTO_HEADERS.UNKNOWN;
        item.body.headers = addHeaderValue(item.body.headers, SIGNED_HEADER_NAME, errorCode);
        throw error;
    }
    return item;
}

function buildSignedPart(address: string) {
    return {
        address,
        mime: SIGNATURE_MIME,
        dispositionType: DispositionType.ATTACHMENT,
        fileName: "smime.p7s",
        headers: [
            { name: "Content-Type", values: [`${SIGNATURE_MIME}; name="smime.p7s"`] },
            { name: "Content-Transfer-Encoding", values: ["base64"] }
        ]
    };
}

function buildMultipartSigned(boundaryValue: string, children: Array<MessageBody.Part>) {
    const contentType = `multipart/signed; protocol="${SIGNATURE_MIME}"; micalg=sha-256; boundary="${boundaryValue}"`;
    return { mime: "multipart/signed", headers: [{ name: "Content-Type", values: [contentType] }], children };
}

function removePreviousSignedPart(structure: MessageBody.Part): MessageBody.Part {
    // FIXME: use MimeType.js once this package is usable in worker
    if (structure.mime === "multipart/mixed" && structure.children) {
        const signedPartIndex = structure.children.findIndex(part => part.mime === SIGNATURE_MIME);
        if (signedPartIndex !== -1) {
            structure.children.splice(signedPartIndex, 1);
            if (structure.children.length === 1) {
                return structure.children[0];
            }
        }
    }
    return structure;
}

function getRemoteContentFn(client: MailboxItemsClient, imapUid: number) {
    return async (p: MessageBody.Part): Promise<string | Uint8Array> => {
        const data: Blob = await client.fetch(imapUid, p.address!, p.encoding, p.mime, p.charset);
        return data.text();
    };
}

function splitHeadersAndContent(content: string): { content: string; headers: string } {
    const lineBreak = "\r\n";
    const separator = lineBreak + lineBreak;
    const separatorIndex = content.indexOf(separator);
    const headers = content.substring(0, separatorIndex).toLowerCase();
    return { content: content.substring(separatorIndex + separator.length), headers };
}

function extractContentType(headers: string): string {
    const match = new RegExp(/content-type:\s?((?:(?![\w-]+:).*(?:\n|$))*)/gi).exec(headers)?.input || "";
    return match.replace("content-type: ", "");
}

//FIXME: This should be imported from a third party package
async function savePart(uid: string, imap: number, part: MessageBody.Part, content: ArrayBuffer | undefined) {
    const cache = await caches.open("part-cache");
    const { address } = part;
    if (address) {
        const request = new Request(constructCacheUrl(uid, imap, address));
        cache.put(request.url, new Response(content));
    }
}

function constructCacheUrl(folderUid: string, imapUid: number, address: string) {
    return `/api/mail_items/${folderUid}/part/${imapUid}/${address}`;
}

export default { isEncrypted, decrypt, encrypt, verify, sign };
