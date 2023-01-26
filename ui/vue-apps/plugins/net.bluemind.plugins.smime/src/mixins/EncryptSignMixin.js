import { CRYPTO_HEADERS, ENCRYPTED_HEADER_NAME, SIGNED_HEADER_NAME } from "../lib/constants";
import { removeHeader, addHeaderValue } from "../lib/helper";

export default {
    methods: {
        stopSignature(message) {
            const headers = removeHeader(message.headers, SIGNED_HEADER_NAME);
            this.setHeadersAndSave(message, headers);
        },
        stopEncryption(message) {
            const headers = removeHeader(message.headers, ENCRYPTED_HEADER_NAME);
            this.setHeadersAndSave(message, headers);
        },
        startEncryption(message) {
            const headers = addHeaderValue(message.headers, ENCRYPTED_HEADER_NAME, CRYPTO_HEADERS.TO_DO);
            this.setHeadersAndSave(message, headers);
        },
        startSignature(message) {
            const headers = addHeaderValue(message.headers, SIGNED_HEADER_NAME, CRYPTO_HEADERS.TO_DO);
            this.setHeadersAndSave(message, headers);
        },
        async setHeadersAndSave(message, headers) {
            this.$store.commit("mail/SET_MESSAGE_HEADERS", { messageKey: message.key, headers });

            await this.$store.dispatch(`mail/DEBOUNCED_SAVE_MESSAGE`, {
                draft: message,
                messageCompose: this.$store.state.mail.messageCompose,
                files: message.attachments.map(({ fileKey }) => this.$store.state.mail.files[fileKey])
            });
        }
    }
};
