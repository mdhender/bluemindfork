import { Request } from "node-fetch";
import fetchMock from "fetch-mock";
import { MimeType } from "@bluemind/email";
import decrypt from "../smime/decrypt";
import encrypt from "../smime/encrypt";
import verify from "../smime/verify";
import sign from "../smime/sign";
import pkcs7 from "../pkcs7";
import pki from "../pki/";
import forge from "node-forge";
import {
    EncryptError,
    InvalidSignatureError,
    SignError,
    UntrustedCertificateError,
    UnmatchedCertificateError
} from "../../lib/exceptions";
import {
    CRYPTO_HEADERS,
    ENCRYPTED_HEADER_NAME,
    SIGNED_HEADER_NAME,
    SMIME_ENCRYPTION_ERROR_PREFIX,
    SMIME_SIGNATURE_ERROR_PREFIX
} from "../../lib/constants";
import { getHeaderValue, isEncrypted, isVerified } from "../../lib/helper";
import { readFile } from "./helpers";

fetchMock.mock("/session-infos", {
    login: "mathilde.michau@blue-mind.net",
    sid: "58a1ee1b-0c30-492c-a83f-4396f0a24730",
    defaultEmail: "math@devenv.blue"
});

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
jest.mock("../smime/cache/SMimePartCache", () => ({
    ...jest.requireActual("../smime/cache/SMimePartCache"),
    getGuid: () => Promise.resolve("99")
}));

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

class MockResponse {}
class MockFetchEvent extends Event {}
global.Request = Request;
global.Response = MockResponse;
global.FetchEvent = MockFetchEvent;

const mainEncrypted = {
    value: {
        body: {
            recipients: [
                {
                    kind: "Originator",
                    dn: "math",
                    address: "math@devenv.blue"
                }
            ],
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
            recipients: [
                {
                    kind: "Originator",
                    dn: "math",
                    address: "math@devenv.blue"
                }
            ],
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

const certificateTxt = readFile("certificates/certificate.crt");
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
        pki.checkCertificate = () => {};
        jest.clearAllMocks();
    });

    describe("isEncrypted", () => {
        test("return true if the message main part is crypted", () => {
            expect(isEncrypted(mainEncrypted.value.body.structure)).toBe(true);
        });
        test("return false if there is not crypted subpart", () => {
            expect(isEncrypted(unencrypted)).toBe(false);
        });
    });
    describe("decrypt", () => {
        beforeAll(() => {
            fetchMock.mock("*", new Response());
        });
        beforeEach(() => {
            mainEncrypted.value.body.headers = [];
            mockCache = {};
        });
        test("adapt message body structure when the main part is encrypted", async () => {
            const { body } = await decrypt("uid", mainEncrypted);
            expect(body.structure).toEqual(expect.objectContaining({ mime: "text/plain", address: "1" }));
        });
        test("add decrypted parts content to part cache", async () => {
            const mockEmlMultipleParts = readEml("eml/unencrypted");
            pkcs7.decrypt = () => Promise.resolve(mockEmlMultipleParts);

            await decrypt("uid", mainEncrypted);
            expect(mockCache).toMatchSnapshot();
        });
        test("add a header if the message is crypted", async () => {
            const { body } = await decrypt("uid", mainEncrypted);
            expect(getCryptoHeaderCode(body)).toBeTruthy();
        });
        test("add a header if the message is correcty decrypted", async () => {
            const { body } = await decrypt("uid", mainEncrypted);
            expect(getCryptoHeaderCode(body) & CRYPTO_HEADERS.OK).toBeTruthy();
        });
        test("add a header if the message cannot be decrypted because certificate is untrusted", async () => {
            pkcs7.decrypt = () => Promise.reject(new UntrustedCertificateError());

            const { body } = await decrypt("uid", mainEncrypted);
            expect(getCryptoHeaderCode(body) & CRYPTO_HEADERS.UNTRUSTED_CERTIFICATE).toBeTruthy();
        });
        test("add a header if the given certificate does not match any recipient", async () => {
            pkcs7.decrypt = () => Promise.reject(new UnmatchedCertificateError());

            const { body } = await decrypt("uid", mainEncrypted);
            expect(getCryptoHeaderCode(body) & CRYPTO_HEADERS.UNMATCHED_CERTIFICATE).toBeTruthy();
        });
    });
    describe("encrypt", () => {
        beforeEach(() => {
            pkcs7.encrypt = () => "encrypted";
            pki.getMyCertificate = () => Promise.resolve(mockCertificate);
        });
        test("adapt message body structure and upload new encrypted part when the main part has to be encrypted ", async () => {
            const structure = await encrypt(item, "folderUid");
            expect(mockUploadPart).toHaveBeenCalled();
            expect(structure.body.structure.address).toBe("address");
            expect(structure.body.structure.mime).toBe(MimeType.PKCS_7);
        });
        test("raise an error if the message cannot be encrypted", async () => {
            pkcs7.encrypt = () => {
                throw new EncryptError();
            };
            try {
                await encrypt(item, "folderUid");
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
            const signedItem = await sign(item, "folderUid");
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
                await sign(item, "folderUid");
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
            const verified = await verify(itemValue, getEml);
            expect(isVerified(verified.headers)).toBe(true);
        });
        test("add a KO header if item cant be verified", async () => {
            pkcs7.verify = () => {
                throw new InvalidSignatureError();
            };
            const itemValue = { value: item };
            const verified = await verify(itemValue, getEml);
            expect(isVerified(verified.headers)).toBe(false);
            const headerValue = getHeaderValue(item.body.headers, SIGNED_HEADER_NAME);
            expect(Boolean(headerValue & CRYPTO_HEADERS.INVALID_SIGNATURE)).toBe(true);
        });
    });
});

function readEml(file) {
    return readFile(`${file}.eml`);
}

function getCryptoHeaderCode(body) {
    const code = body.headers.find(h => h.name === ENCRYPTED_HEADER_NAME).values[0];
    return parseInt(code);
}
