import fetchMock from "fetch-mock";
import { PKIStatus } from "../../lib/constants";
import {
    CertificateRecipientNotFoundError,
    ExpiredCertificateError,
    InvalidCertificateError,
    InvalidKeyError
} from "../exceptions";
import { checkCertificateValidity, getMyCertificate, getMyPrivateKey, getCertificate } from "../pki";
import db from "../pki/SMimeDB";
import { readFile } from "./helpers";
fetchMock.mock("/session-infos", { userId: "baz", domain: "foo.bar" });

const mockCertificateTxt = readTxt("documents/certificate");
const mockKeyTxt = readTxt("documents/privateKey");
const mockOtherCertificateTxt = readTxt("documents/otherCertificate");

const mockKey = {
    text: jest.fn(() => Promise.resolve(mockKeyTxt))
};
const mockCertificate = {
    text: jest.fn(() => Promise.resolve(mockCertificateTxt))
};

jest.mock("../pki/SMimeDB");

const mockMultipleGet = jest.fn(uids => {
    if (uids.includes("2DF7A15F-12FD-4864-8279-12ADC6C08BAF")) {
        return [
            {
                value: {
                    security: {
                        key: {
                            parameters: [],
                            value: mockOtherCertificateTxt
                        }
                    }
                },
                uid: "2DF7A15F-12FD-4864-8279-12ADC6C08BAF"
            }
        ];
    } else {
        return [];
    }
});

jest.mock("@bluemind/addressbook.api", () => ({
    AddressBooksClient: () => ({
        search: searchQuery => {
            if (searchQuery.query.includes("test@mail.com")) {
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
        test("raise an error if the pkiStatus is not OK", async () => {
            db.getPKIStatus = jest.fn(() => Promise.resolve(PKIStatus.EMPTY));
            try {
                await getMyPrivateKey();
            } catch (error) {
                expect(error).toBeInstanceOf(InvalidKeyError);
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
        test("throw ExpiredCertificateError if certificate is expired", done => {
            const certificate = { validity: { notBefore: new Date(100), notAfter: new Date(1000) } };
            try {
                checkCertificateValidity(certificate, new Date(3000));
                done.fail();
            } catch (error) {
                expect(error).toBeInstanceOf(ExpiredCertificateError);
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
    });
});

function readTxt(file) {
    return readFile(`${file}.txt`);
}
