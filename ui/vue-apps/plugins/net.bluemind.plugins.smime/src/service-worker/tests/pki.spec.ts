import fetchMock from "fetch-mock";
import { pki } from "node-forge";
import { VCardQuery } from "@bluemind/addressbook.api";
import { RevocationResult } from "@bluemind/smime.cacerts.api";
import { PKIStatus, SMIME_CERT_USAGE } from "../../lib/constants";
import {
    CertificateRecipientNotFoundError,
    KeyNotFoundError,
    InvalidCertificateError,
    InvalidKeyError,
    UntrustedCertificateError,
    UntrustedCertificateEmailNotFoundError
} from "../../lib/exceptions";
import { getCaCerts } from "../pki/cert";
import {
    checkCertificate,
    clear,
    getMyCertificate,
    getMyPrivateKey,
    getCertificate,
    setMyPrivateKey,
    setMyCertificate
} from "../pki";
import db from "../pki/SMimePkiDB";
import { readFile } from "./helpers";

fetchMock.mock("/session-infos", { userId: "baz", domain: "foo.bar" });

const privateKey = readFile("privateKeys/privateKey.key");

const aliceCA = readFile("certificates/aliceCA.crt"); // alice CA cert from RFC9216
const basicCA = readFile("certificates/basicCA.crt"); // alice CA cert from RFC9216

const aliceCert = readFile("certificates/alice.crt"); // alice cert from RFC9216
const basicCert = readFile("certificates/basicCert.crt");
const expiredCert = readFile("certificates/expiredSSL.crt");
const corruptedCert = readFile("certificates/corruptedSignature.crt");
const otherCertificate = readFile("certificates/otherCertificate.crt");
const invalidCertificate = readFile("certificates/invalid.crt");
const anyExtendedKeyUsageCert = readFile("certificates/anyExtendedKeyUsage.crt"); // self-signed

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

