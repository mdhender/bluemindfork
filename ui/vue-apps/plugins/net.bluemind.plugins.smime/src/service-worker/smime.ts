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
    SMIME_ENCRYPTION_ERROR_PREFIX,
    SMIME_SIGNATURE_ERROR_PREFIX
} from "../lib/constants";
import session from "./environnment/session";
import { EncryptError, SmimeErrors } from "./exceptions";
import extractSignedData from "./signedDataParser";
import pkcs7 from "./pkcs7/";
import { checkCertificateValidity, getMyCertificate, getMyPrivateKey, getCertificate } from "./pki/";
import { addHeaderValue, resetHeader } from "../lib/helper";
import { dispatchFetch } from "@bluemind/service-worker-utils";
import { getCacheKey } from "./smimePartCache";

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
                const partContent = parser.getPartContent(p.address!);
                await savePart(folderUid, item.value!.imapUid!, p, partContent);
            }
            item.value.body.structure = parser.structure as MessageBody.Part;
        }
        item.value.body.headers = addHeaderValue(item.value.body?.headers, ENCRYPTED_HEADER_NAME, CRYPTO_HEADERS.OK);
    } catch (error: unknown) {
        const errorCode = error instanceof SmimeErrors ? error.code : CRYPTO_HEADERS.UNKNOWN;
        item.value.body.headers = addHeaderValue(item.value.body.headers, ENCRYPTED_HEADER_NAME, errorCode);
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
                mimeTree = await new MimeBuilder(getRemoteContentFn(item.imapUid!, folderUid)).build(
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
        throw `[${SMIME_ENCRYPTION_ERROR_PREFIX}:${errorCode}]` + error;
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
    }
    return item;
}

export async function sign(item: MailboxItem, folderUid: string): Promise<MailboxItem> {
    try {
        item.body.structure = removePreviousSignedPart(item.body.structure!);
        const client = new MailboxItemsClient(await session.sid, folderUid);

        const unsignedContent = await new MimeBuilder(getRemoteContentFn(item.imapUid!, folderUid)).build(
            item.body.structure!
        );
        const { content: unsignedWithoutHeaders, headers } = splitHeadersAndContent(unsignedContent);
        const contentType = extractContentType(headers);

        const unsignedPartAddress = await client.uploadPart(unsignedWithoutHeaders);
        const unsignedPart = { ...item.body.structure, address: unsignedPartAddress, children: [] };
        if (!unsignedPart.headers) unsignedPart.headers = [];
        unsignedPart.headers.push({ name: "Content-Type", values: [contentType] });

        const key = await getMyPrivateKey();
        const certificate = await getMyCertificate();
        const signedContent = await pkcs7.sign(unsignedContent, key, certificate);
        const signedPartAddress = await client.uploadPart(signedContent);
        const signedPart = buildSignedPart(signedPartAddress);

        item.body.structure = buildMultipartSigned(UUIDGenerator.generate(), [unsignedPart, signedPart]);
    } catch (error) {
        const errorCode = error instanceof SmimeErrors ? error.code : CRYPTO_HEADERS.UNKNOWN;
        throw `[${SMIME_SIGNATURE_ERROR_PREFIX}:${errorCode}]` + error;
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

function getRemoteContentFn(imapUid: number, folderUid: string) {
    return async (p: MessageBody.Part): Promise<string | Uint8Array> => {
        const filenameParam = p.fileName ? "&filename=" + p.fileName : "";
        const encodedMime = encodeURIComponent(p.mime!);
        const apiCoreUrl = `/api/mail_items/${folderUid}/part/${imapUid}/${p.address}?encoding=${p.encoding}&mime=${encodedMime}&charset=${p.charset}${filenameParam}`;
        const data = await dispatchFetch(new Request(apiCoreUrl));
        return new Uint8Array(await data.arrayBuffer());
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
async function savePart(folderUid: string, imapUid: number, part: MessageBody.Part, content: ArrayBuffer | undefined) {
    const { address } = part;
    const cache: Cache = await caches.open("smime-part-cache");

    if (address) {
        const key = getCacheKey(folderUid, imapUid, address);
        cache.put(new Request(key), new Response(content));
    }
}

export default { isEncrypted, isSigned, decrypt, encrypt, verify, sign };
