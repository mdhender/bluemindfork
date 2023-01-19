import { CRYPTO_HEADERS, ENCRYPTED_HEADER_NAME, SIGNED_HEADER_NAME } from "../lib/constants";
import { removeHeader, addHeaderValue } from "../lib/helper";

export default {
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    methods: {
        stopSignature() {
            const headers = removeHeader(this.message.headers, SIGNED_HEADER_NAME);
            this.setHeadersAndSave(headers);
        },
        stopEncryption() {
            const headers = removeHeader(this.message.headers, ENCRYPTED_HEADER_NAME);
            this.setHeadersAndSave(headers);
        },
        startEncryption() {
            const headers = addHeaderValue(this.message.headers, ENCRYPTED_HEADER_NAME, CRYPTO_HEADERS.TO_DO);
            this.setHeadersAndSave(headers);
        },
        startSignature() {
            const headers = addHeaderValue(this.message.headers, SIGNED_HEADER_NAME, CRYPTO_HEADERS.TO_DO);
            this.setHeadersAndSave(headers);
        },
        async setHeadersAndSave(headers) {
            this.$store.commit("mail/SET_MESSAGE_HEADERS", { messageKey: this.message.key, headers });

            await this.$store.dispatch(`mail/DEBOUNCED_SAVE_MESSAGE`, {
                draft: this.message,
                messageCompose: this.$store.state.mail.messageCompose,
                files: this.message.attachments.map(({ fileKey }) => this.$store.state.mail.files[fileKey])
            });
        }
    }
};
