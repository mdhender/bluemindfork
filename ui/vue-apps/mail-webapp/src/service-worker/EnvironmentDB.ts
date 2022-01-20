import { openDB, DBSchema, IDBPDatabase } from "idb";
import { logger } from "./logger";

interface EnvironmentSchema extends DBSchema {
    system: {
        key: string;
        value: { key: string; value: any };
    };
}

const VERSION: number = 1;

export class EnvironmentDB {
    db: Promise<IDBPDatabase<EnvironmentSchema>>;
    constructor() {
        this.db = this.openDB();
    }
    private async openDB() {
        return await openDB<EnvironmentSchema>("environment", VERSION, {
            upgrade(db, oldVersion) {
                logger.log(`[SW][DB] Upgrading from ${oldVersion} to ${VERSION}`);
                if (oldVersion < VERSION) {
                    logger.log("[SW][DB] Upgrading deleting existing object store");
                    for (const name of Object.values(db.objectStoreNames)) {
                        db.deleteObjectStore(name);
                    }
                }
                db.createObjectStore("system", { keyPath: "key" });
            },
            blocking: async () => {
                (await this.db).close();
                this.db = this.openDB();
            }
        });
    }

    async setMailboxCopyGuid(uid: String) {
        logger.log(`[SW][DB] Initialize environment mailboxCopyGuid}.`);
        await (await this.db).put("system", { key: "mailboxCopyGuid", value: uid });
    }

    async getMailboxCopyGuid() {
        const data = await (await this.db).get("system", "mailboxCopyGuid");
        if (data === undefined) {
            return undefined;
        }
        return data.value;
    }
}
