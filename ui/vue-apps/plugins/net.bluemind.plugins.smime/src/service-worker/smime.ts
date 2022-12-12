import { MailboxItemsClient, MailboxItem, MessageBody } from "@bluemind/backend.mail.api";
import { ItemValue } from "@bluemind/core.container.api";
import { MimeParser, MimeBuilder } from "@bluemind/mime";
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
import { checkCertificateValidity, getMyCertificate, getMyPrivateKey } from "./pki/";
import { logger } from "./environnment/logger";

export function isEncrypted(item: ItemValue<MailboxItem>): boolean {
    return PKCS7_MIMES.includes(item.value!.body!.structure!.mime!);
}
export function isSigned(item: ItemValue<MailboxItem>): boolean {
    return item.value!.body!.structure!.mime === MULTIPART_SIGNED_MIME;
}

export async function decrypt(folderUid: string, item: ItemValue<MailboxItem>): Promise<ItemValue<MailboxItem>> {
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
                savePart(folderUid, item.value!.imapUid!, p, content);
            }
            item.value!.body!.structure = parser.structure as MessageBody.Part;
        }
        setHeader(item, ENCRYPTED_HEADER_NAME, CRYPTO_HEADERS.DECRYPTED);
    } catch (error: unknown) {
        const errorCode = error instanceof SmimeErrors ? error.code : CRYPTO_HEADERS.UNKNOWN;
        setHeader(item, ENCRYPTED_HEADER_NAME, errorCode);
    }
    return item;
}

export async function encrypt(item: MailboxItem, folderUid: string) {
    const encryptedItem = { ...item };

    // TODO: get the certificates for all recipients
    const certificate = await getMyCertificate();
    if (encryptedItem.body.structure && encryptedItem.imapUid) {
        const sid = await session.sid;
        const client = new MailboxItemsClient(sid, folderUid);

        const getContentFn = async (p: MessageBody.Part): Promise<Uint8Array | null> => {
            if (encryptedItem.imapUid && p.address) {
                const data = await client.fetch(encryptedItem.imapUid, p.address, p.encoding, p.mime);
                return new Uint8Array(await data.arrayBuffer());
            }
            return null;
        };
        const mimeTree = await new MimeBuilder(getContentFn).build(encryptedItem.body.structure);
        if (mimeTree) {
            const encryptedPart = pkcs7.encrypt(mimeTree, certificate);
            const address = await client.uploadPart(encryptedPart);
            const part = {
                address,
                charset: "utf-8",
                encoding: "base64",
                mime: PKCS7_MIMES[0]
            };
            encryptedItem.body.structure = part;
        }
    }

    return encryptedItem;
}

export async function verify(folderUid: string, item: ItemValue<MailboxItem>): Promise<ItemValue<MailboxItem>> {
    try {
        const client = new MailboxItemsClient(await session.sid, folderUid);
        const eml = await client.fetchComplete(item.value.imapUid!).then(eml => eml.text());
        const { toDigest, pkcs7Part } = extractSignedData(eml);
        await pkcs7.verify(pkcs7Part, toDigest, getSenderAddress(item));
        setHeader(item, SIGNED_HEADER_NAME, CRYPTO_HEADERS.VERIFIED);
    } catch (error) {
        logger.error(error);
        const errorCode = error instanceof SmimeErrors ? error.code : CRYPTO_HEADERS.UNKNOWN;
        setHeader(item, SIGNED_HEADER_NAME, errorCode);
    }
    return item;
}

export function sign() {
    return null;
}

function getSenderAddress(item: ItemValue<MailboxItem>): string {
    if (item.value.body.recipients) {
        const from = item.value.body.recipients.find(
            recipient => recipient.kind === MessageBody.RecipientKind.Originator
        );
        if (!from) {
            throw new RecipientNotFoundError();
        }
        return from.address || "";
    }
    return "";
}

//FIXME: This should be imported from a third party package
async function savePart(uid: string, imap: number, part: MessageBody.Part, content: ArrayBuffer | undefined) {
    const cache = await caches.open("part-cache");
    const { address } = part;
    const request = new Request(`/api/mail_items/${uid}/part/${imap}/${address}`);
    cache.put(request.url, new Response(content));
}

function setHeader(item: ItemValue<MailboxItem>, headerName: string, headerValue: number): void {
    if (item.value.body?.headers) {
        const index = item.value.body.headers.findIndex(({ name }) => name === headerName);

        if (index === -1) {
            item.value.body.headers.push({ name: headerName, values: [headerValue.toString()] });
        } else {
            const currentValues = item.value.body.headers[index].values || [];
            const newValue = parseInt(currentValues[0]) | headerValue;
            item.value.body.headers[index] = { name: headerName, values: [newValue.toString()] };
        }
    }
}

export default {
    isEncrypted,
    decrypt,
    encrypt,
    verify,
    sign
};
