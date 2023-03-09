import { pkcs7, pki, asn1, util } from "node-forge";
import { binaryToArrayBuffer } from "@bluemind/arraybuffer";
import { MessageBody } from "@bluemind/backend.mail.api";
import { DecryptError, EncryptError, SignError, UnmatchedCertificateError } from "../../lib/exceptions";
import { checkMessageIntegrity, checkSignatureValidity, getSigningTime } from "./verify";
import { checkCertificate } from "../pki/";
import { getSignedDataEnvelope } from "../../lib/envelope";
import { SMIME_CERT_USAGE } from "../../lib/constants";

export async function decrypt(data: Blob, key: pki.rsa.PrivateKey, certificate: pki.Certificate): Promise<string> {
    const buffer = await data.arrayBuffer();
    const text = asn1.fromDer(new util.ByteStringBuffer(buffer));
    const envelope = <pkcs7.Captured<pkcs7.PkcsEnvelopedData>>pkcs7.messageFromAsn1(text);
    const recipient = envelope.findRecipient(certificate);
    if (!recipient) {
        throw new UnmatchedCertificateError();
    }
    try {
        envelope.decrypt(recipient, key);
        if (!envelope.content) {
            throw "after decrypt, no content set in pkcs7 envelope";
        }
        return envelope.content.toString();
    } catch (error) {
        throw new DecryptError(error);
    }
}

export function encrypt(content: string, certificates: pki.Certificate[]): Blob {
    try {
        const envelope = pkcs7.createEnvelopedData();
        certificates.forEach(certificate => envelope.addRecipient(certificate));
        envelope.content = util.createBuffer(content);
        envelope.encrypt();
        const bytes = asn1.toDer(envelope.toAsn1()).getBytes();
        const buffer = binaryToArrayBuffer(bytes);
        return new Blob([buffer]);
    } catch (error) {
        throw new EncryptError(error);
    }
}

export async function verify(pkcs7: ArrayBuffer, toDigest: string, body: MessageBody) {
    const envelope = getSignedDataEnvelope(pkcs7);
    const signingTime = getSigningTime(envelope);
    const certificate = envelope.certificates[0];
    const sender = body.recipients!.find((recipient: any) => recipient.kind === "Originator")!.address!;
    await checkCertificate(certificate, {
        date: signingTime,
        expectedAddress: sender,
        smimeUsage: SMIME_CERT_USAGE.SIGN
    });
    checkSignatureValidity(envelope, certificate);
    checkMessageIntegrity(envelope, toDigest);
}

// create PKCS#7 signed data with authenticatedAttributes
// attributes include: PKCS#9 content-type, message-digest, and signing-time
export async function sign(content: string, myPrivateKey: pki.rsa.PrivateKey, myCert: pki.Certificate) {
    try {
        const p7 = pkcs7.createSignedData();
        p7.content = util.createBuffer(content);
        p7.addCertificate(myCert);
        p7.addSigner({
            key: myPrivateKey,
            certificate: myCert,
            digestAlgorithm: pki.oids.sha256,
            authenticatedAttributes: [
                {
                    type: pki.oids.contentType,
                    value: pki.oids.data
                },
                {
                    type: pki.oids.messageDigest // value will be auto-populated at signing time
                },
                {
                    type: pki.oids.signingTime,
                    value: asn1.dateToUtcTime(new Date())
                }
            ]
        });

        p7.sign({ detached: true }); // PKCS#7 sign in detached mode: includes the signature and certificate without the signed data
        const pem = pkcs7.messageToPem(p7);
        return removePemLabel(pem);
    } catch (error) {
        throw new SignError(error);
    }
}

function removePemLabel(pem: string): string {
    const firstNewLine = pem.indexOf("\r\n");
    return pem.substring(firstNewLine + "\r\n".length, pem.length - "\r\n".length * 2 - "-----END PKCS7-----".length);
}

export default { decrypt, encrypt, verify, sign };
