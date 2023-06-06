import { ImapItemIdentifier, MailboxItem, MailboxItemsClient, MessageBody } from "@bluemind/backend.mail.api";
import { Ack, ItemValue } from "@bluemind/core.container.api";
import {
    hasEncryptionHeader,
    hasSignatureHeader,
    hasToBeEncrypted,
    hasToBeSigned,
    isEncrypted,
    removeSignatureFromStructure
} from "../lib/helper";
import decryptAndVerify from "./smime/decryptAndVerify";
import { getCacheKey, getGuid } from "./smime/cache/SMimePartCache";
import decrypt from "./smime/decrypt";
import encrypt from "./smime/encrypt";
import sign from "./smime/sign";

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
        if (cantRewriteMessage(item.body.headers || [])) {
            throw "signed or encrypted message can't be rewritten";
        }
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

function cantRewriteMessage(headers: MessageBody.Header[]) {
    return (
        !!headers.find(h => h.name === "X-BM-Rewrite") && (hasEncryptionHeader(headers) || hasSignatureHeader(headers))
    );
}
