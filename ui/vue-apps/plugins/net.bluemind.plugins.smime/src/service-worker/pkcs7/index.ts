import { pkcs7, pki, asn1, util } from "node-forge";
import { RecipientNotFoundError, InvalidCertificateError,  DecryptError } from "../exceptions";
import { checkMessageIntegrity, checkSignatureValidity, getSignedDataEnvelope, getSigningTime } from "./verify";
import { checkCertificateValidity, getCertificate } from "../pki";

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
        throw new RecipientNotFoundError();
    }
}

export async function crypt() {
    return null;
}

export async function verify(pkcs7: ArrayBuffer, toDigest: string, senderAddress: string) {
    const envelope = getSignedDataEnvelope(pkcs7);
    const signingTime = getSigningTime(envelope);
    let certificate = await getCertificate(senderAddress);
    if (!certificate) {
        certificate = envelope.certificates[0];
    }
    checkCertificateValidity(certificate, signingTime);
    checkSignatureValidity(envelope, certificate);
    checkMessageIntegrity(envelope, toDigest);
}

export async function sign() {
    return null;
}

export default { decrypt, crypt, verify, sign };
