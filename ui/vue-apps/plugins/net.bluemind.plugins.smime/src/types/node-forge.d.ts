declare module "node-forge" {
    namespace pkcs7 {
        interface PkcsEnvelopedData {
            // content?: string | util.ByteBuffer | undefined;
            // addRecipient(certificate: pki.Certificate): void;
            // encrypt(): void;
            // toAsn1(): asn1.Asn1;
            findRecipient(cert: pki.Certificate): RecipientInfo | null;
            decrypt(recipient: RecipientInfo, privKey: pki.PrivateKey): void;
        }
        type RecipientInfo = {
            version: Version;
            issuerAndSerialNumber: IssuerAndSerialNumber;
            keyEncryptionAlgorithm: KeyEncryptionAlgorithmIdentifier;
            encryptedKey: EncryptedKey;
        };

        type IssuerAndSerialNumber = {
            issuer: Name;
            serialNumber: CertificateSerialNumber;
        };

        type CertificateSerialNumber = number;

        type KeyEncryptionAlgorithmIdentifier = AlgorithmIdentifier;
        type Version = number;
        type AlgorithmIdentifer = {
            algorithm: OID;
            parameters?: unknown;
        };
    }
}
