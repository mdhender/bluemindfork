/* eslint-disable @typescript-eslint/no-non-null-assertion */
import pRetry from "p-retry";
import { MailboxFolder, MailboxFoldersClient } from "@bluemind/backend.mail.api";
import { ItemValue } from "@bluemind/core.container.api";
import { logger } from "../logger";
import SessionLegacy from "../session";

export default class extends MailboxFoldersClient {
    next?: (...args: Array<unknown>) => Promise<never>;
    async all(): Promise<ItemValue<MailboxFolder>[]> {
        try {
            const uid = `${this.mailboxRoot}@${this.partition}`;
            return await retry(async () => {
                const { db } = await SessionLegacy.instance();
                if (await db.isSubscribed(uid)) {
                    return db.getAllMailFolders(this.mailboxRoot);
                }
                return this.next!();
            });
        } catch (error) {
            console.debug(error);
        }
        return this.next!();
    }
}

async function retry<T>(fn: () => Promise<T>): Promise<T> {
    const wrapToThrowErrorOnFailure = <T>(fnToWrap: () => Promise<T>): (() => Promise<T>) => {
        return () =>
            fnToWrap().catch((error: any) => {
                logger.log("catching an error", error);
                throw new Error(error);
            });
    };
    return pRetry(wrapToThrowErrorOnFailure(fn), { retries: 1, onFailedAttempt: () => SessionLegacy.clear() });
}
