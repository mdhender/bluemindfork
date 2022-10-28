import { PKIStatus } from "../../lib/constants";
import { ExpiredCredentialsError, InvalidCertificateError, InvalidKeyError } from "../exceptions";
import { checkCertificateValidity, getMyCertificate, getMyPrivateKey } from "../pki";
import db from "../SMimeDB";
import { readFile } from "./helpers";
jest.mock("../SMimeDB");

const mockCertificateTxt = readTxt("credentials/certificate");
const mockKeyTxt = readTxt("credentials/privateKey");

const mockKey = {
    text: jest.fn(() => Promise.resolve(mockKeyTxt))
};
const mockCertificate = {
    text: jest.fn(() => Promise.resolve(mockCertificateTxt))
};

describe("pki", () => {
    beforeEach(() => {
        db.getPKIStatus = jest.fn(() => Promise.resolve(PKIStatus.OK));
    });
    describe("getMyCertificate", () => {
        test("get the user certificate from database if present", async () => {
            db.getPrivateKey = jest.fn(() => Promise.resolve(mockKey));
            db.getCertificate = jest.fn(() => Promise.resolve(mockCertificate));
            const certificate = await getMyCertificate();
            expect(certificate).toBeTruthy();
        });
        test("raise an error if no certificate present in database", async () => {
            db.getPrivateKey = jest.fn(() => Promise.resolve(mockKey));
            db.getCertificate = jest.fn(() => Promise.resolve(null));
            try {
                await getMyCertificate();
            } catch (error) {
                expect(error).toBeInstanceOf(InvalidCertificateError);
            }
        });
        test("raise an error if the certificate is not valid", async () => {
            db.getPrivateKey = jest.fn(() => Promise.resolve(mockKey));
            db.getCertificate = jest.fn(() => Promise.resolve("invalid"));
            try {
                await getMyCertificate();
            } catch (error) {
                expect(error).toBeInstanceOf(InvalidCertificateError);
            }
        });
        test("raise an error if the certificate is revoked", () => {});
        test("raise an error if the certificate is not trusted", () => {});
    });
    describe("getMyPrivateKey", () => {
        test("get the user private key from database if present", async () => {
            db.getPrivateKey = jest.fn(() => Promise.resolve(mockKey));
            db.getCertificate = jest.fn(() => Promise.resolve(mockCertificate));
            const key = await getMyPrivateKey();
            expect(key).toBeTruthy();
        });
        test("raise an error if no private key present in database", async () => {
            db.getPrivateKey = jest.fn(() => Promise.resolve(null));
            try {
                await getMyPrivateKey();
            } catch (error) {
                expect(error).toBeInstanceOf(InvalidKeyError);
            }
        });
        test("raise an error if the private key is not valid", async () => {
            db.getPrivateKey = jest.fn(() => Promise.resolve("invalid"));
            try {
                await getMyPrivateKey();
            } catch (error) {
                expect(error).toBeInstanceOf(InvalidKeyError);
            }
        });
        test("raise an error if the private key is expired", () => {
            // No expiration date for the private key
        });
        test("raise an error if the private key is revoked", () => {});
        test("raise an error if the private key is not trusted", () => {});
    });
    describe("checkCertificateValidity", () => {
        test("throw ExpiredCredentialsError if certificate is expired", done => {
            const certificate = { validity: { notBefore: new Date(100), notAfter: new Date(1000) } };
            try {
                checkCertificateValidity(certificate, new Date(3000));
                done.fail();
            } catch (error) {
                expect(error).toBeInstanceOf(ExpiredCredentialsError);
                done();
            }
        });
        test("dont throw anything if certificate is valid ", done => {
            const certificate = { validity: { notBefore: new Date(100), notAfter: new Date(1000) } };
            try {
                checkCertificateValidity(certificate, new Date(500));
                done();
            } catch (error) {
                done.fail();
            }
        });
    });
});

function readTxt(file) {
    return readFile(`${file}.txt`);
}
