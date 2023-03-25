import { MessageBody } from "@bluemind/backend.mail.api";
import signedBuilder from "../smime/helpers/SMimeSignedEmlBuilder";

const unsigned =
    "MIME-Version: 1.0\r\n" +
    "Date: Thu, 3 Nov 2022 16:37:41 +0100\r\n" +
    "Content-Type: text/plain; charset=UTF-8\r\n" +
    "Content-Transfer-Encoding: 7bit\r\n\r\n" +
    "Hello unsigned world\r\n";

const fakedSignedContent = `MIAGCSqGSIb3DQEHAqCAMIACAQExDzANBglghkgBZQMEAgEFADCABgkqhkiG9w0BBwEAAKCCA`;

const body: MessageBody = {
    date: new Date().getTime(),
    headers: [],
    recipients: [],
    subject: "Signed mail"
};

describe("SMimeSignedEmlBuilder", () => {
    test("build a signed eml", async () => {
        const eml = await signedBuilder(unsigned, fakedSignedContent, body);
        const rootContentType = `Content-Type: multipart/signed; protocol="application/pkcs7-signature";\r\n micalg=sha-256; boundary="`;
        expect(eml.includes(rootContentType)).toBe(true);
        expect(eml.includes(fakedSignedContent)).toBe(true);

        const lines = eml.split("\r\n");
        const i = lines.indexOf("Content-Type: application/pkcs7-signature; name=smime.p7s");
        expect(i > -1).toBe(true);
        expect(lines[i + 1]).toBe("Content-Transfer-Encoding: base64");
        expect(lines[i + 2]).toBe("Content-Disposition: attachment; filename=smime.p7s");

        expect(lines.includes("Hello unsigned world")).toBe(true);
        expect(lines.includes("Content-Type: text/plain; charset=UTF-8")).toBe(true);
    });
});
