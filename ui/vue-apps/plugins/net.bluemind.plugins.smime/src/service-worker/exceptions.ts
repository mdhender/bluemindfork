import { CRYPTO_HEADERS } from "../lib/constants";

export abstract class SmimeErrors extends Error {
    name: string;
    constructor(message: string, name: string, error?: unknown) {
        const fullMessage = error && error instanceof Error ? `${message}: ${error.message}` : message;
        super(fullMessage);
        this.name = name;
    }
}
export class InvalidCredentialsError extends SmimeErrors {
    constructor(error?: unknown) {
        super("Invalid private key or certificate", CRYPTO_HEADERS.INVALID_CREDENTIALS, error);
    }
}
export class InvalidKeyError extends SmimeErrors {
    constructor(error?: unknown) {
        super("Invalid private key", CRYPTO_HEADERS.INVALID_CREDENTIALS, error);
    }
}
export class InvalidCertificateError extends SmimeErrors {
    constructor(error?: unknown) {
        super("Invalid certificate", CRYPTO_HEADERS.INVALID_CREDENTIALS, error);
    }
}

export class RevokedCrendentialsError extends SmimeErrors {
    constructor(error?: unknown) {
        super("Revoked certificate or private key", CRYPTO_HEADERS.REVOKED_CREDENTIALS, error);
    }
}
export class ExpiredCredentialsError extends SmimeErrors {
    constructor(error?: unknown) {
        super("Expired certificate or private key", CRYPTO_HEADERS.EXPIRED_CREDENTIALS, error);
    }
}
export class UntrustedCredentialsError extends SmimeErrors {
    constructor(error?: unknown) {
        super("Untrusted certificate or private key", CRYPTO_HEADERS.UNTRUSTED_CREDENTIALS, error);
    }
}
export class RecipientNotFoundError extends SmimeErrors {
    constructor(error?: unknown) {
        super("The certificate does not match any of the recipients", CRYPTO_HEADERS.UNMATCHED_RECIPIENTS, error);
    }
}
