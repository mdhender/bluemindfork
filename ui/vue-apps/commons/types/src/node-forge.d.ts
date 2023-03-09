declare module "node-forge" {
    namespace asn1 {
        function dateToUtcTime(date: Date): string;
        function utcTimeToDate(utcTime: string): Date;
        function prettyPrint(asn1: asn1.Asn1, level?: number, indentation?: number): void;
    }
    namespace pkcs7 {
        const asn1: Asn1;
        interface PkcsEnvelopedData {
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

        function messageToPem(msg: PkcsSignedData | PkcsEnvelopedData, maxline?: number): string;
    }
    namespace pki {
        export type ForgePkiCertificateError = { message: string; error: string };

        // x509v3 extensions
        const anyExtendedKeyUsageOid = "2.5.29.37.0";
        export type ExtendedKeyUsageExtension = { [anyExtendedKeyUsageOid]: boolean; emailProtection: boolean };

        export type BasicConstraintsExtension = { cA: boolean };
        export type SubjectAltNameExtension = { altNames: { type: number; value: string }[] };
        export type KeyUsageExtension = {
            digitalSignature: boolean;
            keyEncipherment: boolean;
            nonRepudiation: boolean;
        };

        interface Certificate {
            getExtension(
                options: string | { name: string } | { id: number }
            ): BasicConstraintsExtension | ExtendedKeyUsageExtension | SubjectAltNameExtension | undefined;
        }
    }
}
