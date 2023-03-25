import { CacheFirst } from "workbox-strategies";
import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import partStrategy from "./PartCacheStrategy";

export default class extends MailboxItemsClient {
    event?: FetchEvent;

    async fetch(): Promise<Blob> {
        const response = await partStrategy.handle({
            event: this.event as ExtendableEvent,
            request: this.event!.request
        });
        return response.blob();
    }

    async fetchComplete(): Promise<Blob> {
        const strategy = new CacheFirst({ cacheName: "eml-cache" });
        const response = await strategy.handle({
            event: this.event as ExtendableEvent,
            request: this.event!.request
        });
        return response.blob();
    }
}
