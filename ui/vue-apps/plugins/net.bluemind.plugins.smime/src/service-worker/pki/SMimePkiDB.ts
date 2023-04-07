import { DBSchema, IDBPDatabase, openDB } from "idb";
import { RevocationResult } from "@bluemind/smime.cacerts.api";
import { logger } from "../environnment/logger";
import { PKIEntry, PKIStatus } from "../../lib/constants";
import session from "../environnment/session";

type RevocationSchema = RevocationResult & {
    cacheValidity: Date;
};

interface SMimePkiSchema extends DBSchema {
    my_key_and_cert: {
        key: string;
        value: Blob;
    };
    revocations: {
        key: string;
        value: RevocationSchema;
    };
}

interface SMimePkiDB {
    clearMyCertAndKey(): Promise<void>;
    getPrivateKey(): Promise<Blob | undefined>;
    setPrivateKey(privateKey: Blob): Promise<void>;
    setCertificate(certificate: Blob): Promise<void>;
    getCertificate(): Promise<Blob | undefined>;
    getPKIStatus(): Promise<PKIStatus>;
    getRevocation(serialNumber: string, issuerHash: string): Promise<RevocationSchema | undefined>;
    setRevocation(revocation: RevocationSchema, issuerHash: string): Promise<void>;
    clearRevocations(): Promise<void>;
}

class SMimePkiDBImpl implements SMimePkiDB {
    NAME = "smime:pki";
    VERSION = 1;
    connection: Promise<IDBPDatabase<SMimePkiSchema>>;
    constructor(userId: string) {
        this.connection = this.open(userId);
    }
    private async open(userId: string): Promise<IDBPDatabase<SMimePkiSchema>> {
        return openDB<SMimePkiSchema>(`${userId}:${this.NAME}`, this.VERSION, {
            upgrade: (db, from) => {
                logger.log(`[@bluemind/plugin.smime][SMimePkiDB] Upgrading from ${from} to ${this.VERSION}`);
                db.createObjectStore("my_key_and_cert");
                db.createObjectStore("revocations");
            },
            blocking: async () => {
                (await this.connection).close();
                this.connection = this.open(userId);
            }
        });
    }

    async clearMyCertAndKey(): Promise<void> {
        return (await this.connection).clear("my_key_and_cert");
    }
    async getPrivateKey(): Promise<Blob | undefined> {
        return (await this.connection).get("my_key_and_cert", PKIEntry.PRIVATE_KEY);
    }
    async setPrivateKey(privateKey: Blob): Promise<void> {
        (await this.connection).put("my_key_and_cert", privateKey, PKIEntry.PRIVATE_KEY);
    }
    async setCertificate(certificate: Blob): Promise<void> {
        (await this.connection).put("my_key_and_cert", certificate, PKIEntry.CERTIFICATE);
    }
    async getCertificate(): Promise<Blob | undefined> {
        return (await this.connection).get("my_key_and_cert", PKIEntry.CERTIFICATE);
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
    async getRevocation(serialNumber: string, issuerHash: string): Promise<RevocationSchema | undefined> {
        return (await this.connection).get("revocations", issuerHash + "-" + serialNumber);
    }
    async setRevocation(revocation: RevocationSchema, issuerHash: string): Promise<void> {
        (await this.connection).put("revocations", revocation, issuerHash + "-" + revocation.revocation.serialNumber);
    }
    async clearRevocations(): Promise<void> {
        return (await this.connection).clear("revocations");
    }
}

let implementation: SMimePkiDBImpl | null = null;
async function instance(): Promise<SMimePkiDB> {
    if (!implementation) {
        implementation = new SMimePkiDBImpl(await session.userId);
    }
    return implementation;
}
const db: SMimePkiDB = {
    clearMyCertAndKey: () => instance().then(db => db.clearMyCertAndKey()),
    getPrivateKey: () => instance().then(db => db.getPrivateKey()),
    setPrivateKey: privateKey => instance().then(db => db.setPrivateKey(privateKey)),
    setCertificate: certificate => instance().then(db => db.setCertificate(certificate)),
    getCertificate: () => instance().then(db => db.getCertificate()),
    getPKIStatus: () => instance().then(db => db.getPKIStatus()),
    getRevocation: (serialNumber, issuerHash) => instance().then(db => db.getRevocation(serialNumber, issuerHash)),
    setRevocation: (revocation, issuerHash) => instance().then(db => db.setRevocation(revocation, issuerHash)),
    clearRevocations: () => instance().then(db => db.clearRevocations())
};

export default db;
