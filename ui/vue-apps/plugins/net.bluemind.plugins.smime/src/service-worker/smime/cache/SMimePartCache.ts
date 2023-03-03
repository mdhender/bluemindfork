import { MessageBody } from "@bluemind/backend.mail.api";
import db from "./SMimeBodyDB";

export async function getCacheKey(address: string, guid?: string, folderUid?: string): Promise<string> {
    if (isImapAddress(address)) {
        return `${guid}/${address}`;
    }
    return `${folderUid}/${address}`;
}

function isImapAddress(address: string) {
    const regex = /^[0-9.]*$/;
    return address === "TEXT" || address === "HEADER" || regex.test(address);
}

export async function checkParts(guid: string, parts: MessageBody.Part[]) {
    const cache: Cache = await caches.open("smime-part-cache");
    for (const part of parts) {
        const key = await getCacheKey(part.address!, guid);
        if (!(await cache.match(key))) {
            return false;
        }
    }
    return true;
}

export function getGuid(folderUid: string, imapUid: number): Promise<string | undefined> {
    return db.getGuid(folderUid, +imapUid);
}