jest.mock("../pki/SMimePkiDB");
jest.mock("../pki/cert", () => {
    return {
        ...jest.requireActual("../pki/cert"),
        getCaCerts: jest.fn()
    };
});

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
                value: { security: { key: { parameters: [], value: invalidCertificate } } },
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

    describe("check if certificate can be trusted for S/MIME usage", () => {
        const aliceCertificate = pki.certificateFromPem(aliceCert);

        beforeEach(() => {
            (<jest.Mock>getCaCerts).mockResolvedValue([
                { value: { cert: aliceCA } },
                { value: { cert: basicCA } },
                { value: { cert: anyExtendedKeyUsageCert } }
            ]);
            fetchMock.mock(
                "end:/api/smime_revocation/foo.bar/is_revoked",
                [{ status: RevocationResult.RevocationStatus.NOT_REVOKED }],
                { overwriteRoutes: true }
            );
        });

        test("untrusted if date is not within certificate validity period", async done => {
            try {
                await checkCertificate(aliceCertificate, { date: new Date("2019-11-01") });
                done.fail("certificate was not valid before 20 Nov. 2019");
            } catch (e) {
                expect(e instanceof UntrustedCertificateError).toBe(true);
                expect((<UntrustedCertificateError>e).message.includes("forge.pki.CertificateExpired")).toBe(true);
                done();
            }
        });

        test("untrusted if cert has expired", async done => {
            try {
                await checkCertificate(pki.certificateFromPem(expiredCert));
                done.fail("expired certificate must not be trusted");
            } catch (e) {
                expect(e instanceof UntrustedCertificateError).toBe(true);
                expect((<UntrustedCertificateError>e).message.includes("forge.pki.CertificateExpired")).toBe(true);
                done();
            }
        });

        test("untrusted if cert has been corrupted (invalid signature)", async done => {
            try {
                await checkCertificate(pki.certificateFromPem(corruptedCert));
                done.fail("certificate with an invalid signature must not be trusted");
            } catch (e) {
                expect(e instanceof UntrustedCertificateError).toBe(true);
                expect((<UntrustedCertificateError>e).message.includes("forge.pki.BadCertificate")).toBe(true);
                done();
            }
        });

        test("untrusted if no CA cert set", async done => {
            (<jest.Mock>getCaCerts).mockResolvedValue([]);
            try {
                await checkCertificate(aliceCertificate);
                done.fail("no CA cert defined, cannot trust any end-user certificate");
            } catch (e) {
                expect(getCaCerts).toHaveBeenCalled();
                expect(e instanceof UntrustedCertificateError).toBe(true);
                done();
            }
        });

        test("untrusted if CA issuer is not trusted", async done => {
            try {
                await checkCertificate(pki.certificateFromPem(otherCertificate));
                done.fail("certificate issuer (its CA) is not trusted");
            } catch (e) {
                expect(e instanceof UntrustedCertificateError).toBe(true);
                expect((<UntrustedCertificateError>e).message.includes("forge.pki.UnknownCertificateAuthority")).toBe(
                    true
                );
                done();
            }
        });
        test("cant use a CA certificate", async done => {
            try {
                await checkCertificate(pki.certificateFromPem(aliceCA));
                done.fail("you should not use CA cert for S/MIME");
            } catch (e) {
                expect(e instanceof UntrustedCertificateError).toBe(true);
                done();
            }
        });
        test("cant use certificate because of its 'extendedKeyUsage' (if set, its value should be either emailProtection or anyExtendedKeyUsage)", async done => {
            try {
                await checkCertificate(aliceCertificate); // emailProtection set
                await checkCertificate(pki.certificateFromPem(anyExtendedKeyUsageCert));
                await checkCertificate(pki.certificateFromPem(basicCert));
                done();
            } catch (e) {
                done.fail("those certificates have no extendedKeyUsage issue to be used for S/MIME");
            }
        });
        test("untrusted if expected email is not found neither in emailAddress or in 'Subject Alternative Name' extension", async done => {
            try {
                await checkCertificate(aliceCertificate, { expectedAddress: "alice@smime.example" });
            } catch {
                done.fail("alice@smime.example is set in certificate");
            }
            const expectedAddress = "notfound@mail.com";
            try {
                await checkCertificate(aliceCertificate, { expectedAddress });
                done.fail("email found in certificate should match expected one");
            } catch (e) {
                expect(e instanceof UntrustedCertificateEmailNotFoundError).toBe(true);
                expect((<UntrustedCertificateEmailNotFoundError>e).message.includes(expectedAddress));
                done();
            }
        });

        test("untrusted if keyUsage does not match expected usage", async done => {
            try {
                await checkCertificate(aliceCertificate, { smimeUsage: SMIME_CERT_USAGE.SIGN });
                await checkCertificate(aliceCertificate, { smimeUsage: SMIME_CERT_USAGE.ENCRYPT });
                done.fail("this certificate cannot be used for encryption");
            } catch (e) {
                done();
            }
        });

        test("untrusted if cert is revoked", async done => {
            fetchMock.mock(
                "end:/api/smime_revocation/foo.bar/is_revoked",
                [{ status: RevocationResult.RevocationStatus.REVOKED, date: new Date().getTime() }],
                { overwriteRoutes: true }
            );
            try {
                await checkCertificate(aliceCertificate);
                done.fail("revoked certificate must not be trusted");
            } catch (e) {
                expect(e instanceof UntrustedCertificateError).toBe(true);
                done();
            }
        });

        test("cert is trusted if date checked is before revokation", async done => {
            const revokedDate = new Date("2023-02-01");
            fetchMock.mock(
                "end:/api/smime_revocation/foo.bar/is_revoked",
                [{ status: RevocationResult.RevocationStatus.REVOKED, date: revokedDate.getTime() }],
                { overwriteRoutes: true }
            );
            try {
                await checkCertificate(aliceCertificate, { date: new Date("2023-01-01") });
                done();
            } catch (e) {
                done.fail("certificate is revoked but checked date was before revokation happened");
            }
        });
    });
});
