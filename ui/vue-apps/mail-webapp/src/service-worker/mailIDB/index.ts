import { openDB, DBSchema, IDBPDatabase } from "idb";
import { MailFolder, UID, MailItem, ChangeSet } from "../api/entry";

interface SyncedFolder {
    uid: UID;
    version: number;
    minInterval: number;
}

interface MailSchema extends DBSchema {
    mail_folders: {
        key: UID;
        value: MailFolder;
        indexes: { fullName: string };
    };
    synced_mail_folders: {
        key: UID;
        value: SyncedFolder;
    };
    mail_items: {
        key: number;
        value: MailItem;
    };
}

export async function openWebAppMail() {
    return openDB<MailSchema>("webapp/mail", 1, {
        upgrade(db) {
            if (!db.objectStoreNames.contains("mail_folders")) {
                db.createObjectStore("mail_folders", { keyPath: "uid" }).createIndex("fullName", "value.fullName");
            }
            if (!db.objectStoreNames.contains("synced_mail_folders")) {
                db.createObjectStore("synced_mail_folders");
            }
            if (!db.objectStoreNames.contains("mail_items")) {
                db.createObjectStore("mail_items", { keyPath: "internalId" });
            }
        }
    });
}

export class MailIDB {
    dbPromise: Promise<IDBPDatabase<MailSchema>>;
    constructor() {
        this.dbPromise = openWebAppMail();
    }

    //FIXME: storeMailFolders & storeMailItems differ only by type, can we factorize them?
    async storeMailFolders(mailFolders: Array<MailFolder>) {
        const tx = (await this.dbPromise).transaction("mail_folders", "readwrite");
        await Promise.all(
            mailFolders.map(folder => {
                tx.store.put(folder);
            })
        );
        await tx.done;
    }

    async storeMailItems(mailItems: Array<MailItem>): Promise<void> {
        const tx = (await this.dbPromise).transaction("mail_items", "readwrite");
        await Promise.all(
            mailItems.map(mail => {
                tx.store.put(mail);
            })
        );
        await tx.done;
    }

    async markAsSynced(folder: Pick<MailFolder, "uid">, minInterval: number = 24 * 60 * 60 * 1000) {
        const alreadyMarked = await this.getSyncedFolder(folder);
        if (!alreadyMarked) {
            await this.createNewSyncedFolder(folder, 0, minInterval);
        }
    }

    async createNewSyncedFolder(folder: Pick<MailFolder, "uid">, version: number, minInterval: number) {
        (await this.dbPromise).put("synced_mail_folders", { uid: folder.uid, version, minInterval }, folder.uid);
    }

    async updateSyncedVersion(folder: SyncedFolder, version: number) {
        (await this.dbPromise).put("synced_mail_folders", { ...folder, version }, folder.uid);
    }

    async deleteMailItems(mailItems: Array<Pick<MailItem, "internalId">>): Promise<void> {
        const tx = (await this.dbPromise).transaction("mail_items", "readwrite");
        await Promise.all(
            mailItems.map(mail => {
                tx.store.delete(mail.internalId);
            })
        );
        await tx.done;
    }

    async getSyncedFolders(): Promise<Array<SyncedFolder>> {
        return (await this.dbPromise).getAll("synced_mail_folders");
    }

    async getMailFolderByFullName(fullName: string): Promise<MailFolder | undefined> {
        return (await this.dbPromise).getFromIndex("mail_folders", "fullName", fullName);
    }

    async getSyncedFolder(folder: Pick<MailFolder, "uid">): Promise<SyncedFolder | undefined> {
        return (await this.dbPromise).get("synced_mail_folders", folder.uid);
    }

    async getAllMailItems() {
        return (await this.dbPromise).getAll("mail_items");
    }

    async getMailItems(ids: Array<Pick<MailItem, "internalId">>) {
        const tx = (await this.dbPromise).transaction("mail_items");
        const mailItems = await Promise.all(ids.map(({ internalId }) => tx.store.get(internalId)));
        await tx.done;
        return mailItems;
    }
}
