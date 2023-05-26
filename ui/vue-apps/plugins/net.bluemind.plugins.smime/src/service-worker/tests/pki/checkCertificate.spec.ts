import fetchMock from "fetch-mock";
import { pki } from "node-forge";
import { RevocationResult } from "@bluemind/smime.cacerts.api";
import { SMIME_CERT_USAGE } from "../../../lib/constants";
import checkCertificate, { checkRevoked, getRevocationCacheValidity, resetCaCerts } from "../../pki/checkCertificate";
import { UntrustedCertificateError, UntrustedCertificateEmailNotFoundError } from "../../../lib/exceptions";
import { readFile } from "../helpers";

const basicCA = readFile("certificates/basicCA.crt");
const aliceCA = readFile("certificates/aliceCA.crt"); // alice CA cert from RFC9216

const aliceCert = readFile("certificates/alice.crt"); // alice cert from RFC9216
const basicCert = readFile("certificates/basicCert.crt");
const expiredCert = readFile("certificates/expiredSSL.crt");
const corruptedCert = readFile("certificates/corruptedSignature.crt");
const otherCertificate = readFile("certificates/otherCertificate.crt");
const anyExtendedKeyUsageCert = readFile("certificates/anyExtendedKeyUsage.crt"); // self-signed

describe("check if certificate can be trusted for S/MIME usage", () => {
    beforeEach(() => {
        fetchMock.mock("/session-infos", { userId: "baz", domain: "foo.bar" });
        fetchMock.mock(
            "end:/api/smime_cacerts/smime_cacerts:domain_foo.bar/_all",
            [{ value: { cert: aliceCA } }, { value: { cert: basicCA } }, { value: { cert: anyExtendedKeyUsageCert } }],
            { overwriteRoutes: true }
        );
        fetchMock.mock("end:/api/smime_revocation/foo.bar/revoked_clients", [
            {
                status: RevocationResult.RevocationStatus.NOT_REVOKED,
                revocation: { serialNumber: "myCertSerialNumber" }
            }
        ]);
    });

    afterEach(() => {
        resetCaCerts();
        fetchMock.reset();
    });

    test("untrusted if date is not within certificate validity period", async done => {
        try {
            await checkCertificate(aliceCert, { date: new Date("2019-11-01") });
            done.fail("certificate was not valid before 20 Nov. 2019");
        } catch (e) {
            expect(e instanceof UntrustedCertificateError).toBe(true);
            expect((<UntrustedCertificateError>e).message.includes("forge.pki.CertificateExpired")).toBe(true);
            done();
        }
    });

    test("untrusted if no CA cert set", async done => {
        fetchMock.once("end:/api/smime_cacerts/smime_cacerts:domain_foo.bar/_all", [], {
            overwriteRoutes: true
        });
        try {
            await checkCertificate(aliceCert);
            done.fail("no CA cert defined, cannot trust any end-user certificate");
        } catch (e) {
            expect(e instanceof UntrustedCertificateError).toBe(true);
            done();
        }
    });

    test("untrusted if cert has expired", async done => {
        try {
            await checkCertificate(expiredCert);
            done.fail("expired certificate must not be trusted");
        } catch (e) {
            expect(e instanceof UntrustedCertificateError).toBe(true);
            expect((<UntrustedCertificateError>e).message.includes("forge.pki.CertificateExpired")).toBe(true);
            done();
        }
    });

    test("untrusted if cert has been corrupted (invalid signature)", async done => {
        try {
            await checkCertificate(corruptedCert);
            done.fail("certificate with an invalid signature must not be trusted");
        } catch (e) {
            expect(e instanceof UntrustedCertificateError).toBe(true);
            expect((<UntrustedCertificateError>e).message.includes("forge.pki.BadCertificate")).toBe(true);
            done();
        }
    });

    test("untrusted if CA issuer is not trusted", async done => {
        try {
            await checkCertificate(otherCertificate);
            done.fail("certificate issuer (its CA) is not trusted");
        } catch (e) {
            expect(e instanceof UntrustedCertificateError).toBe(true);
            expect((<UntrustedCertificateError>e).message.includes("forge.pki.UnknownCertificateAuthority")).toBe(true);
            done();
        }
    });
    test("cant use a CA certificate", async done => {
        try {
            await checkCertificate(aliceCA);
            done.fail("you should not use CA cert for S/MIME");
        } catch (e) {
            expect(e instanceof UntrustedCertificateError).toBe(true);
            done();
        }
    });
    test("cant use certificate because of its 'extendedKeyUsage' (if set, its value should be either emailProtection or anyExtendedKeyUsage)", async done => {
        try {
            await checkCertificate(aliceCert); // emailProtection set
            await checkCertificate(pki.certificateFromPem(anyExtendedKeyUsageCert));
            await checkCertificate(pki.certificateFromPem(basicCert));
            done();
        } catch (e) {
            done.fail("those certificates have no extendedKeyUsage issue to be used for S/MIME");
        }
    });
    test("untrusted if expected email is not found neither in emailAddress or in 'Subject Alternative Name' extension", async done => {
        try {
            await checkCertificate(aliceCert, { expectedAddress: "alice@smime.example" });
        } catch {
            done.fail("alice@smime.example is set in certificate");
        }
        const expectedAddress = "notfound@mail.com";
        try {
            await checkCertificate(aliceCert, { expectedAddress });
            done.fail("email found in certificate should match expected one");
        } catch (e) {
            expect(e instanceof UntrustedCertificateEmailNotFoundError).toBe(true);
            expect((<UntrustedCertificateEmailNotFoundError>e).message.includes(expectedAddress));
            done();
        }
    });

    test("untrusted if keyUsage does not match expected usage", async done => {
        try {
            await checkCertificate(aliceCert, { smimeUsage: SMIME_CERT_USAGE.SIGN });
            await checkCertificate(aliceCert, { smimeUsage: SMIME_CERT_USAGE.ENCRYPT });
            done.fail("this certificate cannot be used for encryption");
        } catch (e) {
            done();
        }
    });

    test("untrusted if cert is revoked", async done => {
        const revocation = {
            status: RevocationResult.RevocationStatus.REVOKED,
            revocation: { revocationDate: new Date().getTime() }
        };
        fetchMock.mock("end:/api/smime_revocation/foo.bar/revoked_clients", [revocation], {
            overwriteRoutes: true
        });
        try {
            await checkCertificate(aliceCert);
            done.fail("revoked certificate must not be trusted");
        } catch (error) {
            expect(error).toBeInstanceOf(UntrustedCertificateError);
            done();
        }
    });

    test("cert is trusted if date checked is before revokation", async done => {
        const revocation = {
            status: RevocationResult.RevocationStatus.REVOKED,
            revocation: { revocationDate: new Date("2023-02-01").getTime() }
        };
        fetchMock.mock("end:/api/smime_revocation/foo.bar/revoked_clients", [revocation], {
            overwriteRoutes: true
        });
        try {
            await checkCertificate(aliceCert, { date: new Date("2023-01-01") });
            done();
        } catch (e) {
            done.fail("certificate is revoked but checked date was before revokation happened");
        }
    });
});

