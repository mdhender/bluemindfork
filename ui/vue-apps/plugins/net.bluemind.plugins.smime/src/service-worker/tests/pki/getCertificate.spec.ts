import fetchMock from "fetch-mock";
import { VCardQuery } from "@bluemind/addressbook.api";
import { RevocationResult } from "@bluemind/smime.cacerts.api";
import { readFile } from "../helpers";
import { SMIME_CERT_USAGE } from "../../../lib/constants";
import {
    CertificateRecipientNotFoundError,
    InvalidCertificateError,
    UntrustedCertificateError
} from "../../../lib/exceptions";
import getCertificate from "../../pki/getCertificate";

const basicCACert = readFile("certificates/basicCA.crt");
const basicCert = readFile("certificates/basicCert.crt");
const otherCertificate = readFile("certificates/otherCertificate.crt");
const invalidCertificate = readFile("certificates/invalid.crt");

fetchMock.mock("/session-infos", { userId: "baz", domain: "foo.bar" });
fetchMock.mock("end:/api/smime_cacerts/smime_cacerts:domain_foo.bar/_all", [{ value: { cert: basicCACert } }], {
    overwriteRoutes: true
});

const mockMultipleGet = jest.fn(uids => {
    if (uids.includes("UID_alice@smime.example")) {
        return [
            {
                value: { security: { keys: [{ parameters: [], value: invalidCertificate }] } },
                uid: "UID_invalid"
            },
            {
                value: { security: { keys: [{ parameters: [], value: otherCertificate }] } },
                uid: "UID_other"
            }
        ];
    } else if (uids.includes("UID_test@devenv.blue")) {
        return [
            {
                value: { security: { keys: [{ parameters: [], value: invalidCertificate }] } },
                uid: "UID_invalid"
            },
            {
                value: { security: { keys: [{ parameters: [], value: otherCertificate }] } },
                uid: "UID_other"
            },
            {
                value: { security: { keys: [{ parameters: [], value: basicCert }] } },
                uid: "UID_test@devenv.blue"
            }
        ];
    } else if (uids.includes("2DF7A15F-12FD-4864-8279-12ADC6C08BAF")) {
        return [
            {
                value: { security: { keys: [{ parameters: [], value: otherCertificate }] } },
                uid: "2DF7A15F-12FD-4864-8279-12ADC6C08BAF"
            }
        ];
    } else if (uids.includes("invalid")) {
        return [
            {
                value: { security: { keys: [{ parameters: [], value: invalidCertificate }] } },
                uid: "invalid"
            }
        ];
    }
    return [];
});
jest.mock("@bluemind/addressbook.api", () => {
    return {
        AddressBooksClient: jest.fn().mockImplementation(() => ({
            search: (searchQuery: VCardQuery) => {
                if (searchQuery.query!.includes("alice@smime.example")) {
                    return {
                        total: 1,
                        values: [
                            {
                                containerUid: "addressbook_f8de2c4a.internal",
                                value: { mail: "alice@smime.example", hasSecurityKey: true },
                                uid: "UID_alice@smime.example"
                            }
                        ]
                    };
                } else if (searchQuery.query!.includes("test@devenv.blue")) {
                    return {
                        total: 1,
                        values: [
                            {
                                containerUid: "addressbook_f8de2c4a.internal",
                                value: { mail: "test@devenv.blue", hasSecurityKey: true },
                                uid: "UID_test@devenv.blue"
                            }
                        ]
                    };
                } else if (searchQuery.query!.includes("test@mail.com")) {
                    return {
                        total: 2,
                        values: [
                            {
                                containerUid: "addressbook_2",
                                value: { mail: "deux@devenv.blue", hasSecurityKey: false },
                                uid: "AAA"
                            },
                            {
                                containerUid: "addressbook_f8de2c4a.internal",
                                value: { mail: "deux@devenv.blue", hasSecurityKey: true },
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
                                value: { mail: "invalid@devenv.blue", hasSecurityKey: true },
                                uid: "invalid"
                            }
                        ]
                    };
                }
                return { total: 0, values: [] };
            }
        })),
        AddressBookClient: jest.fn().mockImplementation(() => ({
            multipleGet: mockMultipleGet
        })),
        VCardQuery: { OrderBy: { Pertinance: "Pertinance" } }
    };
});

describe("getCertificate", () => {
    beforeEach(() => {
        mockMultipleGet.mockClear();
        fetchMock.mock(
            "end:/api/smime_revocation/foo.bar/revoked_clients",
            [
                {
                    status: RevocationResult.RevocationStatus.NOT_REVOKED,
                    revocation: { serialNumber: "myCertSerialNumber" }
                }
            ],
            { overwriteRoutes: true }
        );
        jest.useFakeTimers("modern").setSystemTime(new Date("2023-03-17").getTime());

    });
    afterEach(() => {
        jest.useRealTimers()
    })
    test("get first trusted certificate", async () => {
        const certificate = await getCertificate("test@devenv.blue", SMIME_CERT_USAGE.ENCRYPT);
        expect(certificate).toBeTruthy();
    });

    test("raise an error if no certificate found", async () => {
        await expect(() => getCertificate("unknown", SMIME_CERT_USAGE.ENCRYPT)).rejects.toThrowError(
            CertificateRecipientNotFoundError
        );
    });

    test("raise last error if no trusted certificate found", async () => {
        await expect(() => getCertificate("alice@smime.example", SMIME_CERT_USAGE.ENCRYPT)).rejects.toThrowError(
            UntrustedCertificateError
        );
    });
    test("raise an error if the recipient certificate is invalid", async () => {
        await expect(() => getCertificate("invalid@mail.com", SMIME_CERT_USAGE.ENCRYPT)).rejects.toThrowError(
            InvalidCertificateError
        );
    });
});
