import { pkcs7, asn1, util } from "node-forge";

export function getSignedDataEnvelope(pkcs7Buffer: ArrayBuffer): pkcs7.Captured<pkcs7.PkcsSignedData> {
    // authAttributes capture at asn1 format is inspired from https://github.com/digitalbazaar/forge/issues/305
    const signerValidator = pkcs7.asn1.signedDataValidator.value[5]; // SignedData.SignerInfos
    const attrsValidator = signerValidator.value[0].value[3]; // SignerInfo.authenticatedAttributes
    attrsValidator.captureAsn1 = "authenticatedAttributesAsn1";

    const asn1Struct = asn1.fromDer(new util.ByteStringBuffer(pkcs7Buffer));
    return <pkcs7.Captured<pkcs7.PkcsSignedData>>pkcs7.messageFromAsn1(asn1Struct);
}