import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import strategy from "./PartCacheStrategy";

export class PartApiProxy extends MailboxItemsClient {
    event?: FetchEvent;

    async fetch(): Promise<Blob> {
        const response = await strategy.handle({ event: this.event!, request: this.event!.request });
        return response.blob();
    }
}
