<template>
    <bm-icon-button
        :icon="!hasEncryptionHeader ? 'lock-open' : hasEncryptError ? 'lock-slash' : 'lock'"
        class="encrypt-button"
        :class="{ selected: hasEncryptionHeader, error: hasEncryptError }"
        variant="compact"
        size="lg"
        :title="title"
        @click="action"
    />
</template>

<script>
import { mapGetters } from "vuex";
import { BmIconButton } from "@bluemind/ui-components";
import { draftUtils, messageUtils } from "@bluemind/mail";
import { SMIME_AVAILABLE } from "../../store/getterTypes";
import { hasEncryptionHeader, addHeaderValue, removeHeader } from "../../lib/helper";
import { CRYPTO_HEADERS, ENCRYPTED_HEADER_NAME, SIGNED_HEADER_NAME, SMIMEPrefKeys } from "../../lib/constants";

const { MessageCreationModes, messageKey } = messageUtils;
const { isNewMessage } = draftUtils;

export default {
    name: "EncryptButton",
    components: { BmIconButton },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", { SMIME_AVAILABLE }),
        title() {
            return this.hasEncryptionHeader
                ? this.$t("smime.mailapp.composer.no_encrypt")
                : this.$t("smime.mailapp.composer.encrypt");
        },
        hasEncryptionHeader() {
            return hasEncryptionHeader(this.message.headers);
        },
        hasEncryptError() {
            return (
                !this.SMIME_AVAILABLE ||
                this.$store.state.mail.smime.cannotEncryptEmails.length > 0 ||
                this.$store.state.mail.smime.encryptError
            );
        }
    },
    watch: {
        "$route.query.action": {
            handler(action) {
                if (this.SMIME_AVAILABLE) {
                    if (this.$store.state.settings[SMIMEPrefKeys.SIGNATURE] === "true") {
                        const headers = addHeaderValue(this.message.headers, SIGNED_HEADER_NAME, CRYPTO_HEADERS.TO_DO);
                        this.$store.commit("mail/SET_MESSAGE_HEADERS", { messageKey: this.message.key, headers });
                    }
                    if (this.$store.state.settings[SMIMEPrefKeys.ENCRYPTION] === "true") {
                        const headers = addHeaderValue(
                            this.message.headers,
                            ENCRYPTED_HEADER_NAME,
                            CRYPTO_HEADERS.TO_DO
                        );
                        this.$store.commit("mail/SET_MESSAGE_HEADERS", { messageKey: this.message.key, headers });
                    } else if (!isNewMessage(this.message) && this.hasEncryptionHeader) {
                        const headers = addHeaderValue(
                            this.message.headers,
                            ENCRYPTED_HEADER_NAME,
                            CRYPTO_HEADERS.TO_DO
                        );
                        this.$store.commit("mail/SET_MESSAGE_HEADERS", { messageKey: this.message.key, headers });
                    } else if (action && action !== MessageCreationModes.FORWARD_AS_EML) {
                        const key = messageKey(...this.$route.query.message.split(":").reverse());
                        const previous = this.$store.state.mail.conversations.messages[key];

                        if (previous && hasEncryptionHeader(previous.headers)) {
                            const headers = addHeaderValue(
                                this.message.headers,
                                ENCRYPTED_HEADER_NAME,
                                CRYPTO_HEADERS.TO_DO
                            );
                            this.$store.commit("mail/SET_MESSAGE_HEADERS", { messageKey: this.message.key, headers });
                        }
                    }
                }
            },
            immediate: true
        }
    },
    methods: {
        async action() {
            let headers;
            if (this.hasEncryptionHeader) {
                headers = removeHeader(this.message.headers, ENCRYPTED_HEADER_NAME);
            } else {
                headers = addHeaderValue(this.message.headers, ENCRYPTED_HEADER_NAME, CRYPTO_HEADERS.TO_DO);
            }
            this.$store.commit("mail/SET_MESSAGE_HEADERS", { messageKey: this.message.key, headers });

            await this.$store.dispatch(`mail/DEBOUNCED_SAVE_MESSAGE`, {
                draft: this.message,
                messageCompose: this.$store.state.mail.messageCompose,
                files: this.message.attachments.map(({ fileKey }) => this.$store.state.mail.files[fileKey])
            });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables.scss";

.encrypt-button {
    &.selected:not(.error) {
        color: $primary-fg-hi1;
    }
    &.error.selected {
        color: $danger-fg-hi1;
    }
}
</style>
