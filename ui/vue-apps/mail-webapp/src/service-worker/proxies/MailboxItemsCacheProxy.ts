import { CacheFirst } from "workbox-strategies";
import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import fetchStrategy from "./PartCacheStrategy";

const fetchCompleteStrategy = new CacheFirst({ cacheName: "eml-cache" });

export default class extends MailboxItemsClient {
    event?: FetchEvent;

    async fetch(): Promise<Blob> {
        const response = await fetchStrategy.handle({
            event: this.event as ExtendableEvent,
            request: this.event!.request
        });
        return response.blob();
    }

    async fetchComplete(): Promise<Blob> {
        const response = await fetchCompleteStrategy.handle({
            event: this.event as ExtendableEvent,
            request: this.event!.request
        });
        return response.blob();
    }
}
