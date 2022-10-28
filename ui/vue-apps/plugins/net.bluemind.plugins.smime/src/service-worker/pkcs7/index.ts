import { pkcs7, pki, asn1, util } from "node-forge";
import { RecipientNotFoundError, InvalidCredentialsError } from "../exceptions";
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
    try {
        const recipient = envelope.findRecipient(certificate);
        if (recipient) {
            envelope.decrypt(recipient, privateKey);
            return envelope.content?.toString();
        } else {
            throw new RecipientNotFoundError();
        }
    } catch (error) {
        throw new InvalidCredentialsError(error);
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
