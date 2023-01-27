import { pki } from "node-forge";
import { MailboxItemsClient, MailboxItem, MessageBody } from "@bluemind/backend.mail.api";
import { MimeType } from "@bluemind/email"; // FIXME: move MimeType into @bluemind/mime
import { ItemValue } from "@bluemind/core.container.api";
import { MimeParser, MimeBuilder } from "@bluemind/mime";
import {
    CRYPTO_HEADERS,
    ENCRYPTED_HEADER_NAME,
    SIGNED_HEADER_NAME,
    SMIME_ENCRYPTION_ERROR_PREFIX,
    SMIME_SIGNATURE_ERROR_PREFIX
} from "../../lib/constants";
import session from "../environnment/session";
import { dispatchFetch } from "@bluemind/service-worker-utils";
import { getCacheKey } from "../smimePartCache";
import { EncryptError, SmimeErrors } from "../exceptions";
import { extractContentType, splitHeadersAndContent } from "./MimeEntityParserUtils";
import extractSignedData from "./SMimeSignedDataParser";
import buildSignedEml from "./SMimeSignedEmlBuilder";
import pkcs7 from "../pkcs7";
import { checkCertificateValidity, getMyCertificate, getMyPrivateKey, getCertificate } from "../pki";
import { addHeaderValue, resetHeader } from "../../lib/helper";

export function isEncrypted(part: MessageBody.Part): boolean {
    return MimeType.isPkcs7(part);
}

export function isSigned(part: MessageBody.Part): boolean {
    return part.mime === MimeType.PKCS_7_SIGNED_DATA || (!!part.children && part.children.some(isSigned));
}

type DecryptResult = {
    item: ItemValue<MailboxItem>;
    content: string;
};
export async function decrypt(folderUid: string, item: ItemValue<MailboxItem>): Promise<DecryptResult> {
    let content = "";
    try {
        //TODO: Add a cache based on body guid
        item.value.body.headers = resetHeader(item.value.body.headers, ENCRYPTED_HEADER_NAME);
        const sid = await session.sid;
        const client = new MailboxItemsClient(sid, folderUid);
        const part = item.value.body.structure!;
        const key = await getMyPrivateKey();
        const certificate = await getMyCertificate();
        // FIXME: use correct date instead of internalDate
        checkCertificateValidity(certificate, new Date(item.value.body.date!));
        const data = await client.fetch(item.value.imapUid!, part.address!, part.encoding!, part.mime!);
        content = await pkcs7.decrypt(data, key, certificate);
        const parser = await new MimeParser(part.address).parse(content);
        const parts = parser.getParts();
        for (const p of parts) {
            const content = parser.getPartContent(p.address!);
            savePart(folderUid, item.value.imapUid!, p, content);
        }
        item.value.body.structure = parser.structure as MessageBody.Part;
        item.value.body.headers = addHeaderValue(item.value.body?.headers, ENCRYPTED_HEADER_NAME, CRYPTO_HEADERS.OK);
    } catch (error: unknown) {
        const errorCode = error instanceof SmimeErrors ? error.code : CRYPTO_HEADERS.UNKNOWN;
        item.value.body.headers = addHeaderValue(item.value.body.headers, ENCRYPTED_HEADER_NAME, errorCode);
    }
    return { item, content };
}

export async function encrypt(item: MailboxItem, folderUid: string): Promise<MailboxItem> {
    try {
        const myCertificate = await getMyCertificate();
        const recipients = item.body.recipients || [];
        const promises: Promise<pki.Certificate>[] = recipients.flatMap(({ kind, address }) => {
            return address && kind !== MessageBody.RecipientKind.Originator ? getCertificate(address) : [];
        });
        const certificates: pki.Certificate[] = await Promise.all(promises);
        certificates.push(myCertificate);

        const client = new MailboxItemsClient(await session.sid, folderUid);
        let mimeTree: string;
        try {
            if (item.body.structure!.mime === MimeType.EML) {
                // mail has been signed just previously, and uploaded as an eml
                const eml = await client.fetch(item.imapUid!, item.body.structure!.address!).then(blob => blob.text());
                const { body, headers } = splitHeadersAndContent(eml);
                const contentType = "Content-Type: " + extractContentType(headers);
                mimeTree = contentType + "\r\n\r\n" + body;
            } else {
                mimeTree = await new MimeBuilder(getRemoteContentFn(item.imapUid!, folderUid)).build(
                    item.body.structure!
                );
            }
        } catch (error) {
            throw new EncryptError(error);
        }
        const encryptedPart = pkcs7.encrypt(mimeTree, certificates);
        const address = await client.uploadPart(encryptedPart);
        item.body.structure = { address, charset: "utf-8", encoding: "base64", mime: MimeType.PKCS_7 };
    } catch (error) {
        const errorCode = error instanceof SmimeErrors ? error.code : CRYPTO_HEADERS.UNKNOWN;
        throw `[${SMIME_ENCRYPTION_ERROR_PREFIX}:${errorCode}]` + error;
    }
    return item;
}

export async function verify(
    folderUid: string,
    item: ItemValue<MailboxItem>,
    getEml: () => Promise<string>
): Promise<ItemValue<MailboxItem>> {
    try {
        item.value.body.headers = resetHeader(item.value.body.headers, SIGNED_HEADER_NAME);
        const eml = await getEml();
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
        const unsignedMimeEntity = await new MimeBuilder(getRemoteContentFn(item.imapUid!, folderUid)).build(
            item.body.structure!
        );
        const key = await getMyPrivateKey();
        const certificate = await getMyCertificate();
        const signedContent = await pkcs7.sign(unsignedMimeEntity, key, certificate);
        const eml = buildSignedEml(unsignedMimeEntity, signedContent, item);
        const address = await client.uploadPart(eml);
        item.body.structure = { address, mime: "message/rfc822", children: [] };
    } catch (error) {
        const errorCode = error instanceof SmimeErrors ? error.code : CRYPTO_HEADERS.UNKNOWN;
        throw `[${SMIME_SIGNATURE_ERROR_PREFIX}:${errorCode}]` + error;
    }
    return item;
}

function removePreviousSignedPart(structure: MessageBody.Part): MessageBody.Part {
    // FIXME: use MimeType.js once this package is usable in worker
    if (structure.mime === "multipart/mixed" && structure.children) {
        const signedPartIndex = structure.children.findIndex(part => part.mime === MimeType.PKCS_7_SIGNED_DATA);
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
