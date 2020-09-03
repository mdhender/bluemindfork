import { openDB, DBSchema, IDBPDatabase } from "idb";
import { MailFolder, UID, MailItem, ChangeSet } from "./entry";

export interface FolderSyncInfo {
    uid: string;
    fullName: string;
    version: number;
    minInterval: number;
}
interface MailSchema extends DBSchema {
    mail_folders: {
        key: UID;
        value: MailFolder;
        indexes: { "by-fullName": string };
    };
    folders_syncinfo: {
        key: string;
        value: FolderSyncInfo;
    };
    mail_items: {
        key: [string, number];
        value: MailItem;
        indexes: { "by-folderUid": string };
    };
    ids_stack: {
        key: [string, number];
        value: { internalId: number; folderUid: string };
        indexes: { "by-folderUid": string };
    };
}

export class MailDB {
    dbPromise: Promise<IDBPDatabase<MailSchema>>;
    constructor() {
        this.dbPromise = openDB<MailSchema>("webapp/mail", 2, {
            upgrade(db, oldVersion) {
                if (oldVersion < 2) {
                    for (const name of Object.values(db.objectStoreNames)) {
                        db.deleteObjectStore(name);
                    }
                }
                db.createObjectStore("folders_syncinfo", { keyPath: "uid" });
                db.createObjectStore("mail_items", { keyPath: ["folderUid", "internalId"] }).createIndex(
                    "by-folderUid",
                    "folderUid"
                );
                db.createObjectStore("ids_stack", { keyPath: ["folderUid", "internalId"] }).createIndex(
                    "by-folderUid",
                    "folderUid"
                );
                db.createObjectStore("mail_folders", { keyPath: "uid" }).createIndex("by-fullName", "value.fullName");
            }
        });
    }

    async getFolderSyncInfo(uid: string) {
        return (await this.dbPromise).get("folders_syncinfo", uid);
    }

    async getAllFolderSyncInfo() {
        return (await this.dbPromise).getAll("folders_syncinfo");
    }

    async isInFolderSyncInfo(uid: string) {
        const key = await (await this.dbPromise).getKey("folders_syncinfo", uid);
        return key !== undefined;
    }

    async updateFolderSyncInfo(folderSyncInfo: FolderSyncInfo) {
        const old = await this.getFolderSyncInfo(folderSyncInfo.uid);
        if (!old || old.version < folderSyncInfo.version) {
            return (await this.dbPromise).put("folders_syncinfo", folderSyncInfo);
        }
    }

    async putMailFolders(mailFolders: MailFolder[]) {
        const tx = (await this.dbPromise).transaction("mail_folders", "readwrite");
        const promises = mailFolders.map(mailFolder => tx.store.put(mailFolder));
        await Promise.all(promises);
        await tx.done;
    }

    async putMailItems(mailItems: MailItem[], folderUid: string) {
        const tx = (await this.dbPromise).transaction(["ids_stack", "mail_items"], "readwrite");
        await Promise.all(
            mailItems.map(({ internalId }) => tx.objectStore("ids_stack").delete([folderUid, internalId]))
        );
        await Promise.all(
            mailItems
                .map(mailItem => ({ ...mailItem, folderUid: folderUid }))
                .map(mailItem => tx.objectStore("mail_items").put(mailItem))
        );
        await tx.done;
    }

    async getAllMailItems(folderUid: string) {
        return (await this.dbPromise).getAllFromIndex("mail_items", "by-folderUid", folderUid);
    }

    async applyChangeset(changeSet: ChangeSet, folderUid: string, syncInfo: FolderSyncInfo) {
        const { created, updated, deleted, version } = changeSet;
        const tx = (await this.dbPromise).transaction(["folders_syncinfo", "ids_stack", "mail_items"], "readwrite");
        await Promise.all(created.map(({ id }) => tx.objectStore("ids_stack").put({ internalId: id, folderUid })));
        await Promise.all(updated.map(({ id }) => tx.objectStore("ids_stack").put({ internalId: id, folderUid })));
        await Promise.all(deleted.map(({ id }) => tx.objectStore("mail_items").delete([folderUid, id])));
        await tx.objectStore("folders_syncinfo").put({ ...syncInfo, version });
        await tx.done;
    }

    async getMixedMailItems(folderUid: string, ids: number[]) {
        return await ids.reduce(
            async (promise, id) => {
                const { remoteIds, localIds, localMailItems } = await promise;
                const localMailItem = await (await this.dbPromise).get("mail_items", [folderUid, id]);
                if (localMailItem === undefined) {
                    remoteIds.push(id);
                } else {
                    localMailItems.push(localMailItem);
                    localIds.push(id);
                }
                return { remoteIds, localIds, localMailItems };
            },
            Promise.resolve({
                localMailItems: [] as MailItem[],
                localIds: [] as number[],
                remoteIds: [] as number[]
            })
        );
    }
}
