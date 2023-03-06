import { md, pkcs7, pki, asn1, util } from "node-forge";
import {
    InvalidMessageIntegrityError,
    InvalidPkcs7EnvelopeError,
    InvalidSignatureError,
    UnsupportedAlgorithmError
} from "../../lib/exceptions";

export function getSigningTime(envelope: pkcs7.Captured<pkcs7.PkcsSignedData>) {
    const parent = getParent(envelope.rawCapture.authenticatedAttributesAsn1, pki.oids["signingTime"]);
    if (!parent) {
        throw new InvalidPkcs7EnvelopeError();
    }
    return asn1.utcTimeToDate(<string>(<asn1.Asn1>(<asn1.Asn1>parent.value[1]).value[0]).value);
}

export function checkSignatureValidity(envelope: pkcs7.Captured<pkcs7.PkcsSignedData>, certificate: pki.Certificate) {
    const authAttributesDigest = digestAttributes(envelope.rawCapture.authenticatedAttributesAsn1, certificate);

    const pubKey = <pki.rsa.PublicKey>certificate.publicKey;
    if (!pubKey.verify(authAttributesDigest, envelope.rawCapture.signature, "RSASSA-PKCS1-V1_5")) {
        throw new InvalidSignatureError();
    }
}

export function checkMessageIntegrity(envelope: pkcs7.Captured<pkcs7.PkcsSignedData>, contentToDigest: string) {
    const algo = getHashFunction(asn1.derToOid(envelope.rawCapture.digestAlgorithm));
    const computedDigest = algo.create().update(contentToDigest).digest().bytes();
    const messageDigest = getMessageDigest(envelope.rawCapture.authenticatedAttributesAsn1);
    if (util.bytesToHex(<string>messageDigest) !== util.bytesToHex(computedDigest)) {
        if (contentToDigest.match(/(?<!\r)\n/) === null) {
            throw new InvalidMessageIntegrityError();
        }
        contentToDigest = contentToDigest.replace(/(?<!\r)\n/g, "\r\n");
        const newDigest = algo.create().update(contentToDigest).digest().bytes();
        if (util.bytesToHex(<string>messageDigest) !== util.bytesToHex(newDigest)) {
            throw new InvalidMessageIntegrityError();
        }
    }
}

function digestAttributes(attrs: pki.Attribute, certificate: pki.Certificate): string {
    // per RFC 2315, attributes are to be digested using a SET container
    // not the above [0] IMPLICIT container
    const attrsAsn1 = asn1.create(asn1.Class.UNIVERSAL, asn1.Type.SET, true, []);
    attrsAsn1.value = attrs.value;

    // DER-serialize and digest SET OF attributes only
    const bytes = asn1.toDer(attrsAsn1).getBytes();

    const algo = getDigestAttrAlgo(certificate.siginfo.algorithmOid);
    return algo.update(bytes).digest().getBytes();
}

function getDigestAttrAlgo(algorithmOid: string) {
    switch (algorithmOid) {
        case pki.oids.md5WithRSAEncryption:
            return md.md5.create();
        case pki.oids.sha1WithRSAEncryption:
            return md.sha1.create();
        case pki.oids.sha256WithRSAEncryption:
            return md.sha256.create();
        case pki.oids.sha384WithRSAEncryption:
            return md.sha384.create();
        case pki.oids.sha512WithRSAEncryption:
            return md.sha512.create();
        default:
            throw new UnsupportedAlgorithmError(pki.oids[algorithmOid]);
    }
}

function getHashFunction(algorithmOid: string) {
    switch (algorithmOid) {
        case pki.oids.md5:
            return md.md5;
        case pki.oids.sha1:
            return md.sha1;
        case pki.oids.sha256:
            return md.sha256;
        case pki.oids.sha384:
            return md.sha384;
        case pki.oids.sha512:
            return md.sha512;
        default:
            throw new UnsupportedAlgorithmError(pki.oids[algorithmOid]);
    }
}

function getMessageDigest(attribute: asn1.Asn1): string {
    const parent = getParent(attribute, pki.oids["messageDigest"]);
    if (!parent) {
        throw new InvalidPkcs7EnvelopeError();
    }
    return <string>(<asn1.Asn1>(<asn1.Asn1>parent.value[1]).value[0]).value;
}

function getParent(attribute: asn1.Asn1, searchedOid: string): asn1.Asn1 | undefined {
    if (isOid(attribute, searchedOid)) {
        return attribute; // no parent so return myself
    } else if (Array.isArray(attribute.value)) {
        let index = 0;
        do {
            if (getParent(attribute.value[index], searchedOid)) {
                return attribute.value[index];
            }
            index++;
        } while (index < attribute.value.length);
    }
}

function isOid(attr: asn1.Asn1, oid: string): boolean {
    return attr.type === asn1.Type.OID && asn1.derToOid(new util.ByteStringBuffer(<string>attr.value)) === oid;
}
