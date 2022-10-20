import pki from "../pki";

describe("pki", () => {
    describe("getMyCertificate", () => {
        test("get the user certificate from database if present", () => {});
        test("raise an error if no certificate present in database", () => {});
        test("raise an error if the certificate is not valid", () => {});
        test("raise an error if the certificate is expired", () => {});
        test("raise an error if the certificate is revoked", () => {});
        test("raise an error if the certificate is not trusted", () => {});
    });
    describe("getMyPrivateKey", () => {
        test("get the user private key from database if present", () => {});
        test("raise an error if no private key present in database", () => {});
        test("raise an error if the private key is not valid", () => {});
        test("raise an error if the private key is expired", () => {});
        test("raise an error if the private key is revoked", () => {});
        test("raise an error if the private key is not trusted", () => {});
    });
});
