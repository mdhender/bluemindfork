import { CRYPTO_HEADERS, ENCRYPTED_HEADER_NAME } from "../lib/constants";

const base =
    "https://doc.bluemind.net/release/5.0/guide_de_l_administrateur/resolution_de_problemes/resolution_des_problemes_smime";

export default {
    data() {
        return {
            emailAddressesDontMatchLink: base + "#email-addresses-dont-match",
            missingCertificateLink: base + "#missing-other-user-certificate",
            noSmimeOnPreviewLink: base + "#cant-decrypt-or-verify-in-preview"
        };
    },
    methods: {
        getEncryptErrorLink(errorCode) {
            const anchor = getDocAnchor(errorCode);
            return anchor ? base + anchor : base;
        },
        getBodyWrapperLink(errorCode, headerName) {
            const fallback = headerName === ENCRYPTED_HEADER_NAME ? "#decrypt-failure" : "#verify-failure";
            const anchor = getDocAnchor(errorCode);
            return base + anchor || base + fallback;
        }
    }
};

function getDocAnchor(errorCode) {
    switch (errorCode) {
        case CRYPTO_HEADERS.KEY_NOT_FOUND:
        case CRYPTO_HEADERS.MY_CERTIFICATE_NOT_FOUND:
            return "#my-key-or-certificate-is-missing";
        case CRYPTO_HEADERS.MY_INVALID_CERTIFICATE:
            return "#my-invalid-certificate";
        case CRYPTO_HEADERS.CERTIFICATE_RECIPIENT_NOT_FOUND:
            return "#missing-other-user-certificate";
        case CRYPTO_HEADERS.INVALID_CERTIFICATE_RECIPIENT:
            return "#invalid-other-user-certificate";
        case CRYPTO_HEADERS.ENCRYPT_FAILURE:
            return "#encrypt-failure";
        case CRYPTO_HEADERS.DECRYPT_FAILURE:
            return "#decrypt-failure";
        case CRYPTO_HEADERS.UNMATCHED_CERTIFICATE:
            return "#decrypt-failure-unmatched-certificate";
        case CRYPTO_HEADERS.INVALID_MESSAGE_INTEGRITY:
            return "#verify-failure-corrupted-message";
        case CRYPTO_HEADERS.INVALID_SIGNATURE:
            return "#verify-failure-invalid-signature";
        case CRYPTO_HEADERS.INVALID_PKCS7_ENVELOPE:
        case CRYPTO_HEADERS.UNSUPPORTED_ALGORITHM:
            return "#verify-failure";
        case CRYPTO_HEADERS.SIGN_FAILURE:
            return "#sign-failure";
    }
}
