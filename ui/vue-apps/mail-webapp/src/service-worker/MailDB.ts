import { openDB, DBSchema, IDBPDatabase, IDBPTransaction, StoreNames, StoreValue } from "idb";
import { MailFolder, MailItem, MailItemLight, OwnerSubscription, Reconciliation } from "./entry";
import { logger } from "./logger";

export type SyncOptionsType = "mail_folder" | "mail_item" | "owner_subscriptions";
export interface SyncOptions {
    uid: string;
    version: number;
    type: SyncOptionsType;
    pending?: boolean;
}
interface MailSchema extends DBSchema {
    mail_folders: {
        key: string;
        value: MailFolder;
        indexes: { "by-mailboxRoot": string };
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
    mail_item_light: {
        key: [string, number];
        value: MailItemLight;
        indexes: { "by-folderUid": string };
    };
    owner_subscriptions: {
        key: string;
        value: OwnerSubscription;
        indexes: { "by-type": string };
    };
}

export class MailDB {
    dbPromise: Promise<IDBPDatabase<MailSchema>>;
    constructor(userAtDomain: string) {
        const schemaVersion = 10;
        this.dbPromise = openDB<MailSchema>(`${userAtDomain}:webapp/mail`, schemaVersion, {
            upgrade(db, oldVersion) {
                logger.log(`[SW][DB] Upgrading from ${oldVersion} to ${schemaVersion}`);
                if (oldVersion < schemaVersion) {
                    logger.log("[SW][DB] Upgrading deleting existing object store");
                    for (const name of Object.values(db.objectStoreNames)) {
                        db.deleteObjectStore(name);
                    }
                }
                db.createObjectStore("sync_options", { keyPath: "uid" }).createIndex("by-type", "type");
                db.createObjectStore("mail_items", { keyPath: ["folderUid", "internalId"] }).createIndex(
                    "by-folderUid",
                    "folderUid"
                );
                db.createObjectStore("mail_folders", { keyPath: "uid" }).createIndex("by-mailboxRoot", "mailboxRoot");
                db.createObjectStore("mail_item_light", { keyPath: ["folderUid", "internalId"] }).createIndex(
                    "by-folderUid",
                    "folderUid"
                );
                db.createObjectStore("owner_subscriptions", { keyPath: "uid" }).createIndex(
                    "by-type",
                    "value.containerType"
                );
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
        if (actual === undefined || actual.version < syncOptions.version ||
            (actual.version === syncOptions.version && actual.pending !== syncOptions.pending)) {
            return (await this.dbPromise).put("sync_options", syncOptions);
        }
    }

    async isSubscribed(uid: string) {
        const key = await (await this.dbPromise).getKey("sync_options", uid);
        return key !== undefined;
    }

    async deleteSyncOptions(uid: string) {
        await (await this.dbPromise).delete("sync_options", uid);
    }

    async deleteOwnerSubscriptions(deletedIds: number[]) {
        const subscriptionUidsToDelete = (await this.getAllOwnerSubscriptions())
            .filter(ownerSubscription => deletedIds.includes(ownerSubscription.internalId))
            .map(ownerSubscription => ownerSubscription.uid)
        const tx = (await this.dbPromise).transaction("owner_subscriptions", "readwrite");
        tx.onerror = (event) => {
            logger.error("[SW][DB] Failed to delete owner subscriptions", deletedIds, event)
        }
        subscriptionUidsToDelete.forEach(uid => tx.objectStore("owner_subscriptions").delete(uid));
        await tx.done;
    }

    async deleteMailFolders(mailboxRoot: string, deletedIds: number[]) {
        const uids = (await this.getAllMailFolders(mailboxRoot))
            .filter(mailFolder => deletedIds.includes(mailFolder.internalId))
            .map(mailFolder => mailFolder.uid);
        const tx = (await this.dbPromise).transaction("mail_folders", "readwrite");
        uids.forEach(uid => tx.objectStore("mail_folders").delete(uid));
        await tx.done;
    }

    async putMailFolders(
        mailboxRoot: string,
        items: MailFolder[],
        optionalTransaction?: IDBPTransaction<MailSchema, StoreNames<MailSchema>[]>
    ) {
        await this.putItems(items.map(item => ({ ...item, mailboxRoot })), "mail_folders", optionalTransaction);
    }

    async putMailItems(items: MailItem[], optionalTransaction?: IDBPTransaction<MailSchema, StoreNames<MailSchema>[]>) {
        await this.putItems(items, "mail_items", optionalTransaction);
    }

    async putMailItemLight(
        items: MailItemLight[],
        optionalTransaction?: IDBPTransaction<MailSchema, StoreNames<MailSchema>[]>
    ) {
        await this.putItems(items, "mail_item_light", optionalTransaction);
    }

    async putOwnerSubscriptions(items: OwnerSubscription[], optionalTransaction?: IDBPTransaction<MailSchema, StoreNames<MailSchema>[]>) {
        await this.putItems(items, "owner_subscriptions", optionalTransaction);
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

    async getAllMailItemLight(folderUid: string) {
        return (await this.dbPromise).getAllFromIndex("mail_item_light", "by-folderUid", folderUid);
    }

    async getMailItems(folderUid: string, ids: number[]) {
        const tx = (await this.dbPromise).transaction(["mail_items"], "readonly");
        return Promise.all(ids.map(id => tx.objectStore("mail_items").get([folderUid, id])));
    }

    async getAllMailFolders(mailboxRoot: string) {
        return (await this.dbPromise).getAllFromIndex("mail_folders", "by-mailboxRoot", mailboxRoot);
    }

    async getOwnerSubscriptions(type: string) {
        return (await this.dbPromise).getAllFromIndex("owner_subscriptions", "by-type", type);
    }

    async getAllOwnerSubscriptions() {
        return (await this.dbPromise).getAll("owner_subscriptions");
    }

    async reconciliate(data: Reconciliation<MailItem>, syncOptions: SyncOptions) {
        const { items, uid, deletedIds } = data;
        const tx = (await this.dbPromise).transaction(["sync_options", "mail_items", "mail_item_light"], "readwrite");
        this.putMailItems(
            items.map(mailItem => ({ ...mailItem, folderUid: uid })),
            tx
        );
        this.putMailItemLight(
            items.map(mailItem => ({
                internalId: mailItem.internalId,
                flags: mailItem.flags,
                date: mailItem.value.body.date,
                folderUid: uid
            })),
            tx
        );
        deletedIds.map(id => tx.objectStore("mail_items").delete([uid, id]));
        deletedIds.map(id => tx.objectStore("mail_item_light").delete([uid, id]));
        await tx.objectStore("sync_options").put(syncOptions);
        await tx.done;
    }
}
