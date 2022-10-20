import { ItemValue, MailboxItem, MailboxItemsClient } from "@bluemind/backend.mail.api";
import { logger } from "./environnment/logger";
import { isEncrypted, decrypt } from "./smime";
export default class SMimeApiProxy extends MailboxItemsClient {
    async multipleGetById() {
        const items: Array<ItemValue<MailboxItem>> = await this.next();
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
