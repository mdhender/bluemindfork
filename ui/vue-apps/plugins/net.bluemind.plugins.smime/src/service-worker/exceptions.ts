import { CRYPTO_HEADERS } from "../lib/constants";

export abstract class SmimeErrors extends Error {
    name: string
    constructor(name: string, message: string) {
        super(message)
        this.name = name;
    }
}
class InvalidCredentialsError extends SmimeErrors {
    constructor() {
        super("Invalid certificate or private key", CRYPTO_HEADERS.INVALID_CREDENTIALS)
    }
}
class RevokedCrendentialsError extends SmimeErrors {
    constructor() {
        super("Revoked certificate or private key", CRYPTO_HEADERS.REVOKED_CREDENTIALS);
    }
}
class ExpiredCredentialsError extends SmimeErrors {
    constructor() {
        super("Expired certificate or private key", CRYPTO_HEADERS.EXPIRED_CREDENTIALS)

    }
}
class UntrustedCredentialsError extends SmimeErrors {
    constructor() {
        super("Untrusted certificate or private key", CRYPTO_HEADERS.UNTRUSTED_CREDENTIALS)

    }
}
class UnmatchedRecipientError extends SmimeErrors {
    constructor() {
        super("The certificate does not match any of the recipients", CRYPTO_HEADERS.UNMATCHED_RECIPIENTS)
    }
}

export default {
    ExpiredCredentialsError,
    InvalidCredentialsError,
    RevokedCrendentialsError,
    UnmatchedRecipientError,
    UntrustedCredentialsError,
}
