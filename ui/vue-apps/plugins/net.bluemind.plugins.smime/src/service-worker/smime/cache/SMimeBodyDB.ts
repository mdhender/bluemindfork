import { DBSchema, IDBPDatabase, openDB } from "idb";
import { MessageBody } from "@bluemind/backend.mail.api";
import session from "@bluemind/session";

interface SMimeSchema extends DBSchema {
    guid: {
        key: [string, number];
        value: {
            guid: string;
            creation_time: number;
        };
        indexes: { by_creation_time: number };
    };
    body: {
        key: string;
        value: MessageBody;
    };
}
interface SMimeBodyDB {
    setGuid(folderUid: string, imapUid: number, guid: string): Promise<void>;
    getGuid(folderUid: string, imapUid: number): Promise<string | undefined>;
    clearGuid(): Promise<void>;
    setBody(guid: string, itemBody: MessageBody): Promise<void>;
    getBody(guid: string): Promise<MessageBody | undefined>;
    deleteBody(guid: string): Promise<void>;
    clearBody(): Promise<void>;
    invalidate(timestamp?: number): Promise<void>;
}

class SMimeBodyDBImpl implements SMimeBodyDB {
    NAME = "smime:body";
    VERSION = 1;
    connection: Promise<IDBPDatabase<SMimeSchema>>;
    constructor(userId: string) {
        this.connection = this.open(userId);
    }
    private async open(userId: string): Promise<IDBPDatabase<SMimeSchema>> {
        return openDB<SMimeSchema>(`${userId}:${this.NAME}`, this.VERSION, {
            upgrade: db => {
                db.createObjectStore("guid").createIndex("by_creation_time", "creation_time");
                db.createObjectStore("body");
            },
            blocking: async () => {
                await this.close();
                this.connection = this.open(userId);
            }
        });
    }
    async close(): Promise<void> {
        (await this.connection).close();
    }
    async setGuid(folderUid: string, imapUid: number, guid: string): Promise<void> {
        (await this.connection).put("guid", { guid, creation_time: Date.now() }, [folderUid, imapUid]);
    }
    async getGuid(folderUid: string, imapUid: number): Promise<string | undefined> {
        const value = await (await this.connection).get("guid", [folderUid, imapUid]);
        return value?.guid;
    }
    async clearGuid() {
        return (await this.connection).clear("guid");
    }
    async setBody(guid: string, itemBody: MessageBody): Promise<void> {
        (await this.connection).put("body", itemBody, guid);
    }
    async getBody(guid: string): Promise<MessageBody | undefined> {
        return (await this.connection).get("body", guid);
    }
    async deleteBody(guid: string): Promise<void> {
        return (await this.connection).delete("body", guid);
    }
    async clearBody() {
        return (await this.connection).clear("body");
    }
    async invalidate(timestamp: number): Promise<void> {
        const connection = await this.connection;
        const upperBound = IDBKeyRange.upperBound(timestamp);
        const guidKeys = await connection.getAllKeysFromIndex("guid", "by_creation_time", upperBound);
        const guidsValues = await connection.getAllFromIndex("guid", "by_creation_time", upperBound);

        const tx = connection.transaction(["body", "guid"], "readwrite");
        guidsValues.forEach((guidValue, idx) => {
            tx.objectStore("guid").delete(guidKeys[idx]);
            tx.objectStore("body").delete(guidValue.guid);
        });
        tx.done;
    }
}
let implementation: SMimeBodyDBImpl | null = null;
async function instance(): Promise<SMimeBodyDB> {
    if (!implementation) {
        implementation = new SMimeBodyDBImpl(await session.userId);
    }
    return implementation;
}

session.addEventListener("change", event => {
    const { old, value } = event.detail;
    if (value.userId != old?.userId && implementation) {
        implementation?.close();
        implementation = null;
    }
});

const db: SMimeBodyDB = {
    setGuid: (folderUid, imapUid, guid) => instance().then(db => db.setGuid(folderUid, imapUid, guid)),
    getGuid: (folderUid, imapUid) => instance().then(db => db.getGuid(folderUid, imapUid)),
    clearGuid: () => instance().then(db => db.clearGuid()),
    setBody: (guid, body) => instance().then(db => db.setBody(guid, body)),
    getBody: guid => instance().then(db => db.getBody(guid)),
    deleteBody: guid => instance().then(db => db.deleteBody(guid)),
    clearBody: () => instance().then(db => db.clearBody()),
    invalidate: timestamp => instance().then(db => db.invalidate(timestamp))
};

export default db;
