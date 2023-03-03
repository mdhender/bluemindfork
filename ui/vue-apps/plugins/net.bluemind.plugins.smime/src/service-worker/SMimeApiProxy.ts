import { ImapItemIdentifier, MailboxItem, MailboxItemsClient } from "@bluemind/backend.mail.api";
import { Ack, ItemValue } from "@bluemind/core.container.api";
import { removeSignatureFromStructure, hasToBeEncrypted, hasToBeSigned } from "../lib/helper";
import { getCacheKey, getGuid } from "./smime/cache/SMimePartCache";
import { decrypt, decryptAndVerify, encrypt, isEncrypted, sign } from "./smime";

export default class SMimeApiProxy extends MailboxItemsClient {
    next?: (...args: Array<unknown>) => Promise<never>;
    event?: FetchEvent;

    async multipleGetById() {
        const items: Array<ItemValue<MailboxItem>> = await this.next!();
        return decryptAndVerify(items, this.replicatedMailboxUid);
    }
    async getCompleteById(): Promise<ItemValue<MailboxItem>> {
        let item: ItemValue<MailboxItem> = await this.next!();
        [item] = await decryptAndVerify([item], this.replicatedMailboxUid);
        return item;
    }
    async getForUpdate(): Promise<ItemValue<MailboxItem>> {
        const item: ItemValue<MailboxItem> = await this.next!();
        if (isEncrypted(item.value.body.structure!)) {
            const { body } = await decrypt(this.replicatedMailboxUid, item);
            item.value.body = body;
        }
        // Filter the attachment signature for draft
        item.value.body.structure = removeSignatureFromStructure(item.value.body.structure);
        return item;
    }
    async create(item: MailboxItem): Promise<ImapItemIdentifier> {
        if (item.body.headers && hasToBeSigned(item.body.headers)) {
            item = await sign(item, this.replicatedMailboxUid);
        }
        if (item.body.headers && hasToBeEncrypted(item.body.headers)) {
            item = await encrypt(item, this.replicatedMailboxUid);
        }
        return this.next!(item);
    }
    async updateById(id: number, item: MailboxItem): Promise<Ack> {
        if (item.body.headers && hasToBeSigned(item.body.headers)) {
            item = await sign(item, this.replicatedMailboxUid);
        }
        if (item.body.headers && hasToBeEncrypted(item.body.headers)) {
            item = await encrypt(item, this.replicatedMailboxUid);
        }
        return this.next!(id, item);
    }
    async fetch(
        imapUid: number,
        address: string,
        encoding: string,
        mime: string,
        charset: string,
        filename: string
    ): Promise<Blob> {
        const guid = await getGuid(this.replicatedMailboxUid, imapUid);

        const smimeCache = await caches.open("smime-part-cache");
        const key = await getCacheKey(address, guid, this.replicatedMailboxUid);
        const cache = await smimeCache.match(key);
        if (cache) {
            return cache.blob();
        }
        return this.next!(imapUid, address, encoding, mime, charset, filename);
    }
}
