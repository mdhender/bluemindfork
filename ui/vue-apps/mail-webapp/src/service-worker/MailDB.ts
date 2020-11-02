import { openDB, DBSchema, IDBPDatabase, IDBPTransaction, StoreNames, StoreValue } from "idb";
import { MailFolder, MailItem, Reconciliation } from "./entry";
import { userAtDomain, sessionInfos } from "./MailAPI";

type SyncOptionsType = "mail_folder" | "mail_item";
export interface SyncOptions {
    uid: string;
    version: number;
    type: SyncOptionsType;
}
interface MailSchema extends DBSchema {
    mail_folders: {
        key: string;
        value: MailFolder;
        indexes: { "by-fullName": string };
    };
    sync_options: {
        key: string;
        value: SyncOptions;
        indexes: { "by-type": string };
    };
    mail_items: {
        key: [string, number];
        value: MailItem;
        indexes: { "by-folderUid": string };
    };
}

export const maildb = (function () {
    let instance: MailDB | null;

    async function init() {
        return new MailDB(userAtDomain(await sessionInfos.getInstance()));
    }
    return {
        getInstance: async function () {
            if (!instance) {
                instance = await init();
            }
            return instance;
        }
    };
})();

class MailDB {
    dbPromise: Promise<IDBPDatabase<MailSchema>>;
    constructor(userAtDomain: string) {
        const schemaVersion = 6;
        this.dbPromise = openDB<MailSchema>(`${userAtDomain}:webapp/mail`, schemaVersion, {
            upgrade(db, oldVersion) {
                if (oldVersion < schemaVersion) {
                    for (const name of Object.values(db.objectStoreNames)) {
                        db.deleteObjectStore(name);
                    }
                }
                db.createObjectStore("sync_options", { keyPath: "uid" }).createIndex("by-type", "type");
                db.createObjectStore("mail_items", { keyPath: ["folderUid", "internalId"] }).createIndex(
                    "by-folderUid",
                    "folderUid"
                );
                db.createObjectStore("mail_folders", { keyPath: "uid" }).createIndex("by-fullName", "value.fullName");
            }
        });
    }

    async getSyncOptions(uid: string) {
        return (await this.dbPromise).get("sync_options", uid);
    }

    async getAllSyncOptions(type?: SyncOptionsType) {
        if (type) {
            return (await this.dbPromise).getAllFromIndex("sync_options", "by-type", type);
        }
        return (await this.dbPromise).getAll("sync_options");
    }

    async updateSyncOptions(syncOptions: SyncOptions) {
        const actual = await this.getSyncOptions(syncOptions.uid);
        if (actual === undefined || actual.version < syncOptions.version) {
            return (await this.dbPromise).put("sync_options", syncOptions);
        }
    }

    async isSubscribed(uid: string) {
        const key = await (await this.dbPromise).getKey("sync_options", uid);
        return key !== undefined;
    }

    async putMailFolders(
        items: MailFolder[],
        optionalTransaction?: IDBPTransaction<MailSchema, StoreNames<MailSchema>[]>
    ) {
        await this.putItems(items, "mail_folders", optionalTransaction);
    }

    async putMailItems(items: MailItem[], optionalTransaction?: IDBPTransaction<MailSchema, StoreNames<MailSchema>[]>) {
        await this.putItems(items, "mail_items", optionalTransaction);
    }

    async putItems<T extends StoreValue<MailSchema, StoreName>, StoreName extends StoreNames<MailSchema>>(
        items: T[],
        storeName: StoreName,
        optionalTransaction?: IDBPTransaction<MailSchema, StoreName[]>
    ) {
        const tx = optionalTransaction || (await this.dbPromise).transaction(storeName, "readwrite");
        await Promise.all(items.map(item => tx.objectStore(storeName).put(item)));
        await tx.done;
    }

    async getAllMailItems(folderUid: string) {
        return (await this.dbPromise).getAllFromIndex("mail_items", "by-folderUid", folderUid);
    }

    async getMailItems(folderUid: string, ids: number[]) {
        const tx = (await this.dbPromise).transaction(["mail_items"], "readonly");
        return Promise.all(ids.map(id => tx.objectStore("mail_items").get([folderUid, id])));
    }

    async getAllMailFolders() {
        return (await this.dbPromise).getAll("mail_folders");
    }

    async reconciliate(data: Reconciliation<MailItem>, syncOptions: SyncOptions) {
        const { items, uid, deletedIds } = data;
        const tx = (await this.dbPromise).transaction(["sync_options", "mail_items"], "readwrite");
        this.putMailItems(
            items.map(mailItem => ({ ...mailItem, folderUid: uid })),
            tx
        );
        await Promise.all(deletedIds.map(id => tx.objectStore("mail_items").delete([uid, id])));
        tx.objectStore("sync_options").put(syncOptions);
        tx.done;
    }
}
