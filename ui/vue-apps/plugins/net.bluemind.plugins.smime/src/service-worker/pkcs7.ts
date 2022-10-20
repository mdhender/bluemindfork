import { pkcs7, pki, asn1, util } from "node-forge";

export async function decrypt(
    data: Blob,
    privateKey: pki.rsa.PrivateKey,
    certificate: pki.Certificate
): Promise<string | undefined> {
    const buffer = await data.arrayBuffer();
    const text = asn1.fromDer(new util.ByteStringBuffer(buffer));
    const envelope = <pkcs7.Captured<pkcs7.PkcsEnvelopedData>>pkcs7.messageFromAsn1(text);
    const recipient = envelope.findRecipient(certificate);
    if (recipient) {
        envelope.decrypt(recipient, privateKey);
        return envelope.content?.toString();
    }
}

export async function crypt() {
    return null;
}

export async function verify() {
    return null;
}

export async function sign() {
    return null;
}

export default { decrypt, crypt, verify, sign };
