import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import Session from "./session";
import { logger } from "./logger";
import EmlParser from "./eml/EmlParser";

export default class SmimeHandler extends MailboxItemsClient {
    async multipleGetById() {
        const items = await this.next();
        for (let i = 0; i < items.length; i++) {
            if (isEncrypted(items[i])) {
                try {
                    items[i] = await decrypt(this.replicatedMailboxUid, items[i]);
                } catch (e) {
                    logger.error(e);
                }
            }
        }
        return items;
    }
}

function isEncrypted(message) {
    // FIXME : Random message
    return message.value.body.subject.trim().toLowerCase() === "crypted message";
}
async function decrypt(uid, item) {
    // FIXME FEATWEBML-2079: Need the BM client to be upgrader with a version using fetch instead of axion
    const response = await fetch(`/api/mail_items/${uid}/eml/${item.value.imapUid}`, {
        headers: { "x-bm-apikey": (await Session.instance()).infos.sid }
    });
    const eml = await response.text();
    item.value.body = await EmlParser.parseBodyStructure(item.value.body, eml);
    return item;
}
