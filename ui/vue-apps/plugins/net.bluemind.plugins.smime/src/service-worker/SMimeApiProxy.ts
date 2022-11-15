import { MailboxItem, MailboxItemsClient } from "@bluemind/backend.mail.api";
import { ItemValue } from "@bluemind/core.container.api";
import { logger } from "./environnment/logger";
import { decrypt, isEncrypted, isSigned, verify } from "./smime";

export default class SMimeApiProxy extends MailboxItemsClient {
    next?: (...args: Array<unknown>) => Promise<never>;
    async multipleGetById() {
        const items: Array<ItemValue<MailboxItem>> = await this.next!();
        try {
            for (let i = 0; i < items.length; i++) {
                if (isEncrypted(items[i])) {
                    items[i] = await decrypt(this.replicatedMailboxUid, items[i]);
                }
                if (isSigned(items[i])) {
                    items[i] = await verify(this.replicatedMailboxUid, items[i]);
                }
            }
        } catch (e) {
            logger.error(e);
        }
        return items;
    }
}
