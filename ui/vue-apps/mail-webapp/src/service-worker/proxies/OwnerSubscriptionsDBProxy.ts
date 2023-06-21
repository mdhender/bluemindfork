/* eslint-disable @typescript-eslint/no-non-null-assertion */

import { ContainerSubscriptionModel, ItemValue, OwnerSubscriptionsClient } from "@bluemind/core.container.api";
import db from "../MailDB";

export default class extends OwnerSubscriptionsClient {
    next?: (...args: Array<unknown>) => Promise<never>;
    async list(): Promise<ItemValue<ContainerSubscriptionModel>[]> {
        try {
            if (await db.isSubscribed(`${this.ownerUid}@${this.domainUid}.subscriptions`)) {
                return db.getAllOwnerSubscriptions();
            }
            return this.next!();
        } catch (error) {
            console.debug(error);
        }
        return this.next!();
    }
}
