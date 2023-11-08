import FDBFactory from "fake-indexeddb/lib/FDBFactory";
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
        global.indexedDB = new FDBFactory();
        fetchMock.reset();

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
        /**
         * checkCertificate strong coupling with Date Object - 
         * test failing because of date in data-test-certificates was overdue
         * TODO: find a way to extract dependency from checkCertificate 
         *  */
        jest.useFakeTimers("modern").setSystemTime(new Date("2023-03-17").getTime());

    });

    afterEach(() => {
        resetCaCerts();
        fetchMock.reset();
        jest.useRealTimers()
    });

    test("untrusted if date is not within certificate validity period", async () => {
        await expect(checkCertificate(aliceCert, { date: new Date("2019-11-01") })).rejects.toThrowError(
            UntrustedCertificateError
        );
        await expect(checkCertificate(aliceCert, { date: new Date("2019-11-01") })).rejects.toThrow(
            "forge.pki.CertificateExpired"
        );
    });

    test("untrusted if no CA cert set", async () => {
        fetchMock.once("end:/api/smime_cacerts/smime_cacerts:domain_foo.bar/_all", [], {
            overwriteRoutes: true
        });
        await expect(checkCertificate(aliceCert)).rejects.toThrowError(UntrustedCertificateError);
        await expect(checkCertificate(aliceCert)).rejects.toThrow(
            "Untrusted certificate:  could not find any trusted CA certificates"
        );
    });

    test("untrusted if cert has expired", async () => {
        await expect(checkCertificate(expiredCert)).rejects.toThrowError(UntrustedCertificateError);
        await expect(checkCertificate(expiredCert)).rejects.toThrow("forge.pki.CertificateExpired");
    });

    test("untrusted if cert has been corrupted (invalid signature)", async () => {
        const expected = expect(checkCertificate(corruptedCert)).rejects;
        await expected.toThrowError(UntrustedCertificateError);
        await expected.toThrow("forge.pki.BadCertificate");
    });

    test("untrusted if CA issuer is not trusted", async () => {
        const expected = expect(checkCertificate(otherCertificate)).rejects;
        await expected.toThrowError(UntrustedCertificateError);
        await expected.toThrow("forge.pki.UnknownCertificateAuthority");
    });
    test("cant use a CA certificate", done => {
        checkCertificate(aliceCA).catch(e => {
            expect(e instanceof UntrustedCertificateError).toBe(true);
        });
        done();
    });

    test("cant use certificate because of its 'extendedKeyUsage' (if set, its value should be either emailProtection or anyExtendedKeyUsage)", async () => {
        await expect(checkCertificate(aliceCert)).resolves.toBeTruthy();
        await expect(checkCertificate(pki.certificateFromPem(anyExtendedKeyUsageCert))).resolves.toBeTruthy();
        await expect(checkCertificate(pki.certificateFromPem(basicCert))).resolves.toBeTruthy();
    });
    test("untrusted if expected email is not found neither in emailAddress or in 'Subject Alternative Name' extension", async () => {
        const expectedAddress = "notfound@mail.com";
        const expected = expect(checkCertificate(aliceCert, { expectedAddress })).rejects;

        await expected.toThrowError(UntrustedCertificateEmailNotFoundError);
        await expected.toThrow('email "notfound@mail.com" not found in certificate');
    });

    test("untrusted if keyUsage does not match expected usage", async () => {
        await expect(checkCertificate(aliceCert, { smimeUsage: SMIME_CERT_USAGE.SIGN })).resolves.toBeTruthy();
        await expect(checkCertificate(aliceCert, { smimeUsage: SMIME_CERT_USAGE.ENCRYPT })).rejects.toThrowError();
    });

    test("untrusted if cert is revoked", async () => {
        const revocation = {
            status: RevocationResult.RevocationStatus.REVOKED,
            revocation: { revocationDate: new Date().getTime() }
        };
        fetchMock.mock("end:/api/smime_revocation/foo.bar/revoked_clients", [revocation], {
            overwriteRoutes: true
        });
        await expect(checkCertificate(aliceCert)).rejects.toThrowError(UntrustedCertificateError);
    });

    test("cert is trusted if date checked is before revokation", async () => {
        const revocation = {
            status: RevocationResult.RevocationStatus.REVOKED,
            revocation: { revocationDate: new Date("2023-02-01").getTime() }
        };
        fetchMock.mock("end:/api/smime_revocation/foo.bar/revoked_clients", [revocation], {
            overwriteRoutes: true
        });
        await expect(checkCertificate(aliceCert, { date: new Date("2023-01-01") })).resolves.toBeTruthy();
    });
});

describe("check revocation", () => {
    beforeEach(() => {
        fetchMock.reset();

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
        fetchMock.reset();
    });
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
