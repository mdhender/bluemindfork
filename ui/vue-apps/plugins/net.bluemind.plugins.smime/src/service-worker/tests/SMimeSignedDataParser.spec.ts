import fs from "fs";
import path from "path";
import { arrayBufferToBase64 } from "@bluemind/arraybuffer";
import extractSignedData from "../smime/helpers/SMimeSignedDataParser";

describe("extract signed data from an eml", () => {
    test("extract valid informations", done => {
        const newLine = "\r\n";
        const files = readSignedOnlyEmls();
        const extracedData: Array<{ toDigest: ReturnType<typeof extractSignedData>["toDigest"]; pkcs7Text: any }> = [];

        files.forEach(eml => {
            const { toDigest, pkcs7Part } = extractSignedData(eml);
            const pkcs7Text = arrayBufferToBase64(pkcs7Part);
            extracedData.push({ toDigest, pkcs7Text });
            try {
                atob(pkcs7Text);
            } catch {
                done.fail();
            }
        });
        expect(extracedData.every(({ pkcs7Text }) => pkcs7Text.startsWith(newLine))).toBe(false);
        expect(extracedData.every(({ pkcs7Text }) => pkcs7Text.endsWith(newLine))).toBe(false);
        expect(extracedData.every(({ toDigest }) => toDigest.startsWith("Content-"))).toBe(true); // headers must be digested
        expect(extracedData.every(({ toDigest }) => toDigest.endsWith("\n\n\n"))).toBe(false); // headers must be digested
        expect(extracedData.every(({ toDigest }) => toDigest.endsWith("\n\n"))).toBe(true); // headers must be digested

        done();
    });
});

function readSignedOnlyEmls() {
    return ["corrupted.eml", "valid.eml", "invalid_signature.eml"].map(filename =>
        fs.readFileSync(path.join(__dirname, `./data/eml/signed_only/${filename}`), "utf8")
    );
}
