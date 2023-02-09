import { MimeType } from "@bluemind/email";
import smime from "../smime";
import pkcs7 from "../pkcs7";
import pki from "../pki/";
import forge from "node-forge";
import {
    EncryptError,
    ExpiredCertificateError,
    InvalidSignatureError,
    RevokedCertificateError,
    SignError,
    UntrustedCertificateError,
    UnmatchedCertificateError
} from "../exceptions";
import {
    CRYPTO_HEADERS,
    ENCRYPTED_HEADER_NAME,
    SIGNED_HEADER_NAME,
    SMIME_ENCRYPTION_ERROR_PREFIX,
    SMIME_SIGNATURE_ERROR_PREFIX
} from "../../lib/constants";
import { getHeaderValue, isVerified } from "../../lib/helper";
import { readFile } from "./helpers";

jest.mock("../pki/", () => jest.fn);
jest.mock("@bluemind/mime", () => {
    return {
        ...jest.requireActual("@bluemind/mime"),
        MimeBuilder: () => ({
            build: () => "dummy structure"
        })
    };
});

const mockUploadPart = jest.fn(() => Promise.resolve("address"));
jest.mock("@bluemind/backend.mail.api", () => ({
    ...jest.requireActual("@bluemind/backend.mail.api"),
    MailboxItemsClient: () => ({
        fetch: () => Promise.resolve("data"),
        uploadPart: mockUploadPart
    })
}));
jest.mock("../pkcs7", () => jest.fn);

let mockCache = {};
global.caches = {
    open: () => {
        return Promise.resolve({
            put: (request, response) => {
                mockCache[request.url] = response;
            }
        });
    }
};
jest.mock("../environnment/session", () =>
    Promise.resolve({
        json: () =>
            Promise.resolve({
                login: "mathilde.michau@blue-mind.net",
                sid: "58a1ee1b-0c30-492c-a83f-4396f0a24730"
            })
    })
);

class MockRequest {
    constructor(url) {
        this.url = url;
    }
}
class MockResponse {}
global.Request = MockRequest;
global.Response = MockResponse;

const mainEncrypted = {
    value: {
        body: {
            structure: {
                address: "1",
                mime: "application/pkcs7-mime"
            },
            headers: [],
            date: 1668534530000
        },
        imapUid: 99
    }
};

const unencrypted = {
    value: {
        body: {
            structure: {
                mime: "multipart/alternative",
                address: "TEXT",
                children: [{ mime: "text/plain" }, { mime: "text/html" }]
            },
            headers: [],
            date: 1668534530
        }
    }
};

const certificateTxt = readTxt("documents/certificate");
const mockCertificate = forge.pki.certificateFromPem(certificateTxt);

