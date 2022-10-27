import { MailboxItemsClient, ItemValue, MailboxItem, MessageBody } from "@bluemind/backend.mail.api";
import { MimeParser } from "@bluemind/mime";

import session from "./environnment/session";
import pkcs7 from "./pkcs7";
import { getMyCertificate, getMyPrivateKey } from "./pki";

// TODO : Support more complex smime arch (only a sub-part of the message could be encrypted)
export function isEncrypted(item: ItemValue<MailboxItem>): boolean {
    return item.value.body.structure.mime === "application/pkcs7-mime";
}
// TODO : Support more complex smime arch (multiple parts could be encrypted)
function getEncryptedPart(item: ItemValue<MailboxItem>): MessageBody.Part {
    return item.value.body.structure;
}

export async function decrypt(folderUid: string, item: ItemValue<MailboxItem>): Promise<ItemValue<MailboxItem>> {
    //FIXME: For now decrypt is a "mock" version.
    //TODO: Add a cache based on body guid
    const client = new MailboxItemsClient(await session.sid, folderUid);
    const part = getEncryptedPart(item);
    const data = await client.fetch(item.value.imapUid, part.address, part.encoding, part.mime);
    const key = await getMyPrivateKey();
    const certificate = await getMyCertificate();
    const content = await pkcs7.decrypt(data, key, certificate);
    if (content) {
        const parser = await new MimeParser(part.address).parse(content);

        savePart(folderUid, item.value.imapUid, parser.getPartContent("1.1"));
        item.value.body.structure = parser.structure as MessageBody.Part;
    }

    return item;
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
async function savePart(uid: string, imap: string, data?: ArrayBuffer) {
    const cache = await caches.open("part-cache");
    const request = new Request(
        `/api/mail_items/${uid}/part/${imap}/1.1?encoding=quoted-printable&mime=text%2Fplain&charset=utf-8`
    );
    return cache.put(request.url, new Response(data));
}
