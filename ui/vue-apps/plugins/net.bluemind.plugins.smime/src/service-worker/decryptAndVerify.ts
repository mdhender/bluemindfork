import { MailboxItem, MessageBody } from "@bluemind/backend.mail.api";
import { ItemValue } from "@bluemind/core.container.api";
import { fetchCompleteRequest, dispatchFetch } from "@bluemind/service-worker-utils";
import { logger } from "./environnment/logger";
import session from "./environnment/session";
import { decrypt, isEncrypted, isSigned, verify } from "./smime";
import { invalidate, getBody, setReference } from "./smime/cache/BodyCache";

export default async function decryptAndVerify(items: ItemValue<MailboxItem>[], folderUid: string) {
    for (let i = 0; i < items.length; i++) {
        try {
            if (isSMime(items[i].value.body.structure!)) {
                await setReference(folderUid, items[i].value.imapUid!, items[i].value.body!.guid!);
                items[i].value.body = await getBody(items[i].value.body.guid!, () =>
                    decryptAndVerifyImpl(items[i], folderUid)
                );
            }
        } catch (error) {
            logger.error(error);
        }
    }
    invalidate();
    return items;
}

async function decryptAndVerifyImpl(item: ItemValue<MailboxItem>, folderUid: string): Promise<MessageBody> {
    let body = item.value.body;
    let getEml = async () => {
        const request = fetchCompleteRequest(await session.sid, folderUid, item.value.imapUid!);
        const response = await dispatchFetch(request);
        return response.text();
    };
    if (isEncrypted(body.structure!)) {
        let decryptedContent: string;
        ({ body, content: decryptedContent } = await decrypt(folderUid, item));
        getEml = () => Promise.resolve(decryptedContent);
    }
    if (isSigned(body.structure!)) {
        body = await verify(item, getEml);
    }
    return body;
}

function isSMime(item: MessageBody.Part) {
    return isEncrypted(item) || isSigned(item);
}