describe("smime", () => {
    let item;
    beforeEach(() => {
        item = {
            body: {
                date: 1671032461777,
                subject: "Mail",
                headers: [],
                recipients: [
                    {
                        kind: "Primary",
                        dn: "math",
                        address: "math@devenv.blue"
                    },
                    {
                        kind: "Originator",
                        dn: "math",
                        address: "math@devenv.blue"
                    }
                ],
                messageId: "<lbntihyw.j2pop9bobhc0@devenv.blue>",
                structure: {
                    mime: "multipart/alternative",
                    children: [
                        {
                            mime: "text/plain",
                            address: "06a5ccf7-4094-4c7f-8533-eb99b072b28d",
                            encoding: "quoted-printable",
                            charset: "utf-8"
                        },
                        {
                            mime: "text/html",
                            address: "9ba724be-a4b9-4679-be0c-1c5207401d38",
                            encoding: "quoted-printable",
                            charset: "utf-8"
                        }
                    ]
                }
            },
            imapUid: 424,
            flags: ["\\Seen"]
        };

        pkcs7.decrypt = () => Promise.resolve("content");
        pki.getMyPrivateKey = () => Promise.resolve("PrivateKey");
        pki.getMyCertificate = () =>
            Promise.resolve({
                validity: {
                    notBefore: new Date("2022-09-25T13:43:26.000Z"),
                    notAfter: new Date("2023-09-25T13:43:26.000Z")
                }
            });
        pki.getCertificate = () => [];
        pki.checkCertificateValidity = () => {};
        jest.clearAllMocks();
    });

    describe("isEncrypted", () => {
        test("return true if the message main part is crypted", () => {
            const isEncrypted = smime.isEncrypted(mainEncrypted.value.body.structure);
            expect(isEncrypted).toBe(true);
        });
        test("return true if a subpart of the message is crypted", () => {});
        test("return true if multiple subpart of the message is crypted", () => {});
        test("return false if there is not crypted subpart", () => {
            const isEncrypted = smime.isEncrypted(unencrypted);
            expect(isEncrypted).toBe(false);
        });
    });
    describe("decrypt", () => {
        beforeEach(() => {
            mainEncrypted.value.body.headers = [];
            mockCache = {};
        });
        test("adapt message body structure when the main part is encrypted", async () => {
            const { item } = await smime.decrypt("uid", mainEncrypted);
            expect(item.value.body.structure).toEqual(expect.objectContaining({ mime: "text/plain", address: "1" }));
        });
        test("add decrypted parts content to part cache", async () => {
            const mockEmlMultipleParts = readEml("eml/unencrypted");
            pkcs7.decrypt = () => Promise.resolve(mockEmlMultipleParts);

            await smime.decrypt("uid", mainEncrypted);
            expect(mockCache).toMatchSnapshot();
        });
        test("raise an error if the certificate is expired", async () => {
            pki.checkCertificateValidity = () => {
                throw new ExpiredCertificateError();
            };
            const old = {
                value: {
                    body: {
                        structure: {
                            address: "1",
                            mime: "application/pkcs7-mime"
                        },
                        headers: [],
                        date: 1000
                    },
                    imapUid: 99
                }
            };
            const { item } = await smime.decrypt("uid", old);
            expect(getCryptoHeaderCode(item) & CRYPTO_HEADERS.EXPIRED_CERTIFICATE).toBeTruthy();
        });
        test("add a header if the message is crypted", async () => {
            const { item } = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeaderCode(item)).toBeTruthy();
        });
        test("add a header if the message is correcty decrypted", async () => {
            const { item } = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeaderCode(item) & CRYPTO_HEADERS.OK).toBeTruthy();
        });
        test("add a header if the message cannot be decrypted because private key or certificate are expired", async () => {
            pkcs7.decrypt = () => Promise.reject(new ExpiredCertificateError());

            const { item } = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeaderCode(item) & CRYPTO_HEADERS.EXPIRED_CERTIFICATE).toBeTruthy();
        });
        test("add a header if the message cannot be decrypted because private key or certificate are revoked", async () => {
            pkcs7.decrypt = () => Promise.reject(new RevokedCertificateError());

            const { item } = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeaderCode(item) & CRYPTO_HEADERS.REVOKED_CERTIFICATE).toBeTruthy();
        });
        test("add a header if the message cannot be decrypted because private key or certificate are not trusted", async () => {
            pkcs7.decrypt = () => Promise.reject(new UntrustedCertificateError());

            const { item } = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeaderCode(item) & CRYPTO_HEADERS.UNTRUSTED_CERTIFICATE).toBeTruthy();
        });
        test("add a header if the given certificate does not match any recipient", async () => {
            pkcs7.decrypt = () => Promise.reject(new UnmatchedCertificateError());

            const { item } = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeaderCode(item) & CRYPTO_HEADERS.UNMATCHED_CERTIFICATE).toBeTruthy();
        });
    });
    describe("encrypt", () => {
        beforeEach(() => {
            pkcs7.encrypt = () => "encrypted";
            pki.getMyCertificate = () => Promise.resolve(mockCertificate);
        });
        test("adapt message body structure and upload new encrypted part when the main part has to be encrypted ", async () => {
            const structure = await smime.encrypt(item, "folderUid");
            expect(mockUploadPart).toHaveBeenCalled();
            expect(structure.body.structure.address).toBe("address");
            expect(structure.body.structure.mime).toBe(MimeType.PKCS_7);
        });
        test("raise an error if the message cannot be encrypted", async () => {
            pkcs7.encrypt = () => {
                throw new EncryptError();
            };
            try {
                await smime.encrypt(item, "folderUid");
            } catch (error) {
                expect(error).toContain(SMIME_ENCRYPTION_ERROR_PREFIX);
            }
        });
    });
    describe("sign", () => {
        beforeEach(() => {
            pkcs7.sign = jest.fn(() => Promise.resolve("b64"));
        });
        test("adapt body structure and upload full eml", async () => {
            const signedItem = await smime.sign(item, "folderUid");
            const multipartSigned = signedItem.body.structure;
            expect(multipartSigned.mime).toBe("message/rfc822");
            expect(multipartSigned.children.length).toBe(0);
            expect(mockUploadPart).toHaveBeenCalledTimes(1);
        });
        test("raise an error if the message cannot be signed", async done => {
            try {
                pkcs7.sign = () => {
                    throw new SignError();
                };
                await smime.sign(item, "folderUid");
                done.fail();
            } catch (error) {
                expect(error).toContain(SMIME_SIGNATURE_ERROR_PREFIX);
                expect(error).toContain(CRYPTO_HEADERS.SIGN_FAILURE);
                done();
            }
        });
    });
    describe("verify", () => {
        const getEml = () => readEml("eml/signed_only/valid");
        beforeEach(() => {
            pkcs7.verify = () => Promise.resolve();
        });
        test("add a OK header if item is successfuly verified", async () => {
            const itemValue = { value: item };
            const verified = await smime.verify("folderUid", itemValue, getEml);
            expect(isVerified(verified.value.body.headers)).toBe(true);
        });
        test("add a KO header if item cant be verified", async () => {
            pkcs7.verify = () => {
                throw new InvalidSignatureError();
            };
            const itemValue = { value: item };
            const verified = await smime.verify("folderUid", itemValue, getEml);
            expect(isVerified(verified.value.body.headers)).toBe(false);
            const headerValue = getHeaderValue(item.body.headers, SIGNED_HEADER_NAME);
            expect(Boolean(headerValue & CRYPTO_HEADERS.INVALID_SIGNATURE)).toBe(true);
        });
    });
});

function readEml(file) {
    return readFile(`${file}.eml`);
}

function getCryptoHeaderCode(item) {
    const code = item.value.body.headers.find(h => h.name === ENCRYPTED_HEADER_NAME).values[0];
    return parseInt(code);
}

function readTxt(file) {
    return readFile(`${file}.txt`);
}
