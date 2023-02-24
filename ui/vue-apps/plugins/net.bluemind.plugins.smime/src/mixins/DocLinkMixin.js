import { CRYPTO_HEADERS, ENCRYPTED_HEADER_NAME } from "../lib/constants";

const base =
    "https://doc.bluemind.net/release/5.0/guide_de_l_administrateur/resolution_de_problemes/resolution_des_problemes_smime";

export default {
    methods: {
        getEncryptErrorLink(errorCode, missingCertificates = false) {
            if (missingCertificates) {
                return base + "#missing-other-user-certificate";
            }
            const link = getErrorCode(errorCode);
            return link || base;
        },
        getBodyWrapperLink(errorCode, headerName) {
            const link = getErrorCode(errorCode);
            const fallback = headerName === ENCRYPTED_HEADER_NAME ? "#decrypt-failure" : "#verify-failure";
            return link || base + fallback;
        }
    }
};

function getErrorCode(errorCode) {
    console.log(errorCode);
    switch (errorCode) {
        case CRYPTO_HEADERS.MY_INVALID_CERTIFICATE:
            return base + "#my-invalid-certificate";
        case CRYPTO_HEADERS.CERTIFICATE_RECIPIENT_NOT_FOUND:
            return base + "#missing-other-user-certificate";
        case CRYPTO_HEADERS.INVALID_CERTIFICATE_RECIPIENT:
            return base + "#invalid-other-user-certificate";
        case CRYPTO_HEADERS.ENCRYPT_FAILURE:
            return base + "#encrypt-failure";
        case CRYPTO_HEADERS.DECRYPT_FAILURE:
            return base + "#decrypt-failure";
        case CRYPTO_HEADERS.UNMATCHED_CERTIFICATE:
            return base + "#decrypt-failure-unmatched-certificate";
        case CRYPTO_HEADERS.INVALID_MESSAGE_INTEGRITY:
            return base + "#verify-failure-corrupted-message";
        case CRYPTO_HEADERS.INVALID_SIGNATURE:
            return base + "#verify-failure-invalid-signature";
        case CRYPTO_HEADERS.INVALID_PKCS7_ENVELOPE:
            return base + "#verify-failure";
    }
}
