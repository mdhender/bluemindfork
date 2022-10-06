import { openDB } from "idb";

const SMIME_DB_VERSION = 1;

async function open() {
    return await openDB("smime", SMIME_DB_VERSION, {
        upgrade(db) {
            db.createObjectStore("crypto_files");
        }
    });
}

export default {
    async deleteCryptoFiles() {
        const db = await open();
        try {
            await db.delete("crypto_files", "privateKey");
            await db.delete("crypto_files", "publicCert");
        } finally {
            db.close();
        }
    },

    async hasCryptoFiles() {
        const db = await open();
        try {
            const privateKey = Boolean(await db.get("crypto_files", "privateKey"));
            const publicCert = Boolean(await db.get("crypto_files", "publicCert"));
            return { privateKey, publicCert };
        } finally {
            db.close();
        }
    },

    async setPrivateKey(file) {
        const db = await open();
        try {
            await db.add("crypto_files", file, "privateKey");
        } finally {
            db.close();
        }
    },

    async setPublicCert(file) {
        const db = await open();
        try {
            await db.add("crypto_files", file, "publicCert");
        } finally {
            db.close();
        }
    }
};
