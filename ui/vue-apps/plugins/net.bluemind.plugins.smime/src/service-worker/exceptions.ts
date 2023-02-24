import { CRYPTO_HEADERS } from "../lib/constants";

export abstract class SmimeErrors extends Error {
    code: number;
    constructor(message: string, code: number, error?: unknown) {
        let fullMessage = message;
        if (error instanceof Error) {
            fullMessage = `${message}: ${error.message}`;
        } else if (typeof error === "string") {
            fullMessage = `${message} ${error}`;
        }
        super(fullMessage);
        this.code = code;
    }
}

export class InvalidKeyError extends SmimeErrors {
    constructor(error?: unknown) {
        super("Invalid private key", CRYPTO_HEADERS.INVALID_KEY, error);
    }
}
export class MyInvalidCertificateError extends SmimeErrors {
    constructor(error?: unknown) {
        super("My certificate is invalid", CRYPTO_HEADERS.MY_INVALID_CERTIFICATE, error);
    }
}
export class KeyNotFoundError extends SmimeErrors {
    constructor(error?: unknown) {
        super("Private key not found", CRYPTO_HEADERS.KEY_NOT_FOUND, error);
    }
}
export class MyCertificateNotFoundError extends SmimeErrors {
    constructor(error?: unknown) {
        super("Cant find my certificate", CRYPTO_HEADERS.MY_CERTIFICATE_NOT_FOUND, error);
    }
}

export class InvalidMessageIntegrityError extends SmimeErrors {
    constructor(error?: unknown) {
        super("Message has been corrupted after sender signed it", CRYPTO_HEADERS.INVALID_MESSAGE_INTEGRITY, error);
    }
}

export class InvalidSignatureError extends SmimeErrors {
    constructor(error?: unknown) {
        super("Invalid signature compared to authenticate attributes", CRYPTO_HEADERS.INVALID_SIGNATURE, error);
    }
}

export class InvalidPkcs7EnvelopeError extends SmimeErrors {
    constructor(error?: unknown) {
        super("Invalid pkcs7 envelope", CRYPTO_HEADERS.INVALID_PKCS7_ENVELOPE, error);
    }
}

export class RevokedCertificateError extends SmimeErrors {
    constructor(error?: unknown) {
        super("Revoked certificate", CRYPTO_HEADERS.REVOKED_CERTIFICATE, error);
    }
}
export class ExpiredCertificateError extends SmimeErrors {
    constructor(error?: unknown) {
        super("Expired certificate ", CRYPTO_HEADERS.EXPIRED_CERTIFICATE, error);
    }
}

export class UnsupportedAlgorithmError extends SmimeErrors {
    constructor(algorithm: string) {
        super("Algorithm " + algorithm + " is not supported", CRYPTO_HEADERS.UNSUPPORTED_ALGORITHM);
    }
}

export class UntrustedCertificateError extends SmimeErrors {
    constructor(error?: unknown) {
        super("Untrusted certificate", CRYPTO_HEADERS.UNTRUSTED_CERTIFICATE, error);
    }
}
export class UnmatchedCertificateError extends SmimeErrors {
    constructor(error?: unknown) {
        super("The certificate does not match any of the recipients", CRYPTO_HEADERS.UNMATCHED_CERTIFICATE, error);
    }
}
export class DecryptError extends SmimeErrors {
    constructor(error?: unknown) {
        super("An error occured on decryption", CRYPTO_HEADERS.DECRYPT_FAILURE, error);
    }
}
export class EncryptError extends SmimeErrors {
    constructor(error?: unknown) {
        super("An error occured on encryption", CRYPTO_HEADERS.ENCRYPT_FAILURE, error);
    }
}

export class CertificateRecipientNotFoundError extends SmimeErrors {
    constructor(error?: unknown) {
        super("Certificate not found", CRYPTO_HEADERS.CERTIFICATE_RECIPIENT_NOT_FOUND, error);
    }
}

export class InvalidCertificateRecipientError extends SmimeErrors {
    constructor(error?: unknown) {
        super("Invalid certificate", CRYPTO_HEADERS.INVALID_CERTIFICATE_RECIPIENT, error);
    }
}
export class SignError extends SmimeErrors {
    constructor(error?: unknown) {
        super("An error occured when signing", CRYPTO_HEADERS.SIGN_FAILURE, error);
    }
}
export class InvalidOriginatorError extends SmimeErrors {
    constructor(error?: unknown) {
        super("The originator has to be the user default address", CRYPTO_HEADERS.INVALID_ORIGINATOR, error);
    }
}
