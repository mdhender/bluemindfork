import fs from "fs";
import { pki } from "node-forge";
import path from "path";
import { CRYPTO_HEADERS } from "../../lib/constants";
import { base64ToArrayBuffer } from "@bluemind/arraybuffer";
import extractSignedData from "../signedDataParser";
import {
    DecryptError,
    EncryptError,
    InvalidMessageIntegrityError,
    InvalidSignatureError,
    UnmatchedCertificateError
} from "../exceptions";
import { readFile } from "./helpers";
import { checkSignatureValidity, getSignedDataEnvelope, checkMessageIntegrity } from "../pkcs7/verify";
import pkcs7 from "../pkcs7/";

const blob = {
    arrayBuffer() {
        return base64ToArrayBuffer(readTxt("parts/encryptedPart"));
    }
};
const blobMultipleRecipients = {
    arrayBuffer() {
        return base64ToArrayBuffer(readTxt("parts/encryptedMultiRecipients"));
    }
};

const privatekeyTxt = readTxt("documents/privateKey");
const otherPrivateKey = readTxt("documents/otherPrivateKey");
const certificateTxt = readTxt("documents/certificate");
const otherCertificateTxt = readTxt("documents/otherCertificate");
const mockKey = pki.privateKeyFromPem(privatekeyTxt);
const mockCertificate = pki.certificateFromPem(certificateTxt);

describe("pkcs7", () => {
    describe("decrypt", () => {
        test("decrypt pkc7 part if the right private key is given", async () => {
            const res = await pkcs7.decrypt(blob, mockKey, mockCertificate);
            expect(res).toMatchSnapshot();
        });
        test("select the right recipient if multiple recipient are present", async () => {
            const res = await pkcs7.decrypt(blobMultipleRecipients, mockKey, mockCertificate);
            expect(res).toMatchSnapshot();
        });

        test("raise an error if the given certificate does not match any recipient", async () => {
            const mockOtherCertificateTxt = pki.certificateFromPem(otherCertificateTxt);
            try {
                await pkcs7.decrypt(blob, mockKey, mockOtherCertificateTxt);
            } catch (error) {
                expect(error).toBeInstanceOf(UnmatchedCertificateError);
            }
        });

        test("raise an error on decrypt failure", async () => {
            const mockKey = pki.privateKeyFromPem(otherPrivateKey);
            try {
                await pkcs7.decrypt(blob, mockKey, mockCertificate);
            } catch (error) {
                expect(error).toBeInstanceOf(DecryptError);
            }
        });
    });

    describe("verify", () => {
        const { envelope: validEnvelope, toDigest } = getEnvelopeFromEml("valid.eml");
        const invalidSignatureEnvelope = getEnvelopeFromEml("invalid_signature.eml").envelope;

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
                expect(error.code).toBe(CRYPTO_HEADERS.INVALID_SIGNATURE);
                done();
            }
        });

        test("check message integrity", done => {
            try {
                checkMessageIntegrity(validEnvelope, toDigest);
            } catch {
                done.fail();
            }

            const { envelope, toDigest: invalidDigest } = getEnvelopeFromEml("corrupted.eml");
            try {
                checkMessageIntegrity(envelope, invalidDigest);
                done.fail();
            } catch (error) {
                expect(error).toBeInstanceOf(InvalidMessageIntegrityError);
                expect(error.code).toBe(CRYPTO_HEADERS.INVALID_MESSAGE_INTEGRITY);
                done();
            }
        });
    });
    describe("encrypt", () => {
        beforeEach(() => {});
        test("encrypt message with my own certificate", async () => {
            const result = pkcs7.encrypt("hello", [mockCertificate]);
            expect(result).toBeTruthy();
        });
        test("error in encrypt raise an error", async () => {
            try {
                pkcs7.encrypt("hello", ["wrongCertificate"]);
            } catch (error) {
                expect(error).toBeInstanceOf(EncryptError);
            }
        });
    });
});

function readTxt(file) {
    return readFile(`${file}.txt`);
}

function getEnvelopeFromEml(filename) {
    const eml = readSignedOnly(filename);
    const { pkcs7Part, toDigest } = extractSignedData(eml);
    return { envelope: getSignedDataEnvelope(pkcs7Part), toDigest };
}

function readSignedOnly(filename) {
    return fs.readFileSync(path.join(__dirname, `./data/eml/signed_only/${filename}`), "utf8", (err, data) => data);
}
