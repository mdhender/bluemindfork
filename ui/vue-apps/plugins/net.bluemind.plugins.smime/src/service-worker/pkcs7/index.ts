import { pkcs7, pki, asn1, util } from "node-forge";
import {
    InvalidCertificateError,
    DecryptError,
    EncryptError,
    SignError,
    UnmatchedCertificateError
} from "../exceptions";
import { checkMessageIntegrity, checkSignatureValidity, getSignedDataEnvelope, getSigningTime } from "./verify";
import { checkCertificateValidity, getMyCertificate, getMyPrivateKey } from "../pki/";
import { binaryToArrayBuffer } from "@bluemind/arraybuffer";

export async function decrypt(
    data: Blob,
    privateKey: pki.rsa.PrivateKey,
    certificate: pki.Certificate
): Promise<string | undefined> {
    const buffer = await data.arrayBuffer();
    const text = asn1.fromDer(new util.ByteStringBuffer(buffer));
    const envelope = <pkcs7.Captured<pkcs7.PkcsEnvelopedData>>pkcs7.messageFromAsn1(text);
    let recipient;
    try {
        recipient = envelope.findRecipient(certificate);
    } catch (error) {
        throw new InvalidCertificateError(error);
    }
    if (recipient) {
        try {
            envelope.decrypt(recipient, privateKey);
            return envelope.content?.toString();
        } catch (error) {
            throw new DecryptError(error);
        }
    } else {
        throw new UnmatchedCertificateError();
    }
}

export function encrypt(content: string, certificates: pki.Certificate[]): Blob {
    try {
        const envelope = pkcs7.createEnvelopedData();
        certificates.forEach(certificate => envelope.addRecipient(certificate));
        envelope.content = util.createBuffer(content);
        envelope.encrypt();

        const asn1Content = envelope.toAsn1();
        const bytes = asn1.toDer(asn1Content).getBytes();
        const buffer = binaryToArrayBuffer(bytes);
        return new Blob([buffer]);
    } catch (error) {
        throw new EncryptError(error);
    }
}

export async function verify(pkcs7: ArrayBuffer, toDigest: string) {
    const envelope = getSignedDataEnvelope(pkcs7);
    const signingTime = getSigningTime(envelope);
    const certificate = envelope.certificates[0];
    checkCertificateValidity(certificate, signingTime);
    checkSignatureValidity(envelope, certificate);
    checkMessageIntegrity(envelope, toDigest);
}

// create PKCS#7 signed data with authenticatedAttributes
// attributes include: PKCS#9 content-type, message-digest, and signing-time
export async function sign(content: string) {
    try {
        const p7 = pkcs7.createSignedData();
        p7.content = util.createBuffer(content);

        const privateKey = await getMyPrivateKey();
        const myCert = await getMyCertificate();

        // FIXME: add CA cert ?
        p7.addCertificate(myCert);
        p7.addSigner({
            key: privateKey,
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
        const bytes = asn1.toDer(p7.toAsn1()).getBytes();
        const buffer = binaryToArrayBuffer(bytes);
        return new Blob([buffer]);
    } catch (error) {
        throw new SignError(error);
    }
}

export default { decrypt, encrypt, verify, sign };
