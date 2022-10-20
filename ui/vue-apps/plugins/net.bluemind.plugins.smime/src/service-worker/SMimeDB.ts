import { DBSchema, IDBPDatabase, openDB } from "idb";
import { logger } from "./environnment/logger";
import { PKIEntry, PKIStatus } from "../lib/constants";
import session from "./environnment/session";

interface SMimeSchema extends DBSchema {
    pki: {
        key: string;
        value: Blob;
    };
}

interface SMimeDB {
    clearPKI(): Promise<void>;
    getPrivateKey(): Promise<Blob | undefined>;
    setPrivateKey(privateKey: Blob): Promise<void>;
    setCertificate(certificate: Blob): Promise<void>;
    getCertificate(): Promise<Blob | undefined>;
    getPKIStatus(): Promise<PKIStatus>;
}

class SMimeDBImpl implements SMimeDB {
    NAME = "smime";
    VERSION = 1;
    connection: Promise<IDBPDatabase<SMimeSchema>>;
    constructor(login: string) {
        this.connection = this.open(login);
    }
    private async open(login: string): Promise<IDBPDatabase<SMimeSchema>> {
        return openDB<SMimeSchema>(`${login}:${this.NAME}`, this.VERSION, {
            upgrade: (db, from) => {
                logger.log(`[@bluemind/plugin.smime][SMimeDB] Upgrading from ${from} to ${this.VERSION}`);
                db.createObjectStore("pki");
            }
        });
    }

    async clearPKI(): Promise<void> {
        return (await this.connection).clear("pki");
    }
    async getPrivateKey(): Promise<Blob | undefined> {
        return (await this.connection).get("pki", PKIEntry.PRIVATE_KEY);
    }
    async setPrivateKey(privateKey: Blob): Promise<void> {
        (await this.connection).add("pki", privateKey, PKIEntry.PRIVATE_KEY);
    }
    async setCertificate(certificate: Blob): Promise<void> {
        (await this.connection).add("pki", certificate, PKIEntry.CERTIFICATE);
    }
    async getCertificate(): Promise<Blob | undefined> {
        return (await this.connection).get("pki", PKIEntry.CERTIFICATE);
    }
    async getPKIStatus(): Promise<PKIStatus> {
        let status = PKIStatus.EMPTY;
        if (await this.getCertificate()) {
            status |= PKIStatus.CERTIFICATE_OK;
        }
        if (await this.getPrivateKey()) {
            status |= PKIStatus.PRIVATE_KEY_OK;
        }
        return status;
    }
}

let implementation: SMimeDBImpl | null = null;
async function instance(): Promise<SMimeDB> {
    if (!implementation) {
        implementation = new SMimeDBImpl(await session.login);
    }
    return implementation;
}
const db: SMimeDB = {
    clearPKI: () => instance().then(db => db.clearPKI()),
    getPrivateKey: () => instance().then(db => db.getPrivateKey()),
    setPrivateKey: privateKey => instance().then(db => db.setPrivateKey(privateKey)),
    setCertificate: certificate => instance().then(db => db.setCertificate(certificate)),
    getCertificate: () => instance().then(db => db.getCertificate()),
    getPKIStatus: () => instance().then(db => db.getPKIStatus())
};

export default db;
