import { CRYPTO_HEADERS, ENCRYPTED_HEADER_NAME, SIGNED_HEADER_NAME } from "../lib/constants";

const base =
    "https://doc.bluemind.net/release/5.0/guide_de_l_administrateur/resolution_de_problemes/resolution_des_problemes_smime";

export default {
    data() {
        return {
            emailAddressesDontMatchLink: base + "#email-addresses-dont-match",
            incompatibleBrowserLink: base + "#incompatible-browser",
            missingCertificateLink: base + "#missing-other-user-certificate",
            noSmimeOnPreviewLink: base + "#cant-decrypt-or-verify-in-preview"
        };
    },
    methods: {
        linkFromCode(errorCode: CRYPTO_HEADERS) {
            const anchor = getDocAnchor(errorCode);
            return anchor ? base + anchor : base;
        },
        linkFromCodeOrHeader(errorCode: CRYPTO_HEADERS, headerName: string) {
            const fallback = anchorFromHeaderName(headerName);
            const anchor = getDocAnchor(errorCode);
            return anchor ? base + anchor : base + fallback;
        }
    }
};

function anchorFromHeaderName(headerName: string) {
    switch (headerName) {
        case ENCRYPTED_HEADER_NAME:
            return "#decrypt-failure";
        case SIGNED_HEADER_NAME:
            return "#verify-failure";
    }
    return "";
}

function getDocAnchor(errorCode: CRYPTO_HEADERS) {
    switch (errorCode) {
        case errorCode & CRYPTO_HEADERS.KEY_NOT_FOUND:
        case errorCode & CRYPTO_HEADERS.MY_CERTIFICATE_NOT_FOUND:
            return "#my-key-or-certificate-is-missing";
        case errorCode & CRYPTO_HEADERS.UNTRUSTED_CERTIFICATE:
            return "#untrusted-certificate";
        case errorCode & CRYPTO_HEADERS.UNTRUSTED_CERTIFICATE_EMAIL_NOT_FOUND:
            return "#untrusted-certificate-email-not-found";
        case errorCode & CRYPTO_HEADERS.INVALID_CERTIFICATE:
            return "#invalid-certificate";
        case errorCode & CRYPTO_HEADERS.CERTIFICATE_RECIPIENT_NOT_FOUND:
            return "#missing-other-user-certificate";
        case errorCode & CRYPTO_HEADERS.UNMATCHED_CERTIFICATE:
            return "#decrypt-failure-unmatched-certificate";
        case errorCode & CRYPTO_HEADERS.INVALID_MESSAGE_INTEGRITY:
            return "#verify-failure-corrupted-message";
        case errorCode & CRYPTO_HEADERS.INVALID_SIGNATURE:
            return "#verify-failure-invalid-signature";
        case errorCode & CRYPTO_HEADERS.ENCRYPT_FAILURE:
            return "#encrypt-failure";
        case errorCode & CRYPTO_HEADERS.DECRYPT_FAILURE:
            return "#decrypt-failure";
        case errorCode & CRYPTO_HEADERS.INVALID_PKCS7_ENVELOPE:
        case errorCode & CRYPTO_HEADERS.UNSUPPORTED_ALGORITHM:
            return "#verify-failure";
        case errorCode & CRYPTO_HEADERS.SIGN_FAILURE:
            return "#sign-failure";
    }
}
