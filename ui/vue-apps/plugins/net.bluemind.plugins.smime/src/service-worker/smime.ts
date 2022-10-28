import { MailboxItemsClient, ItemValue, MailboxItem, MessageBody } from "@bluemind/backend.mail.api";
import { MimeParser } from "@bluemind/mime";
import { CRYPTO_HEADERS, CRYPTO_HEADER_NAME, PKCS7_MIMES } from "../lib/constants";

import session from "./environnment/session";

import pkcs7 from "./pkcs7";
import { getMyCertificate, getMyPrivateKey } from "./pki";
import { SmimeErrors } from "./exceptions";

export function isEncrypted(item: ItemValue<MailboxItem>): boolean {
    return PKCS7_MIMES.includes(item.value.body.structure.mime);
}

export async function decrypt(folderUid: string, item: ItemValue<MailboxItem>): Promise<ItemValue<MailboxItem>> {
    //TODO: Add a cache based on body guid
    const encryptedItem = setHeader(item, CRYPTO_HEADERS.IS_ENCRYPTED);
    try {
        const sid = await session.sid;
        const client = new MailboxItemsClient(sid, folderUid);
        const part = encryptedItem.value.body.structure;
        const key = await getMyPrivateKey();
        const certificate = await getMyCertificate();
        const data = await client.fetch(encryptedItem.value.imapUid, part.address, part.encoding, part.mime);
        const content = await pkcs7.decrypt(data, key, certificate);
        if (content) {
            const parser = await new MimeParser(part.address).parse(content);
            const parts = parser.getParts();
            for (const part of parts) {
                const content = parser.getPartContent(part.address);
                savePart(folderUid, encryptedItem.value.imapUid, part, content);
            }
            encryptedItem.value.body.structure = parser.structure as MessageBody.Part;
        }
        return setHeader(encryptedItem, CRYPTO_HEADERS.DECRYPTED);
    } catch (error) {
        if (error instanceof SmimeErrors) {
            return setHeader(item, error.name);
        }
        throw error;
    }
}

export function encrypt() {
    return null;
}
export function verify() {
    return null;
}
export function sign() {
    return null;
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

export function setHeader(item: ItemValue<MailboxItem>, header: string) {
    const index = item.value.body.headers.findIndex(({ name }) => name === CRYPTO_HEADER_NAME);
    if (index === -1) {
        item.value.body.headers.push({ name: CRYPTO_HEADER_NAME, values: [header] });
    } else {
        item.value.body.headers[index].values.push(header);
    }
    return item;
}

export default {
    CRYPTO_HEADERS,
    isEncrypted,
    decrypt,
    encrypt,
    verify,
    setHeader,
    sign
};
