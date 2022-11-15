import pkcs7 from "../pkcs7";
import { pki } from "node-forge";
import { InvalidCredentialsError } from "../exceptions";
import { base64ToArrayBuffer, readFile } from "./helpers";

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

const privatekeyTxt = readTxt("credentials/privateKey");
const certificateTxt = readTxt("credentials/certificate");
const otherCertificateTxt = readTxt("credentials/otherCertificate");

describe("pkcs7", () => {
    describe("decrypt", () => {
        test("decrypt pkc7 part if the right private key is given", async () => {
            const mockKey = pki.privateKeyFromPem(privatekeyTxt);
            const mockCertificate = pki.certificateFromPem(certificateTxt);
            const res = await pkcs7.decrypt(blob, mockKey, mockCertificate);
            expect(res).toMatchSnapshot();
        });
        test("raise an error if the private key is wrong", async () => {
            const mockCertificate = pki.certificateFromPem(certificateTxt);
            try {
                await pkcs7.decrypt(blob, "what", mockCertificate);
            } catch (error) {
                expect(error).toBeInstanceOf(InvalidCredentialsError);
            }
        });

        test("select the right recipient if multiple recipient are present", async () => {
            const mockKey = pki.privateKeyFromPem(privatekeyTxt);
            const mockCertificate = pki.certificateFromPem(certificateTxt);
            const res = await pkcs7.decrypt(blobMultipleRecipients, mockKey, mockCertificate);
            expect(res).toMatchSnapshot();
        });

        test("raise an error if the given certificate does not match any recipient", async () => {
            const mockKey = pki.privateKeyFromPem(privatekeyTxt);
            const mockOtherCertificateTxt = pki.certificateFromPem(otherCertificateTxt);
            try {
                await pkcs7.decrypt(blob, mockKey, mockOtherCertificateTxt);
            } catch (error) {
                expect(error).toBeInstanceOf(InvalidCredentialsError);
            }
        });
    });
});

function readTxt(file) {
    return readFile(`${file}.txt`);
}
