import fetchMock from "fetch-mock";
import { VCardQuery } from "@bluemind/addressbook.api";
import { PKIStatus } from "../../lib/constants";
import {
    CertificateRecipientNotFoundError,
    InvalidCertificateError,
    InvalidKeyError,
    UntrustedCertificateError
} from "../../lib/exceptions";
import {
    checkCertificate,
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

const certificate = readFile("certificates/certificate.crt");
const privateKey = readFile("privateKeys/privateKey.key");
const otherCertificate = readFile("certificates/otherCertificate.crt");
const nonRepudiationCert = readFile("certificates/alice.pem");
// const anyExtendedKeyUsageCert = readFile("certificates/anyExtendedKeyUsage.crt");

class MockedKeyAsBlob extends Blob {
    text() {
        return Promise.resolve(privateKey);
    }
}
class MockedCertAsBlob extends Blob {
    text() {
        return Promise.resolve(certificate);
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
                value: { security: { key: { parameters: [], value: otherCertificate } } },
                uid: "2DF7A15F-12FD-4864-8279-12ADC6C08BAF"
            }
        ];
    } else if (uids.includes("invalid")) {
        return [
            {
                value: { security: { key: { parameters: [], value: otherCertificate } } },
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

jest.mock("@bluemind/smime.cacerts.api", () => ({
    SmimeCACertClient: () => ({
        all: () => [
            {
                value: {
                    // alice CA cert from RFC9216
                    cert: `-----BEGIN CERTIFICATE-----
                    MIIDezCCAmOgAwIBAgITcBn0xb/zdaeCQlqp6yZUAGZUCDANBgkqhkiG9w0BAQ0F
                    ADBVMQ0wCwYDVQQKEwRJRVRGMREwDwYDVQQLEwhMQU1QUyBXRzExMC8GA1UEAxMo
                    U2FtcGxlIExBTVBTIFJTQSBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTAgFw0xOTEx
                    MjAwNjU0MThaGA8yMDUyMDkyNzA2NTQxOFowVTENMAsGA1UEChMESUVURjERMA8G
                    A1UECxMITEFNUFMgV0cxMTAvBgNVBAMTKFNhbXBsZSBMQU1QUyBSU0EgQ2VydGlm
                    aWNhdGlvbiBBdXRob3JpdHkwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB
                    AQC2GGPTEFVNdi0LsiQ79A0Mz2G+LRJlbX2vNo8STibAnyQ9VzFrGJHjUhRX/Omr
                    OP3rDCB2SYfBPVwd0CdC6z9qfJkcVxDc1hK+VS9vKncL0IPUYlkJwWuMpXa1Ielz
                    +zCuV+gjV83Uvn6wTn39MCmymu7nFPzihcuOnbMYOCdMmUbi1Dm8TX9P6itFR3hi
                    IHpSKMbkoXlM1837WaFfx57kBIoIuNjKEyPIuK9wGUAeppc5QAHJg95PPEHNHlmM
                    yhBzClmgkyozRSeSrkxq9XeJKU94lWGaZ0zb4karCur/eiMoCk3YNV8L3styvcMG
                    1qUDCAaKx6FZEf7hE9RN6L3bAgMBAAGjQjBAMA8GA1UdEwEB/wQFMAMBAf8wDgYD
                    VR0PAQH/BAQDAgEGMB0GA1UdDgQWBBSRMI58BxcMp/EJKGU2GmccaHb0WTANBgkq
                    hkiG9w0BAQ0FAAOCAQEACDXWlJGjzKadNMPcFlZInZC+Hl7RLrcBDR25jMCXg9yL
                    IwGVEcNp2fH4+YHTRTGLH81aPADMdUGHgpfcfqwjesavt/mO0T0S0LjJ0RVm93fE
                    heSNUHUigVR9njTVw2EBz7e2p+v3tOsMnunvm6PIDgHxx0W6mjzMX7lG74bJfo+v
                    dx+jI/aXt+iih5pi7/2Yu9eTDVu+S52wsnF89BEJeV0r+EmGDxUv47D+5KuQpKM9
                    U/isXpwC6K/36T8RhhdOQXDq0Mt91TZ4dJTT0m3cmo80zzcxsKMDStZHOOzCBtBq
                    uIbwWw5Oa72o/Iwg9v+W0WkSBCWEadf/uK+cRicxrQ==
                    -----END CERTIFICATE-----`
                }
            }
        ]
    })
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
                expect(error).toBeInstanceOf(InvalidCertificateError);
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

    describe("check if certificate can be trusted for S/MIME usage", () => {
        const date = new Date("2023-02-18"),
            sender = "test@devenv.blue";
        test("untrusted if cert has been corrupted (signature check failed)", async () => {
            // await checkCertificate(pki.certificateFromPem(anyExtendedKeyUsageCert), sendingDate, senderEmail);
        });
        test.todo("untrusted if cert issuer (CA) is not trusted");
        test.todo("untrusted if cert has expired");
        test.only("untrusted if its a CA certificate", async done => {
            try {
                await checkCertificate(pki.certificateFromPem(nonRepudiationCert), date, sender);
                done.fail("CA cert cannot be used");
            } catch (error) {
                expect(error).toBeInstanceOf(UntrustedCertificateError);
                done();
            }
        });
        test.todo(
            "untrusted if Extended Key Usage is set but its value is neither emailProtection nor anyExtendedKeyUsage"
        );
        test.todo(
            "untrusted if expected email is not found neither in emailAddress or in 'Subject Alternative Name' extension"
        );
        test.todo("untrusted if cert is revoked");
        test.todo(
            "CHECKME!! untrusted if keyUsage is set but its value is not 'Non Repudiation' or 'Digital Signature' or 'Key Encipherment'"
        );

        // FIXME
        // test("throw UntrustedCertificateError if certificate is expired", done => {
        //     const certificate = { validity: { notBefore: new Date(100), notAfter: new Date(1000) } };
        //     try {
        //         checkCertificate(<pki.Certificate>certificate, new Date(3000));
        //         done.fail();
        //     } catch (error) {
        //         expect(error).toBeInstanceOf(UntrustedCertificateError);
        //         done();
        //     }
        // });
        // test("dont throw anything if certificate is valid ", done => {
        //     const certificate = { validity: { notBefore: new Date(100), notAfter: new Date(1000) } };
        //     try {
        //         checkCertificate(<pki.Certificate>certificate, new Date(500));
        //         done();
        //     } catch (error) {
        //         done.fail();
        //     }
        // });
    });
});
