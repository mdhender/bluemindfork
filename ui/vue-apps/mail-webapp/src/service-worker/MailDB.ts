import { openDB, DBSchema, IDBPDatabase, IDBPTransaction, StoreNames, StoreValue } from "idb";
import sortedIndexBy from "lodash.sortedindexby";
import { ContainerSubscriptionModel, ItemFlag, ItemValue } from "@bluemind/core.container.api";
import { MailboxFolder, MailboxItem } from "@bluemind/backend.mail.api";
import session from "@bluemind/session";

import { logger } from "./logger";

export type SyncOptionsType = "mail_folder" | "mail_item" | "owner_subscriptions";

export interface SyncOptions {
    uid: string;
    version: number;
    type: SyncOptionsType;
    pending?: boolean;
}

export type MailItemLight = {
    internalId: number;
    flags: ItemFlag[];
    date: number;
    subject?: string;
    size: number;
    sender?: string;
};

interface Reconciliation<T> {
    uid: string;
    items: T[];
    deletedIds: number[];
}
interface MailSchema extends DBSchema {
    mail_folders: {
        key: string;
        value: ItemValue<MailboxFolder>;
        indexes: { "by-mailboxRoot": string };
    };
    sync_options: {
        key: string;
        value: SyncOptions;
        indexes: { "by-type": string };
    };
    mail_items: {
        key: [string, number];
        value: ItemValue<MailboxItem>;
        indexes: { "by-folderUid": string };
    };
    mail_item_light: {
        key: string;
        value: MailItemLight[];
    };
    owner_subscriptions: {
        key: string;
        value: ItemValue<ContainerSubscriptionModel>;
        indexes: { "by-type": string };
    };
}

type MailDBTransaction = IDBPTransaction<MailSchema, StoreNames<MailSchema>[], IDBTransactionMode>;

export interface MailDB {
    getSyncOptions(uid: string): Promise<SyncOptions | undefined>;
    updateSyncOptions(options: SyncOptions): Promise<string | undefined>;
    isSubscribed(uid: string): Promise<boolean>;
    deleteSyncOptions(uid: string): Promise<void>;
    deleteOwnerSubscriptions(deletedIds: number[]): Promise<void>;
    deleteMailFolders(mailboxRoot: string, deletedIds: number[]): Promise<void>;
    putMailFolders(mailboxRoot: string, items: ItemValue<MailboxFolder>[], tx?: MailDBTransaction): Promise<void>;
    putOwnerSubscriptions(items: ItemValue<ContainerSubscriptionModel>[], tx?: MailDBTransaction): Promise<void>;
    getAllMailItemLight(folderUid: string, tx?: MailDBTransaction): Promise<MailItemLight[]>;
    getMailItems(folderUid: string, ids: number[]): Promise<(ItemValue<MailboxItem> | undefined)[]>;
    getAllMailFolders(mailboxRoot: string): Promise<ItemValue<MailboxFolder>[]>;
    getOwnerSubscriptions(type: string): Promise<ItemValue<ContainerSubscriptionModel>[]>;
    getAllOwnerSubscriptions(): Promise<ItemValue<ContainerSubscriptionModel>[]>;
    reconciliate(data: Reconciliation<ItemValue<MailboxItem>>, syncOptions: SyncOptions): Promise<void>;
    setMailItemLight(
        folderUid: string,
        items: ItemValue<MailboxItem>[],
        deleted: number[],
        tx?: MailDBTransaction
    ): Promise<void>;
}

