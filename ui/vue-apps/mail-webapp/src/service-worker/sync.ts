import { MailIDB } from "./mailIDB";
import { MailAPI } from "./api";
import { UID } from "./api/entry";

let sessionId: string;
let db: MailIDB;
let mailapi: MailAPI;

export async function initSync() {
    const { sid, userId, domain } = await getSessionInfos();
    await initIDB(sid, userId, domain);
    sessionId = sid;
    db = new MailIDB();
    mailapi = new MailAPI(sessionId);
}

export async function periodicSync(foldersFullName: Array<string>) {
    await Promise.all(foldersFullName.map(markAsSynced));
    // start sync process
    const folders = await db.getSyncedFolders();
    return await Promise.all(
        folders.map(async ({ uid, minInterval }) => {
            const syncer = sync(uid);
            const intervalId = setInterval(syncer, minInterval);
            return { intervalId, minInterval, uid };
        })
    );
}

export function sync(uid: UID) {
    return async () => {
        console.log("SYNCING FOLDER ", uid);
        const lastSync = await db.getSyncedFolder({ uid });
        if (!lastSync) {
            throw new Error(`Folder not found: ${{ uid }}`);
        }
        const changeset = await mailapi.getChangeset({ uid }, lastSync.version);
        if (changeset.version === lastSync.version) {
            return;
        }
        const createdAndUpdatedIds = changeset.created
            .map(({ id }) => ({ internalId: id }))
            .concat(changeset.updated.map(({ id }) => ({ internalId: id })));
        const toBeUpdatedMailItems = await mailapi.getMailItems({ uid }, createdAndUpdatedIds);
        await db.storeMailItems(toBeUpdatedMailItems);
        const deletedIds = changeset.deleted.map(({ id }) => ({ internalId: id }));
        await db.deleteMailItems(deletedIds);
        const folder = await db.getSyncedFolder({ uid });
        if (folder) {
            await db.updateSyncedVersion(folder, changeset.version);
        } else {
            throw new Error(`Folder not found: ${{ uid }}`);
        }
    };
}

async function getSessionInfos(): Promise<{ sid: string; userId: string; domain: string }> {
    const response = await fetch("/session-infos");
    return response.json();
}

async function markAsSynced(syncedFolderFullName: string) {
    const db = new MailIDB();
    const folder = await db.getMailFolderByFullName(syncedFolderFullName);
    if (folder) {
        console.log(`mark ${syncedFolderFullName} as synced folder`);
        await db.markAsSynced(folder, 1 * 60 * 1000);
    }
}

async function initIDB(sid: string, userId: string, domain: string) {
    const db = new MailIDB();
    const mailboxRoot = `user.${userId}`;
    const partition = domain.replace(".", "_");
    const mailapi = new MailAPI(sid);
    const mailFolders = await mailapi.getMailFolders(partition, mailboxRoot);
    await db.storeMailFolders(mailFolders);
}
