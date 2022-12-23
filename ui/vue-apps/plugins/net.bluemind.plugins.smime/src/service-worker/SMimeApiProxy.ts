import { ImapItemIdentifier, MailboxItem, MailboxItemsClient } from "@bluemind/backend.mail.api";
import { Ack, ItemValue } from "@bluemind/core.container.api";
import { hasToBeEncrypted, hasToBeSigned } from "../lib/helper";
import { logger } from "./environnment/logger";
import { decrypt, encrypt, isEncrypted, isSigned, sign, verify } from "./smime";

export default class SMimeApiProxy extends MailboxItemsClient {
    next?: (...args: Array<unknown>) => Promise<never>;
    async multipleGetById() {
        const items: Array<ItemValue<MailboxItem>> = await this.next!();

        for (let i = 0; i < items.length; i++) {
            try {
                if (isEncrypted(items[i])) {
                    items[i] = await decrypt(this.replicatedMailboxUid, items[i]);
                }
                if (isSigned(items[i])) {
                    items[i] = await verify(this.replicatedMailboxUid, items[i]);
                }
            } catch (e) {
                logger.error(e);
            }
        }

        return items;
    }
    async getCompleteById(): Promise<ItemValue<MailboxItem>> {
        let item: ItemValue<MailboxItem> = await this.next!();
        try {
            if (isEncrypted(item)) {
                item = await decrypt(this.replicatedMailboxUid, item);
            }
            if (isSigned(item)) {
                item = await verify(this.replicatedMailboxUid, item);
            }
        } catch (e) {
            logger.error(e);
        }
        return item;
    }
    async getForUpdate(): Promise<ItemValue<MailboxItem>> {
        let item: ItemValue<MailboxItem> = await this.next!();
        try {
            if (isEncrypted(item)) {
                item = await decrypt(this.replicatedMailboxUid, item);
            }
        } catch (e) {
            logger.error(e);
        }
        return item;
    }
    async create(item: MailboxItem): Promise<ImapItemIdentifier> {
        try {
            if (item.body.headers && hasToBeSigned(item.body.headers)) {
                item = await sign(item, this.replicatedMailboxUid);
            }
            if (item.body.headers && hasToBeEncrypted(item.body.headers)) {
                item = await encrypt(item, this.replicatedMailboxUid);
            }
        } catch (e) {
            logger.error(e);
        }
        return this.next!(item);
    }
    async updateById(id: number, item: MailboxItem): Promise<Ack> {
        try {
            if (item.body.headers && hasToBeSigned(item.body.headers)) {
                item = await sign(item, this.replicatedMailboxUid);
            }
            if (item.body.headers && hasToBeEncrypted(item.body.headers)) {
                item = await encrypt(item, this.replicatedMailboxUid);
            }
        } catch (e) {
            logger.error(e);
        }
        return await this.next!(id, item);
    }
}
