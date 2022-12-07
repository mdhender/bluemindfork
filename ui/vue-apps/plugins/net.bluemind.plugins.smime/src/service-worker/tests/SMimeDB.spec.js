import db from "../pki/SMimeDB";

describe("SMimeDB", () => {
    describe("clearPKI", () => {
        test("reset all data from pki", () => {});
    });
    describe("getPrivateKey", () => {
        test("get key content as blob", () => {});
        test("return undefined if there is no key", () => {});
    });
    describe("setPrivateKey", () => {
        test("set privateKey content from blob", () => {});
        test("override private key if already present", () => {});
        test("do noting if new private key content is empty", () => {});
    });
    describe("setCertificate", () => {
        test("set certificate content from blob", () => {});
        test("override certificate if already present", () => {});
        test("do noting if new certificate content is empty", () => {});
    });
    describe("getCertificate", () => {
        test("get certificate content as blob", () => {});
        test("return undefined if there is no certificate", () => {});
    });
    describe("getPKIStatus", () => {
        test("return ok status if all data are set", () => {});
        test("return MISSING_CERTIFICATE if certificate is missing", () => {});
        test("return MISSING_PRIVATE_KEY if certificate is missing", () => {});
        test("return EMPTY if PKI is empty ", () => {});
        test("return CERTIVATE_REVOKED if certificate has been revoked", () => {});
        test("return PRIVATE_KEY_REVOKED if private key has been revoked", () => {});
        test("return CERTIVATE_EXPIRED if certificate has epired", () => {});
        test("return PRIVATE_KEY_EXPIRED if private key has epired", () => {});
        test("return INVALID_CERTIVATE if certificat is invalid", () => {});
        test("return INVALID_PRIVATE_KEY if private key is invalid", () => {});
        test("return UNTRUSTED_CERTIVATE if certificat is not trusted", () => {});
        test("return UNTRUSTED_PRIVATE_KEY if private key is not trusted", () => {});
        test("return ok status if all data are set", () => {});
    });
});
