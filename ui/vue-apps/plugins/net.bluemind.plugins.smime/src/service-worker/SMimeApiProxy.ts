import { ImapItemIdentifier, MailboxItem, MailboxItemsClient } from "@bluemind/backend.mail.api";
import { Ack, ItemValue } from "@bluemind/core.container.api";
import { removeSignatureFromStructure, hasToBeEncrypted, hasToBeSigned } from "../lib/helper";
import { getCacheKey } from "./smimePartCache";
import session from "./environnment/session";
import { decrypt, encrypt, isEncrypted, isSigned, sign, verify } from "./smime";

export default class SMimeApiProxy extends MailboxItemsClient {
    next?: (...args: Array<unknown>) => Promise<never>;
    event?: FetchEvent;

    async multipleGetById() {
        const items: Array<ItemValue<MailboxItem>> = await this.next!();

        for (let i = 0; i < items.length; i++) {
            items[i] = await decryptAndVerify(items[i], this.replicatedMailboxUid);
        }

        return items;
    }
    async getCompleteById(): Promise<ItemValue<MailboxItem>> {
        let item: ItemValue<MailboxItem> = await this.next!();
        item = await decryptAndVerify(item, this.replicatedMailboxUid);
        return item;
    }
    async getForUpdate(): Promise<ItemValue<MailboxItem>> {
        let item: ItemValue<MailboxItem> = await this.next!();
        if (isEncrypted(item.value.body.structure!)) {
            ({ item } = await decrypt(this.replicatedMailboxUid, item));
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
        const smimeCache = await caches.open("smime-part-cache");
        const key = getCacheKey(this.replicatedMailboxUid, imapUid, address);
        const cache = await smimeCache.match(key);
        if (cache) {
            return cache.blob();
        }
        return this.next!(imapUid, address, encoding, mime, charset, filename);
    }
}

async function decryptAndVerify(item: ItemValue<MailboxItem>, folderUid: string) {
    let adaptedItem = item;
    const client = new MailboxItemsClient(await session.sid, folderUid);
    let getEml = () => client.fetchComplete(item.value.imapUid!).then(blob => blob.text());
    if (isEncrypted(item.value.body.structure!)) {
        let decryptedContent: string;
        ({ item: adaptedItem, content: decryptedContent } = await decrypt(folderUid, item));
        getEml = () => Promise.resolve(decryptedContent);
    }
    if (isSigned(item.value.body.structure!)) {
        adaptedItem = await verify(item, getEml);
    }
    return adaptedItem;
}
