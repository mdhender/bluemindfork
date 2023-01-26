import FDBFactory from "fake-indexeddb/lib/FDBFactory";
import SMimeDB from "../pki/SMimeDB";
import { PKIStatus } from "../../lib/constants";
import "../environnment/session";
jest.mock("../environnment/session", () => ({ userId: "my-user-id" }));

describe("SMimeDB", () => {
    beforeEach(async () => {
        global.indexedDB = new FDBFactory();
        await SMimeDB.clearPKI();
    });

    describe("clearPKI", () => {
        test("reset all data from pki", async () => {
            await SMimeDB.setPrivateKey("heya");
            const privKey = await SMimeDB.getPrivateKey();
            expect(privKey).toBe("heya");

            await SMimeDB.clearPKI();
            const status = await SMimeDB.getPKIStatus();
            expect(status).toBe(PKIStatus.EMPTY);
        });
    });
    describe("getPrivateKey", () => {
        test("return undefined if there is no key", async () => {
            const privKey = await SMimeDB.getPrivateKey();
            expect(privKey).toBe(undefined);
        });
        test("get and set key content", async () => {
            await SMimeDB.setPrivateKey("heya");
            const privKey = await SMimeDB.getPrivateKey();
            expect(privKey).toBe("heya");
        });
    });
    describe("setPrivateKey", () => {
        test("overwrite private key if already set", async () => {
            await SMimeDB.setPrivateKey("plop");
            await SMimeDB.setPrivateKey("anotherPlop");
            const privKey = await SMimeDB.getPrivateKey();
            expect(privKey).toBe("anotherPlop");
        });
        test("do nothing if new private key content is empty", async () => {
            const privKey = await SMimeDB.getPrivateKey();
            expect(privKey).toBe(undefined);
            await SMimeDB.setPrivateKey("");
            expect(privKey).toBe(undefined);
        });
    });
    describe("setCertificate", () => {
        test("set certificate content", async () => {
            await SMimeDB.setCertificate("ok");
            const cert = await SMimeDB.getCertificate();
            expect(cert).toBe("ok");
        });
        test("overwrite certificate if already set", async () => {
            await SMimeDB.setCertificate("plop");
            await SMimeDB.setCertificate("anotherPlop");
            const privKey = await SMimeDB.getCertificate();
            expect(privKey).toBe("anotherPlop");
        });
        test("do nothing if new certificate content is empty", async () => {
            await SMimeDB.setCertificate();
            let cert = await SMimeDB.setCertificate("");
            expect(cert).toBe(undefined);
            cert = await SMimeDB.setCertificate(null);
            expect(cert).toBe(undefined);
            cert = await SMimeDB.setCertificate();
            expect(cert).toBe(undefined);
        });
    });
    describe("getCertificate", () => {
        test("get certificate content", async () => {
            await SMimeDB.setCertificate("plop");
            const cert = await SMimeDB.getCertificate();
            expect(cert).toBe("plop");
        });
        test("return undefined if there is no certificate", async () => {
            const cert = await SMimeDB.getCertificate();
            expect(cert).toBe(undefined);
        });
    });
    describe("getPKIStatus", () => {
        test("return OK if all data are set", async () => {
            await SMimeDB.setCertificate("cert");
            await SMimeDB.setPrivateKey("privKey");
            const status = await SMimeDB.getPKIStatus();
            expect(status).toBe(PKIStatus.OK);
            expect(Boolean(status && PKIStatus.CERTIFICATE_OK)).toBe(true);
            expect(Boolean(status && PKIStatus.PRIVATE_KEY_OK)).toBe(true);
        });
        test("return EMPTY if PKI is empty ", async () => {
            const status = await SMimeDB.getPKIStatus();
            expect(status).toBe(PKIStatus.EMPTY);
        });
        test("return CERTIFICATE_OK if only cert is set", async () => {
            await SMimeDB.setCertificate("cert");
            const status = await SMimeDB.getPKIStatus();
            expect(status).toBe(PKIStatus.CERTIFICATE_OK);
            expect(status & PKIStatus.OK).not.toBe(PKIStatus.OK);
        });
        test("return PRIVATE_KEY_OK if only private key is set", async () => {
            await SMimeDB.setPrivateKey("privKey");
            const status = await SMimeDB.getPKIStatus();
            expect(status).toBe(PKIStatus.PRIVATE_KEY_OK);
            expect(status & PKIStatus.OK).not.toBe(PKIStatus.OK);
        });
    });
});
