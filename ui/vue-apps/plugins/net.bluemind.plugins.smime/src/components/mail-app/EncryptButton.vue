<template>
    <bm-icon-button
        :icon="isEncrypted ? 'lock' : 'lock-open'"
        class="encrypt-button"
        :class="{ encrypted: isEncrypted }"
        variant="compact"
        size="lg"
        :title="title"
        @click="action"
    />
</template>

<script>
import { BmIconButton } from "@bluemind/ui-components";
import { isEncrypted, addHeaderValue, removeHeader } from "../../lib/helper";
import { CRYPTO_HEADERS, ENCRYPTED_HEADER_NAME } from "../../lib/constants";
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
        title() {
            return this.isEncrypted
                ? this.$t("smime.mailapp.composer.unencrypt")
                : this.$t("smime.mailapp.composer.encrypt");
        },
        isEncrypted() {
            return isEncrypted(this.message.headers);
        }
    },
    methods: {
        async action() {
            let headers;
            if (this.isEncrypted) {
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
    &.encrypted {
        color: $primary-fg-hi1;
    }
}
</style>
