import { openDB, DBSchema, IDBPDatabase } from "idb";
import { logger } from "./logger";
import { StringArraySupportOption } from "prettier";

interface EnvironmentSchema extends DBSchema {
    system: {
        key: string;
        value: { key: string; value: string };
    };
}

const VERSION = 1;

interface EnvironmentDB {
    setMailboxCopyGuid(uid: string): Promise<void>;
    getMailboxCopyGuid(): Promise<string | undefined>;
}
export class EnvironmentDBImpl implements EnvironmentDB {
    db: Promise<IDBPDatabase<EnvironmentSchema>>;
    constructor() {
        this.db = this.openDB();
    }
    private async openDB() {
        return openDB<EnvironmentSchema>("environment", VERSION, {
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

    async setMailboxCopyGuid(uid: string) {
        logger.log(`[SW][DB] Initialize environment mailboxCopyGuid to ${uid}.`);
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

let implementation: EnvironmentDB | null = null;
async function instance(): Promise<EnvironmentDB> {
    if (!implementation) {
        implementation = new EnvironmentDBImpl();
    }
    return implementation;
}

const db: EnvironmentDB = {
    setMailboxCopyGuid: uid => instance().then(db => db.setMailboxCopyGuid(uid)),
    getMailboxCopyGuid: () => instance().then(db => db.getMailboxCopyGuid())
};

export default db;