export class MailDBImpl implements MailDB {
    dbPromise: Promise<IDBPDatabase<MailSchema>>;
    constructor(userAtDomain: string) {
        this.dbPromise = this.openDB(userAtDomain);
    }
    private async openDB(userAtDomain: string): Promise<IDBPDatabase<MailSchema>> {
        const schemaVersion = 11;
        return await openDB<MailSchema>(`${userAtDomain}:webapp/mail`, schemaVersion, {
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
                db.createObjectStore("mail_item_light");
                db.createObjectStore("owner_subscriptions", { keyPath: "uid" }).createIndex(
                    "by-type",
                    "value.containerType"
                );
            },
            blocking: async () => {
                (await this.dbPromise).close();
                this.dbPromise = this.openDB(userAtDomain);
            }
        });
    }

    private async getTx<StoreName extends StoreNames<MailSchema>>(
        storeName: StoreName,
        mode: IDBTransactionMode,
        tx?: IDBPTransaction<MailSchema, StoreName[], IDBTransactionMode>
    ): Promise<IDBPTransaction<MailSchema, StoreName[], IDBTransactionMode>> {
        return tx || (await this.dbPromise).transaction([storeName], mode);
    }

    private async putItems<T extends StoreValue<MailSchema, StoreName>, StoreName extends StoreNames<MailSchema>>(
        items: T[],
        storeName: StoreName,
        optionalTransaction?: IDBPTransaction<MailSchema, StoreName[], IDBTransactionMode>
    ) {
        const tx = optionalTransaction || (await this.dbPromise).transaction(storeName, "readwrite");
        await Promise.all(items.map(item => tx.objectStore(storeName).put?.(item)));
        await tx.done;
    }

    private async putMailItems(items: ItemValue<MailboxItem>[], tx?: MailDBTransaction) {
        await this.putItems(items, "mail_items", tx);
    }

    async getSyncOptions(uid: string) {
        return (await this.dbPromise).get("sync_options", uid);
    }

    async updateSyncOptions(syncOptions: SyncOptions) {
        const actual = await this.getSyncOptions(syncOptions.uid);
        if (
            actual === undefined ||
            actual.version < syncOptions.version ||
            (actual.version === syncOptions.version && actual.pending !== syncOptions.pending)
        ) {
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
            .filter(ownerSubscription => deletedIds.includes(ownerSubscription.internalId as number))
            .map(ownerSubscription => ownerSubscription.uid);
        const tx = (await this.dbPromise).transaction("owner_subscriptions", "readwrite");
        tx.onerror = event => {
            logger.error("[SW][DB] Failed to delete owner subscriptions", deletedIds, event);
        };
        subscriptionUidsToDelete.forEach(uid => tx.objectStore("owner_subscriptions").delete(uid as string));
        await tx.done;
    }

    async deleteMailFolders(mailboxRoot: string, deletedIds: number[]) {
        const uids = (await this.getAllMailFolders(mailboxRoot))
            .filter(({ internalId }) => deletedIds.includes(internalId as number))
            .map(({ uid }) => uid as string);
        const tx = (await this.dbPromise).transaction("mail_folders", "readwrite");
        uids.forEach(uid => tx.objectStore("mail_folders").delete(uid));
        await tx.done;
    }

    async putMailFolders(mailboxRoot: string, items: ItemValue<MailboxFolder>[], tx?: MailDBTransaction) {
        await this.putItems(
            items.map(item => ({ ...item, mailboxRoot })),
            "mail_folders",
            tx
        );
    }

    async putOwnerSubscriptions(items: ItemValue<ContainerSubscriptionModel>[], tx?: MailDBTransaction) {
        await this.putItems(items, "owner_subscriptions", tx);
    }

    async getAllMailItemLight(folderUid: string, tx?: MailDBTransaction): Promise<MailItemLight[]> {
        const _tx = await this.getTx("mail_item_light", "readonly", tx);
        const store = _tx.objectStore("mail_item_light");
        return (await store.get(folderUid)) || [];
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

    async reconciliate(data: Reconciliation<ItemValue<MailboxItem>>, syncOptions: SyncOptions) {
        const { items, uid, deletedIds } = data;
        const tx = (await this.dbPromise).transaction(["sync_options", "mail_items", "mail_item_light"], "readwrite");
        this.putMailItems(
            items.map(mailItem => ({ ...mailItem, folderUid: uid })),
            tx
        );
        this.setMailItemLight(uid, items, deletedIds, tx);
        deletedIds.map(id => tx.objectStore("mail_items").delete([uid, id]));
        await tx.objectStore("sync_options").put(syncOptions);
        await tx.done;
    }
    async setMailItemLight(
        folderUid: string,
        items: ItemValue<MailboxItem>[],
        deleted: number[],
        optTx?: IDBPTransaction<MailSchema, StoreNames<MailSchema>[], IDBTransactionMode>
    ) {
        const lights: Array<MailItemLight> = await this.getAllMailItemLight(folderUid, optTx);
        deleted.forEach(id => {
            const dummy = { internalId: id, flags: [], date: 0, subject: "", size: 0, sender: "" };
            const index = sortedIndexBy(lights, dummy, "internalId");
            if (lights[index]?.internalId === id) {
                lights.splice(index, 1);
            }
        });
        items.forEach(mail => {
            const light = toLight(mail);
            const index = sortedIndexBy(lights, light, "internalId");
            if (lights[index]?.internalId === light.internalId) {
                lights.splice(index, 1, light);
            } else {
                lights.splice(index, 0, light);
            }
        });
        const tx = (await this.dbPromise).transaction("mail_item_light", "readwrite");

        await tx.objectStore("mail_item_light").put(lights, folderUid);
        await tx.done;
    }
}

function toLight(mail: ItemValue<MailboxItem>): MailItemLight {
    return {
        internalId: mail.internalId as number,
        flags: mail.flags as ItemFlag[],
        date: mail.value.body.date as number,
        subject: mail.value.body.subject?.toLowerCase().replace(/^(\W*|re\s*:)*/i, ""),
        size: mail.value.body.size as number,
        sender: mail.value.body.recipients?.find(recipient => recipient.kind === "Originator")?.address?.toLowerCase()
    };
}

let implementation: MailDB | null = null;
async function instance(): Promise<MailDB> {
    if (!implementation) {
        const name = `user.${await session.userId}@${(await session.domain).replace(/\./g, "_")}`;
        implementation = new MailDBImpl(name);
    }
    return implementation;
}

const db: MailDB = {
    getSyncOptions: uid => instance().then(db => db.getSyncOptions(uid)),
    updateSyncOptions: options => instance().then(db => db.updateSyncOptions(options)),
    isSubscribed: uid => instance().then(db => db.isSubscribed(uid)),
    deleteSyncOptions: uid => instance().then(db => db.deleteSyncOptions(uid)),
    deleteOwnerSubscriptions: deletedIds => instance().then(db => db.deleteOwnerSubscriptions(deletedIds)),
    deleteMailFolders: (mailboxRoot, deletedIds) =>
        instance().then(db => db.deleteMailFolders(mailboxRoot, deletedIds)),
    putMailFolders: (mailboxRoot, items, tx) => instance().then(db => db.putMailFolders(mailboxRoot, items, tx)),
    putOwnerSubscriptions: (items, tx) => instance().then(db => db.putOwnerSubscriptions(items, tx)),
    getAllMailItemLight: (folderUid, tx) => instance().then(db => db.getAllMailItemLight(folderUid, tx)),
    getMailItems: (folderUid, ids) => instance().then(db => db.getMailItems(folderUid, ids)),
    getAllMailFolders: mailboxRoot => instance().then(db => db.getAllMailFolders(mailboxRoot)),
    getOwnerSubscriptions: type => instance().then(db => db.getOwnerSubscriptions(type)),
    getAllOwnerSubscriptions: () => instance().then(db => db.getAllOwnerSubscriptions()),
    reconciliate: (data, syncOptions) => instance().then(db => db.reconciliate(data, syncOptions)),
    setMailItemLight: (folderUid, items, deleted, tx) =>
        instance().then(db => db.setMailItemLight(folderUid, items, deleted, tx))
};

export default db;
