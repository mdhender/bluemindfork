import fetchMock from "fetch-mock";
import { PKIStatus } from "../../../lib/constants";
import { KeyNotFoundError, InvalidCertificateError, InvalidKeyError } from "../../../lib/exceptions";
import { clear, getMyCertificate, getMyPrivateKey, setMyPrivateKey, setMyCertificate } from "../../pki";
import db from "../../pki/SMimePkiDB";
import { readFile } from "../helpers";

fetchMock.mock("/session-infos", { userId: "baz", domain: "foo.bar" });

const privateKey = readFile("privateKeys/privateKey.key");
const aliceCert = readFile("certificates/alice.crt"); // alice cert from RFC9216

class MockedKeyAsBlob extends Blob {
    text() {
        return Promise.resolve(privateKey);
    }
}
class MockedCertAsBlob extends Blob {
    text() {
        return Promise.resolve(aliceCert);
    }
}
class MockInvalidCertAsBlob extends Blob {
    text() {
        return Promise.resolve("invalid");
    }
}

jest.mock("../../pki/SMimePkiDB");

describe("pki", () => {
    beforeEach(() => {
        db.getPKIStatus = () => Promise.resolve(PKIStatus.OK);
    });
    describe("getMyCertificate", () => {
        test("get the user certificate from database if present", async () => {
            db.getPrivateKey = () => Promise.resolve(new MockedKeyAsBlob());
            db.getCertificate = () => Promise.resolve(new MockedCertAsBlob());
            const certificate = await getMyCertificate();
            expect(certificate).toBeTruthy();
        });
        test("raise an error if no certificate present in database", async () => {
            db.getPrivateKey = () => Promise.resolve(new MockedKeyAsBlob());
            db.getCertificate = () => Promise.resolve(new Blob());
            try {
                await getMyCertificate();
            } catch (error) {
                expect(error).toBeInstanceOf(InvalidCertificateError);
            }
        });
        test("raise an error if the certificate is not valid", async () => {
            db.getPrivateKey = () => Promise.resolve(new MockedKeyAsBlob());
            db.getCertificate = () => Promise.resolve(new MockInvalidCertAsBlob());
            try {
                await getMyCertificate();
            } catch (error) {
                expect(error).toBeInstanceOf(InvalidCertificateError);
            }
        });
        test("raise an error if the pkiStatus is not OK", async () => {
            db.getPKIStatus = () => Promise.resolve(PKIStatus.EMPTY);
            try {
                await getMyPrivateKey();
            } catch (error) {
                expect(error).toBeInstanceOf(KeyNotFoundError);
            }
        });
    });
    describe("getMyPrivateKey", () => {
        test("get the user private key from database if present", async () => {
            db.getPrivateKey = () => Promise.resolve(new MockedKeyAsBlob());
            db.getCertificate = () => Promise.resolve(new MockedCertAsBlob());
            const key = await getMyPrivateKey();
            expect(key).toBeTruthy();
        });
        test("raise an error if no private key present in database", async () => {
            db.getPrivateKey = () => Promise.resolve(new Blob());
            try {
                await getMyPrivateKey();
            } catch (error) {
                expect(error).toBeInstanceOf(InvalidKeyError);
            }
        });
        test("raise an error if the private key is not valid", async () => {
            db.getPrivateKey = () => Promise.resolve(new MockInvalidCertAsBlob());
            try {
                await getMyPrivateKey();
            } catch (error) {
                expect(error).toBeInstanceOf(InvalidKeyError);
            }
        });
    });
    describe("manage service-worker cache for my cert and private key", () => {
        test("get my private key uses cache first", async () => {
            db.getPrivateKey = jest.fn(() => Promise.resolve(new MockedKeyAsBlob()));
            await setMyPrivateKey(new MockedKeyAsBlob());
            await getMyPrivateKey();
            expect(db.getPrivateKey).not.toHaveBeenCalled();
        });
        test("get my cert uses cache first", async () => {
            db.getCertificate = jest.fn(() => Promise.resolve(new MockedCertAsBlob()));
            await setMyCertificate(new MockedCertAsBlob());
            await getMyCertificate();
            expect(db.getCertificate).not.toHaveBeenCalled();
        });
        test("clear my crypto files call clearPKI and reset cache", async () => {
            db.getCertificate = jest.fn(() => Promise.resolve(new MockedCertAsBlob()));
            db.getPrivateKey = jest.fn(() => Promise.resolve(new MockedKeyAsBlob()));
            await clear();
            try {
                await getMyCertificate();
            } catch {
                expect(db.getCertificate).toHaveBeenCalledTimes(1);
            }
            try {
                await getMyPrivateKey();
            } catch {
                expect(db.getPrivateKey).toHaveBeenCalledTimes(1);
            }
        });
    });
});
