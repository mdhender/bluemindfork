import { MessageBody } from "@bluemind/backend.mail.api";
import { hasEncryptionHeader, hasSignatureHeader, isDecrypted, isVerified } from "../../../lib/helper";
import extractLeafs from "./extractLeafs";
import db from "./SMimeBodyDB";
import { checkParts } from "./SMimePartCache";

export async function getBody(guid: string, mappingFunction: () => Promise<MessageBody>): Promise<MessageBody> {
    let body = await db.getBody(guid);
    const hasCachedParts = isDecrypted(body?.headers); // Only decrypt function cache smime parts
    let isPartCacheValid = body && !hasCachedParts;

    if (body && !isPartCacheValid) {
        try {
            const parts = extractLeafs(body.structure!);
            isPartCacheValid = await checkParts(guid, parts);
        } catch {
            isPartCacheValid = false;
        }
    }
    if (!isPartCacheValid) {
        body = await mappingFunction();
        if (isSuccess(body)) {
            await db.setBody(guid, body);
        }
    }
    return body!;
}
export function setReference(folderUid: string, imapUid: number, guid: string): Promise<void> {
    return db.setGuid(folderUid, imapUid, guid);
}
export function invalidate(): Promise<void> {
    const today = new Date();
    const timestamp = new Date(today.setDate(today.getDate() - 7)).getTime();
    return db.invalidate(timestamp);
}

function isSuccess(body: MessageBody): boolean {
    return (
        hasSignatureHeader(body.headers) === isVerified(body.headers) &&
        hasEncryptionHeader(body.headers) === isDecrypted(body.headers)
    );
}
