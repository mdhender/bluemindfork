import { MailboxItemsClient, MailboxItem, MessageBody } from "@bluemind/backend.mail.api";
import { MimeType } from "@bluemind/email"; // FIXME: move MimeType into @bluemind/mime
import { ItemValue } from "@bluemind/core.container.api";
import { MimeParser, MimeBuilder } from "@bluemind/mime";
import {
    CRYPTO_HEADERS,
    ENCRYPTED_HEADER_NAME,
    SIGNED_HEADER_NAME,
    SMIME_CERT_USAGE,
    SMIME_ENCRYPTION_ERROR_PREFIX,
    SMIME_SIGNATURE_ERROR_PREFIX
} from "../../lib/constants";
import session from "../environnment/session";
import { fetchRequest, dispatchFetch } from "@bluemind/service-worker-utils";
import { getCacheKey, getGuid } from "./cache/SMimePartCache";
import { SmimeErrors } from "../../lib/exceptions";
import { extractContentType, splitHeadersAndContent } from "./MimeEntityParserUtils";
import extractSignedData from "./SMimeSignedDataParser";
import buildSignedEml from "./SMimeSignedEmlBuilder";
import pkcs7 from "../pkcs7";
import { checkCertificate, getMyCertificate, getMyPrivateKey, getCertificate } from "../pki";
import { DecryptResult } from "../../types";
import { addHeaderValue, resetHeader } from "../../lib/helper";

export function isEncrypted(part: MessageBody.Part): boolean {
    return MimeType.isPkcs7(part);
}

export function isSigned(part: MessageBody.Part): boolean {
    return part.mime === MimeType.PKCS_7_SIGNED_DATA || (!!part.children && part.children.some(isSigned));
}

export async function decrypt(folderUid: string, item: ItemValue<MailboxItem>): Promise<DecryptResult> {
    let content = "";
    const { imapUid, body } = item.value;
    try {
        body.headers = resetHeader(body.headers, ENCRYPTED_HEADER_NAME);
        const sid = await session.sid;
        const { address, mime, encoding, charset, fileName } = body.structure!;
        const key = await getMyPrivateKey();
        const certificate = await getMyCertificate();

        await checkCertificate(certificate, { date: new Date(body.date!) });
        const request = fetchRequest(sid, folderUid, imapUid!, address!, encoding!, mime!, charset!, fileName);
        const response = await dispatchFetch(request);
        const data = await response.blob();
        content = await pkcs7.decrypt(data, key, certificate);
        const parser = await new MimeParser(address).parse(content);
        const parts = parser.getParts();

        const savePartsPromises = [];
        for (const p of parts) {
            const partContent = parser.getPartContent(p.address!);
            const promise = savePart(folderUid, imapUid!, p, partContent);
            savePartsPromises.push(promise);
        }
        await Promise.all(savePartsPromises);

        body.structure = parser.structure as MessageBody.Part;
        body.headers = addHeaderValue(body?.headers, ENCRYPTED_HEADER_NAME, CRYPTO_HEADERS.OK);
    } catch (error: unknown) {
        const errorCode = error instanceof SmimeErrors ? error.code : CRYPTO_HEADERS.UNKNOWN;
        body.headers = addHeaderValue(body.headers, ENCRYPTED_HEADER_NAME, errorCode);
    }
    return { body, content };
}

export async function encrypt(item: MailboxItem, folderUid: string): Promise<MailboxItem> {
    try {
        const recipients = item.body.recipients || [];
        const promises = recipients.map(async ({ address, dn, kind }) => {
            if (!address) {
                throw "recipient " + dn + " has no address set";
            }
            const smimeUsage = SMIME_CERT_USAGE.ENCRYPT;
            if (kind === MessageBody.RecipientKind.Originator) {
                const myCertificate = await getMyCertificate();
                await checkCertificate(myCertificate, { expectedAddress: address, smimeUsage });
                return myCertificate;
            }
            const certificate = await getCertificate(address);
            await checkCertificate(certificate, { smimeUsage });
            return certificate;
        });
        const certificates = await Promise.all(promises);

        const client = new MailboxItemsClient(await session.sid, folderUid);
        let mimeTree: string;
        if (item.body.structure!.mime === MimeType.EML) {
            // mail has been signed just previously, and uploaded as an eml
            const eml = await client.fetch(item.imapUid!, item.body.structure!.address!).then(blob => blob.text());
            const { body, headers } = splitHeadersAndContent(eml);
            const contentType = "Content-Type: " + extractContentType(headers);
            mimeTree = contentType + "\r\n\r\n" + body;
        } else {
            mimeTree = await new MimeBuilder(getRemoteContentFn(item.imapUid!, folderUid)).build(item.body.structure!);
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

export async function verify(item: ItemValue<MailboxItem>, getEml: () => Promise<string>): Promise<MessageBody> {
    const body = item.value.body;
    try {
        body.headers = resetHeader(body.headers, SIGNED_HEADER_NAME);
        const eml = await getEml();
        const { toDigest, pkcs7Part } = extractSignedData(eml);
        await pkcs7.verify(pkcs7Part, toDigest, body);
        body.headers = addHeaderValue(body.headers, SIGNED_HEADER_NAME, CRYPTO_HEADERS.OK);
    } catch (error) {
        const errorCode = error instanceof SmimeErrors ? error.code : CRYPTO_HEADERS.UNKNOWN;
        body.headers = addHeaderValue(body.headers, SIGNED_HEADER_NAME, errorCode);
    }
    return body;
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
        await checkCertificate(certificate, { smimeUsage: SMIME_CERT_USAGE.SIGN });
        const signedContent = await pkcs7.sign(unsignedMimeEntity, key, certificate);
        const eml = buildSignedEml(unsignedMimeEntity, signedContent, item.body);
        const address = await client.uploadPart(eml);
        item.body.structure = { address, mime: "message/rfc822", children: [] };
    } catch (error) {
        const errorCode = error instanceof SmimeErrors ? error.code : CRYPTO_HEADERS.UNKNOWN;
        throw `[${SMIME_SIGNATURE_ERROR_PREFIX}:${errorCode}]` + error;
    }
    return item;
}

function removePreviousSignedPart(structure: MessageBody.Part): MessageBody.Part {
    if (structure.mime === MimeType.MULTIPART_MIXED && structure.children) {
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
        const sid = await session.sid;
        const request = fetchRequest(sid, folderUid, imapUid, p.address!, p.encoding!, p.mime!, p.charset!, p.fileName);
        const data = await dispatchFetch(request);
        return new Uint8Array(await data.arrayBuffer());
    };
}

async function savePart(
    folderUid: string,
    imapUid: number,
    part: MessageBody.Part,
    content: ArrayBuffer | undefined
): Promise<void> {
    const cache: Cache = await caches.open("smime-part-cache");
    const { address } = part;
    const guid = await getGuid(folderUid, imapUid);

    if (address && guid) {
        const key = await getCacheKey(address, guid, folderUid);
        cache.put(new Request(key), new Response(content));
    }
}

export default { isEncrypted, isSigned, decrypt, encrypt, verify, sign };
