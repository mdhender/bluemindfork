declare module "node-forge" {
    namespace asn1 {
        function dateToUtcTime(date: Date): string;
        function utcTimeToDate(utcTime: string): Date;
        function prettyPrint(asn1: asn1.Asn1, level?: number, indentation?: number): void;
    }
    namespace pkcs7 {
        const asn1: Asn1;
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
            algorithm: asn1.OID;
            parameters?: unknown;
        };
    }
}
