import FDBFactory from "fake-indexeddb/lib/FDBFactory";
import SMimePkiDB from "../pki/SMimePkiDB";
import { PKIStatus } from "../../lib/constants";
import "../environnment/session";
jest.mock("../environnment/session", () => ({ userId: "my-user-id" }));

describe("SMimePkiDB", () => {
    beforeEach(async () => {
        global.indexedDB = new FDBFactory();
        await SMimePkiDB.clearPKI();
    });

    describe("clearPKI", () => {
        test("reset all data from pki", async () => {
            await SMimePkiDB.setPrivateKey("heya");
            const privKey = await SMimePkiDB.getPrivateKey();
            expect(privKey).toBe("heya");

            await SMimePkiDB.clearPKI();
            const status = await SMimePkiDB.getPKIStatus();
            expect(status).toBe(PKIStatus.EMPTY);
        });
    });
    describe("getPrivateKey", () => {
        test("return undefined if there is no key", async () => {
            const privKey = await SMimePkiDB.getPrivateKey();
            expect(privKey).toBe(undefined);
        });
        test("get and set key content", async () => {
            await SMimePkiDB.setPrivateKey("heya");
            const privKey = await SMimePkiDB.getPrivateKey();
            expect(privKey).toBe("heya");
        });
    });
    describe("setPrivateKey", () => {
        test("overwrite private key if already set", async () => {
            await SMimePkiDB.setPrivateKey("plop");
            await SMimePkiDB.setPrivateKey("anotherPlop");
            const privKey = await SMimePkiDB.getPrivateKey();
            expect(privKey).toBe("anotherPlop");
        });
        test("do nothing if new private key content is empty", async () => {
            const privKey = await SMimePkiDB.getPrivateKey();
            expect(privKey).toBe(undefined);
            await SMimePkiDB.setPrivateKey("");
            expect(privKey).toBe(undefined);
        });
    });
    describe("setCertificate", () => {
        test("set certificate content", async () => {
            await SMimePkiDB.setCertificate("ok");
            const cert = await SMimePkiDB.getCertificate();
            expect(cert).toBe("ok");
        });
        test("overwrite certificate if already set", async () => {
            await SMimePkiDB.setCertificate("plop");
            await SMimePkiDB.setCertificate("anotherPlop");
            const privKey = await SMimePkiDB.getCertificate();
            expect(privKey).toBe("anotherPlop");
        });
        test("do nothing if new certificate content is empty", async () => {
            await SMimePkiDB.setCertificate();
            let cert = await SMimePkiDB.setCertificate("");
            expect(cert).toBe(undefined);
            cert = await SMimePkiDB.setCertificate(null);
            expect(cert).toBe(undefined);
            cert = await SMimePkiDB.setCertificate();
            expect(cert).toBe(undefined);
        });
    });
    describe("getCertificate", () => {
        test("get certificate content", async () => {
            await SMimePkiDB.setCertificate("plop");
            const cert = await SMimePkiDB.getCertificate();
            expect(cert).toBe("plop");
        });
        test("return undefined if there is no certificate", async () => {
            const cert = await SMimePkiDB.getCertificate();
            expect(cert).toBe(undefined);
        });
    });
    describe("getPKIStatus", () => {
        test("return OK if all data are set", async () => {
            await SMimePkiDB.setCertificate("cert");
            await SMimePkiDB.setPrivateKey("privKey");
            const status = await SMimePkiDB.getPKIStatus();
            expect(status).toBe(PKIStatus.OK);
            expect(Boolean(status && PKIStatus.CERTIFICATE_OK)).toBe(true);
            expect(Boolean(status && PKIStatus.PRIVATE_KEY_OK)).toBe(true);
        });
        test("return EMPTY if PKI is empty ", async () => {
            const status = await SMimePkiDB.getPKIStatus();
            expect(status).toBe(PKIStatus.EMPTY);
        });
        test("return CERTIFICATE_OK if only cert is set", async () => {
            await SMimePkiDB.setCertificate("cert");
            const status = await SMimePkiDB.getPKIStatus();
            expect(status).toBe(PKIStatus.CERTIFICATE_OK);
            expect(status & PKIStatus.OK).not.toBe(PKIStatus.OK);
        });
        test("return PRIVATE_KEY_OK if only private key is set", async () => {
            await SMimePkiDB.setPrivateKey("privKey");
            const status = await SMimePkiDB.getPKIStatus();
            expect(status).toBe(PKIStatus.PRIVATE_KEY_OK);
            expect(status & PKIStatus.OK).not.toBe(PKIStatus.OK);
        });
    });
});
