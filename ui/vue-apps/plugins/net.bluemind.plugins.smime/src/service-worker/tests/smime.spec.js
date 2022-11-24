import smime from "../smime";
import pkcs7 from "../pkcs7";
import pki from "../pki";

import {
    InvalidCredentialsError,
    ExpiredCredentialsError,
    RevokedCrendentialsError,
    UntrustedCredentialsError,
    RecipientNotFoundError
} from "../exceptions";
import { CRYPTO_HEADERS, ENCRYPTED_HEADER_NAME } from "../../lib/constants";
import { readFile } from "./helpers";

jest.mock("../pki", () => jest.fn);
jest.mock("@bluemind/backend.mail.api", () => ({
    MailboxItemsClient: () => ({
        fetch: () => Promise.resolve("data")
    })
}));
jest.mock("../pkcs7", () => jest.fn);

let mockCache = {};
global.caches = {
    open: () => {
        return Promise.resolve({
            put: (url, response) => {
                mockCache[url] = response;
            }
        });
    }
};
global.fetch = jest.fn(() =>
    Promise.resolve({
        json: () =>
            Promise.resolve({
                login: "mathilde.michau@blue-mind.net",
                sid: "58a1ee1b-0c30-492c-a83f-4396f0a24730"
            }),
        ok: () => true
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
            headers: []
        },
        imapUid: 99,
        internalDate: 1668534530000
    }
};

const unecrypted = {
    value: {
        body: {
            structure: {
                mime: "multipart/alternative",
                address: "TEXT",
                children: [{ mime: "text/plain" }, { mime: "text/html" }]
            },
            headers: [],
            internalDate: 1668534530
        }
    }
};
describe("smime", () => {
    beforeEach(() => {
        pkcs7.decrypt = jest.fn(() => Promise.resolve("content"));
        pki.getMyPrivateKey = jest.fn(() => Promise.resolve("PrivateKey"));
        pki.getMyCertificate = jest.fn(() =>
            Promise.resolve({
                validity: {
                    notBefore: new Date("2022-09-25T13:43:26.000Z"),
                    notAfter: new Date("2023-09-25T13:43:26.000Z")
                }
            })
        );
        pki.isCertificateExpired = jest.fn(() => false);
    });

    describe("isEncrypted", () => {
        test("return true if the message main part is crypted", () => {
            const isEncrypted = smime.isEncrypted(mainEncrypted);
            expect(isEncrypted).toBe(true);
        });
        test("return true if a subpart of the message  is crypted", () => {});
        test("return true if multiple subpart of the message  is crypted", () => {});
        test("return false if there is not crypted subpart", () => {
            const isEncrypted = smime.isEncrypted(unecrypted);
            expect(isEncrypted).toBe(false);
        });
    });
    describe("decrypt", () => {
        beforeEach(() => {
            mainEncrypted.value.body.headers = [];
            mockCache = {};
        });
        test("adapt message body structure when the main part is encrypted", async () => {
            const structure = await smime.decrypt("uid", mainEncrypted);
            expect(structure.value.body.structure).toEqual(
                expect.objectContaining({ mime: "text/plain", address: "1" })
            );
        });
        test("adapt message body structure when a subpart is crypted", () => {});
        test("adapt message body structure when multiple subpart are crypted", () => {});

        test("add decrypted parts content to part cache", async () => {
            const mockEmlMultipleParts = readEml("eml/unencrypted");
            pkcs7.decrypt = jest.fn(() => Promise.resolve(mockEmlMultipleParts));

            await smime.decrypt("uid", mainEncrypted);
            expect(mockCache).toMatchSnapshot();
        });
        test("raise an error if the certificate is expired", async () => {
            pki.isCertificateExpired = jest.fn(() => true);
            const old = {
                value: {
                    body: {
                        structure: {
                            address: "1",
                            mime: "application/pkcs7-mime"
                        },
                        headers: []
                    },
                    imapUid: 99,
                    internalDate: 1000
                }
            };
            const item = await smime.decrypt("uid", old);
            expect(getCryptoHeader(item).values[0]).toEqual(CRYPTO_HEADERS.EXPIRED_CREDENTIALS);
        });
        test("add a header if the message is crypted", async () => {
            const item = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeader(item)).toBeTruthy();
        });
        test("add a header if the message is correcty decrypted", async () => {
            const item = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeader(item).values[0]).toEqual(CRYPTO_HEADERS.DECRYPTED);
        });
        test("add a header if the message cannot be decrypted because private key or certificate are not valid", async () => {
            pkcs7.decrypt = jest.fn(() => Promise.reject(new InvalidCredentialsError()));

            const item = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeader(item).values[0]).toEqual(CRYPTO_HEADERS.INVALID_CREDENTIALS);
        });
        test("add a header if the message cannot be decrypted because private key or certificate are expired", async () => {
            pkcs7.decrypt = jest.fn(() => Promise.reject(new ExpiredCredentialsError()));

            const item = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeader(item).values[0]).toEqual(CRYPTO_HEADERS.EXPIRED_CREDENTIALS);
        });
        test("add a header if the message cannot be decrypted because private key or certificate are revoked", async () => {
            pkcs7.decrypt = jest.fn(() => Promise.reject(new RevokedCrendentialsError()));

            const item = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeader(item).values[0]).toEqual(CRYPTO_HEADERS.REVOKED_CREDENTIALS);
        });
        test("add a header if the message cannot be decrypted because private key or certificate are not trusted", async () => {
            pkcs7.decrypt = jest.fn(() => Promise.reject(new UntrustedCredentialsError()));

            const item = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeader(item).values[0]).toEqual(CRYPTO_HEADERS.UNTRUSTED_CREDENTIALS);
        });
        test("add a header if the given certificate does not match any recipient", async () => {
            pkcs7.decrypt = jest.fn(() => Promise.reject(new RecipientNotFoundError()));

            const item = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeader(item).values[0]).toEqual(CRYPTO_HEADERS.UNMATCHED_RECIPIENTS);
        });
    });
});

function readEml(file) {
    return readFile(`${file}.eml`);
}

function getCryptoHeader(item) {
    return item.value.body.headers.find(h => h.name === ENCRYPTED_HEADER_NAME);
}
