import fetchMock from "fetch-mock";
import { VCardQuery } from "@bluemind/addressbook.api";
import { PKIStatus } from "../../lib/constants";
import {
    CertificateRecipientNotFoundError,
    ExpiredCertificateError,
    MyInvalidCertificateError,
    InvalidCertificateRecipientError,
    InvalidKeyError
} from "../exceptions";
import {
    checkCertificateValidity,
    clearMyCryptoFiles,
    getMyCertificate,
    getMyPrivateKey,
    getCertificate,
    setMyPrivateKey,
    setMyCertificate
} from "../pki";
import db from "../pki/SMimePkiDB";
import { readFile } from "./helpers";
import { pki } from "node-forge";
fetchMock.mock("/session-infos", { userId: "baz", domain: "foo.bar" });

const mockCertificateTxt = readTxt("documents/certificate");
const mockKeyTxt = readTxt("documents/privateKey");
const mockOtherCertificateTxt = readTxt("documents/otherCertificate");
const mockInvalidCertificateTxt = readTxt("documents/invalidCertificate");

class MockedKeyAsBlob extends Blob {
    text() {
        return Promise.resolve(mockKeyTxt);
    }
}
class MockedCertAsBlob extends Blob {
    text() {
        return Promise.resolve(mockCertificateTxt);
    }
}
class MockInvalidCertAsBlob extends Blob {
    text() {
        return Promise.resolve("invalid");
    }
}

jest.mock("../pki/SMimePkiDB");

const mockMultipleGet = jest.fn(uids => {
    if (uids.includes("2DF7A15F-12FD-4864-8279-12ADC6C08BAF")) {
        return [
            {
                value: { security: { key: { parameters: [], value: mockOtherCertificateTxt } } },
                uid: "2DF7A15F-12FD-4864-8279-12ADC6C08BAF"
            }
        ];
    } else if (uids.includes("invalid")) {
        return [
            {
                value: { security: { key: { parameters: [], value: mockInvalidCertificateTxt } } },
                uid: "invalid"
            }
        ];
    } else {
        return [];
    }
});

jest.mock("@bluemind/addressbook.api", () => ({
    AddressBooksClient: () => ({
        search: (searchQuery: VCardQuery) => {
            if (searchQuery.query!.includes("test@mail.com")) {
                return {
                    total: 2,
                    values: [
                        {
                            containerUid: "addressbook_2",
                            value: { mail: "deux@devenv.blue" },
                            uid: "AAA"
                        },
                        {
                            containerUid: "addressbook_f8de2c4a.internal",
                            value: { mail: "deux@devenv.blue" },
                            uid: "2DF7A15F-12FD-4864-8279-12ADC6C08BAF"
                        }
                    ]
                };
            } else if (searchQuery.query!.includes("invalid@mail.com")) {
                return {
                    total: 1,
                    values: [
                        {
                            containerUid: "addressbook_invalid.internal",
                            value: { mail: "invalid@devenv.blue" },
                            uid: "invalid"
                        }
                    ]
                };
            } else {
                return {
                    total: 0
                };
            }
        }
    }),
    AddressBookClient: () => ({
        multipleGet: mockMultipleGet
    }),
    VCardQuery: { OrderBy: { Pertinance: "Pertinance" } }
}));

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
                expect(error).toBeInstanceOf(MyInvalidCertificateError);
            }
        });
        test("raise an error if the certificate is not valid", async () => {
            db.getPrivateKey = () => Promise.resolve(new MockedKeyAsBlob());
            db.getCertificate = () => Promise.resolve(new MockInvalidCertAsBlob());
            try {
                await getMyCertificate();
            } catch (error) {
                expect(error).toBeInstanceOf(MyInvalidCertificateError);
            }
        });
        test("raise an error if the pkiStatus is not OK", async () => {
            db.getPKIStatus = () => Promise.resolve(PKIStatus.EMPTY);
            try {
                await getMyPrivateKey();
            } catch (error) {
                expect(error).toBeInstanceOf(InvalidKeyError);
            }
        });
        // test("raise an error if the certificate is revoked", () => {});
        // test("raise an error if the certificate is not trusted", () => {});
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
    describe("checkCertificateValidity", () => {
        test("throw ExpiredCertificateError if certificate is expired", done => {
            const certificate = { validity: { notBefore: new Date(100), notAfter: new Date(1000) } };
            try {
                checkCertificateValidity(<pki.Certificate>certificate, new Date(3000));
                done.fail();
            } catch (error) {
                expect(error).toBeInstanceOf(ExpiredCertificateError);
                done();
            }
        });
        test("dont throw anything if certificate is valid ", done => {
            const certificate = { validity: { notBefore: new Date(100), notAfter: new Date(1000) } };
            try {
                checkCertificateValidity(<pki.Certificate>certificate, new Date(500));
                done();
            } catch (error) {
                done.fail();
            }
        });
    });
    describe("getCertificate", () => {
        beforeEach(() => {
            mockMultipleGet.mockClear();
        });
        test("get the user certificate when given an email, if present", async () => {
            const certificate = await getCertificate("test@mail.com");
            expect(certificate).toBeTruthy();
        });
        test("calls multipleGet on several addressbooks if the user appears in multiple address books", async () => {
            await getCertificate("test@mail.com");
            expect(mockMultipleGet).toHaveBeenCalledTimes(2);
        });
        test("raise an error if no certificate is found", async done => {
            try {
                await getCertificate("unknown");
                done.fail();
            } catch (error) {
                expect(error).toBeInstanceOf(CertificateRecipientNotFoundError);
                done();
            }
        });
        test("raise an error if the recipient certificate in invalid", async done => {
            try {
                await getCertificate("invalid@mail.com");
                done.fail();
            } catch (error) {
                expect(error).toBeInstanceOf(InvalidCertificateRecipientError);
                done();
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
            await clearMyCryptoFiles();
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

function readTxt(file: string) {
    return readFile(`${file}.txt`);
}
