import { MailboxItem, MessageBody } from "@bluemind/backend.mail.api";
import { ItemValue } from "@bluemind/core.container.api";
import { EmlParser } from "@bluemind/mime";
import { fetchRequest, dispatchFetch } from "@bluemind/service-worker-utils";
import { CRYPTO_HEADERS, ENCRYPTED_HEADER_NAME } from "../../lib/constants";
import { SmimeErrors } from "../../lib/exceptions";
import { addHeaderValue, resetHeader } from "../../lib/helper";
import session from "../environnment/session";
import pkcs7 from "../pkcs7";
import { checkCertificate, getMyCertificate, getMyPrivateKey } from "../pki";
import { DecryptResult } from "../../types";
import { getCacheKey, getGuid } from "./cache/SMimePartCache";

export default async function (folderUid: string, item: ItemValue<MailboxItem>): Promise<DecryptResult> {
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
        const parser = await new EmlParser().parse(content);
        const parts = parser.getParts();

        const savePartsPromises = [];
        for (const p of parts) {
            const partContent = parser.getPartContent(p.address!);
            const promise = savePart(folderUid, imapUid!, p, partContent);
            savePartsPromises.push(promise);
        }
        await Promise.all(savePartsPromises);

        body.preview = parser.body.preview;
        body.structure = parser.body.structure as MessageBody.Part;
        body.headers = addHeaderValue(body?.headers, ENCRYPTED_HEADER_NAME, CRYPTO_HEADERS.OK);
    } catch (error: unknown) {
        const errorCode = error instanceof SmimeErrors ? error.code : CRYPTO_HEADERS.UNKNOWN;
        body.headers = addHeaderValue(body.headers, ENCRYPTED_HEADER_NAME, errorCode);
    }
    return { body, content };
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
