import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import strategy from "./PartCacheStrategy";

export class PartApiProxy extends MailboxItemsClient {
    event?: FetchEvent;

    async fetch(): Promise<any> {
        return strategy.handle({ event: this.event as ExtendableEvent, request: this.event!.request });
    }
}