describe("check revocation", () => {
    test("revocation results are cached", async () => {
        const aliceCertificate = pki.certificateFromPem(aliceCert);
        const route = "end:/api/smime_revocation/foo.bar/revoked_clients";
        fetchMock.mock(
            route,
            [
                {
                    status: RevocationResult.RevocationStatus.NOT_REVOKED,
                    revocation: { serialNumber: aliceCertificate.serialNumber }
                }
            ],
            { overwriteRoutes: true }
        );
        await checkRevoked(aliceCertificate);
        expect(fetchMock.calls(route).length).toEqual(1);

        await checkRevoked(aliceCertificate);
        expect(fetchMock.calls(route).length).toEqual(1);
    });

    test("if certificate is not revoked, cache is valid 7 days", async () => {
        const now = new Date();
        const validity = getRevocationCacheValidity({
            status: RevocationResult.RevocationStatus.NOT_REVOKED,
            revocation: { issuer: "", serialNumber: "" }
        });
        const diff = validity.getTime() - now.getTime();

        const days = Math.ceil(diff / (1000 * 3600 * 24));
        expect(days).toBe(7);
    });

    test("if certificate is revoked but for 'certificateHold' reason, cache is valid only one day", async () => {
        const now = new Date();
        const validity = getRevocationCacheValidity({
            status: RevocationResult.RevocationStatus.REVOKED,
            revocation: { issuer: "", revocationReason: "certificateHold", serialNumber: "" }
        });
        const diff = validity.getTime() - now.getTime();

        const days = Math.round(diff / (1000 * 3600 * 24));
        expect(days).toBe(1);
    });

    test("if certificate is revoked, cache is valid forever", async () => {
        const MAX_TIMESTAMP = 8640000000000000;

        const validity = getRevocationCacheValidity({
            status: RevocationResult.RevocationStatus.REVOKED,
            revocation: { issuer: "", serialNumber: "" }
        });

        expect(validity.getTime()).toBe(MAX_TIMESTAMP);
    });
});
