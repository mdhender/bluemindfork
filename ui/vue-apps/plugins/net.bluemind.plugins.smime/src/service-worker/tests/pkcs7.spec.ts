import fs from "fs";
import { pki } from "node-forge";
import path from "path";
import { CRYPTO_HEADERS } from "../../lib/constants";
import { base64ToArrayBuffer } from "@bluemind/arraybuffer";
import extractSignedData from "../smime/SMimeSignedDataParser";
import {
    DecryptError,
    EncryptError,
    InvalidMessageIntegrityError,
    InvalidSignatureError,
    SmimeErrors,
    UnmatchedCertificateError
} from "../exceptions";
import { readFile } from "./helpers";
import { checkSignatureValidity, getSignedDataEnvelope, checkMessageIntegrity } from "../pkcs7/verify";
import pkcs7 from "../pkcs7/";

class MockedBlob extends Blob {
    arrayBuffer() {
        return Promise.resolve(base64ToArrayBuffer(readTxt("parts/encryptedPart")));
    }
}

class MultipleRecipientsMockedBlob extends Blob {
    arrayBuffer() {
        return Promise.resolve(base64ToArrayBuffer(readTxt("parts/encryptedMultiRecipients")));
    }
}

const privatekeyTxt = readTxt("documents/privateKey");
const otherPrivateKey = readTxt("documents/otherPrivateKey");
const certificateTxt = readTxt("documents/certificate");
const otherCertificateTxt = readTxt("documents/otherCertificate");
const mockKey = pki.privateKeyFromPem(privatekeyTxt);
const mockCertificate = pki.certificateFromPem(certificateTxt);
const mockOtherCertificate = pki.certificateFromPem(otherCertificateTxt);

describe("pkcs7", () => {
    describe("decrypt", () => {
        test("decrypt pkc7 part if the right private key is given", async () => {
            const res = await pkcs7.decrypt(new MockedBlob(), mockKey, mockCertificate);
            expect(res).toMatchSnapshot();
        });
        test("select the right recipient if multiple recipient are present", async () => {
            const res = await pkcs7.decrypt(new MultipleRecipientsMockedBlob(), mockKey, mockCertificate);
            expect(res).toMatchSnapshot();
        });

        test("raise an error if the given certificate does not match any recipient", async () => {
            try {
                await pkcs7.decrypt(new MockedBlob(), mockKey, mockOtherCertificate);
            } catch (error) {
                expect(error).toBeInstanceOf(UnmatchedCertificateError);
            }
        });

        test("raise an error on decrypt failure", async () => {
            const mockKey = pki.privateKeyFromPem(otherPrivateKey);
            try {
                await pkcs7.decrypt(new MockedBlob(), mockKey, mockCertificate);
            } catch (error) {
                expect(error).toBeInstanceOf(DecryptError);
            }
        });
    });

    describe("verify", () => {
        const eml = readSignedOnly("valid.eml");
        const { pkcs7Part: validPkcs7Part, toDigest: validToDigest } = extractSignedData(eml);
        const validEnvelope = getSignedDataEnvelope(validPkcs7Part);

        const { pkcs7Part: invalidPkcs7Part, toDigest: invalidToDigest } = extractSignedData(
            readSignedOnly("invalid_signature.eml")
        );
        const invalidSignatureEnvelope = getSignedDataEnvelope(invalidPkcs7Part);

        const corruptedEml = readSignedOnly("corrupted.eml");
        const { pkcs7Part: corruptedPkcs7Part, toDigest: corruptedToDigest } = extractSignedData(corruptedEml);
        const corruptedEnvelope = getSignedDataEnvelope(corruptedPkcs7Part);

        test("verify a valid eml", async done => {
            try {
                await pkcs7.verify(validPkcs7Part, validToDigest);
                done();
            } catch {
                done.fail("failed verify a valid signed eml.");
            }
        });

        test("verify invalid or corrupted eml throw an exception", async done => {
            try {
                await pkcs7.verify(invalidPkcs7Part, invalidToDigest);
                done.fail();
            } catch {
                try {
                    await pkcs7.verify(corruptedPkcs7Part, corruptedToDigest);
                    done.fail();
                } catch {
                    done();
                }
            }
        });

        test("check if signature matches authenticate attributes", done => {
            try {
                checkSignatureValidity(validEnvelope, validEnvelope.certificates[0]);
            } catch {
                done.fail();
            }

            try {
                checkSignatureValidity(invalidSignatureEnvelope, invalidSignatureEnvelope.certificates[0]);
                done.fail();
            } catch (error) {
                expect(error).toBeInstanceOf(InvalidSignatureError);
                expect((<SmimeErrors>error).code).toBe(CRYPTO_HEADERS.INVALID_SIGNATURE);
                done();
            }
        });

        test("check message integrity", done => {
            try {
                checkMessageIntegrity(validEnvelope, validToDigest);
            } catch {
                done.fail();
            }

            try {
                checkMessageIntegrity(corruptedEnvelope, corruptedToDigest);
                done.fail();
            } catch (error) {
                expect(error).toBeInstanceOf(InvalidMessageIntegrityError);
                expect((<SmimeErrors>error).code).toBe(CRYPTO_HEADERS.INVALID_MESSAGE_INTEGRITY);
                done();
            }
        });
    });
    describe("encrypt", () => {
        test("encrypt message with my own certificate", async () => {
            const result = pkcs7.encrypt("hello", [mockCertificate]);
            expect(result).toBeTruthy();
        });
        test("error in encrypt raise an error", async () => {
            try {
                pkcs7.encrypt("hello", [mockOtherCertificate]);
            } catch (error) {
                expect(error).toBeInstanceOf(EncryptError);
            }
        });
    });
    describe("sign", () => {
        test("sign message returns a valid base64", async done => {
            const signed = await pkcs7.sign("blabla", mockKey, mockCertificate);
            try {
                atob(signed);
                done();
            } catch {
                done.fail("sign return is not a valid base64");
            }
        });
    });
});

function readTxt(file: string) {
    return readFile(`${file}.txt`);
}

function readSignedOnly(filename: string) {
    return fs.readFileSync(path.join(__dirname, `./data/eml/signed_only/${filename}`), "utf8");
}
