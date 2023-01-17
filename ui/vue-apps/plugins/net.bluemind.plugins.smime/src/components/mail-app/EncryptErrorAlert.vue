<template>
    <div class="encrypt-error-alert">
        <div class="text">
            <div>{{ text }}</div>
            <div v-if="!missingCertificates && encryptError">
                ({{ $t("common.error.code", { code: encryptError }) }})
            </div>
            <!-- TODO: doc link -->
            <bm-read-more-button href="" />
        </div>
        <bm-button class="stop-encryption" variant="text" @click="stopEncryption">
            {{ $t("smime.mailapp.composer.stop_encryption") }}
        </bm-button>
    </div>
</template>

<script>
import { BmButton } from "@bluemind/ui-components";
import { BmReadMoreButton } from "@bluemind/ui-components";
import { CRYPTO_HEADERS, ENCRYPTED_HEADER_NAME } from "../../lib/constants";
import { mapGetters } from "vuex";
import { removeHeader } from "../../lib/helper";

export default {
    name: "EncryptErrorAlert",
    components: { BmButton, BmReadMoreButton },
    props: {
        alert: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", { ACTIVE_MESSAGE: "ACTIVE_MESSAGE" }),
        missingCertificates() {
            return (
                this.$store.state.mail.smime.missingCertificates.length > 0 ||
                this.encryptError & CRYPTO_HEADERS.CERTIFICATE_RECIPIENT_NOT_FOUND
            );
        },
        recipientsCount() {
            return this.ACTIVE_MESSAGE.to.length + this.ACTIVE_MESSAGE.bcc.length + this.ACTIVE_MESSAGE.cc.length;
        },
        allInvalid() {
            return (
                this.missingCertificates &&
                this.$store.state.mail.smime.missingCertificates.length === this.recipientsCount
            );
        },
        encryptError() {
            return this.$store.state.mail.smime.encryptError;
        },
        text() {
            return !this.missingCertificates
                ? this.$t("smime.mailapp.alert.encrypt_failed")
                : this.allInvalid
                ? this.$t("smime.mailapp.alert.recipients.all_invalid")
                : this.$t("smime.mailapp.alert.recipients.some_invalid");
        }
    },
    methods: {
        async stopEncryption() {
            const headers = removeHeader(this.ACTIVE_MESSAGE.headers, ENCRYPTED_HEADER_NAME);
            this.$store.commit("mail/SET_MESSAGE_HEADERS", { messageKey: this.ACTIVE_MESSAGE.key, headers });

            await this.$store.dispatch(`mail/DEBOUNCED_SAVE_MESSAGE`, {
                draft: this.ACTIVE_MESSAGE,
                messageCompose: this.$store.state.mail.messageCompose,
                files: this.ACTIVE_MESSAGE.attachments.map(({ fileKey }) => this.$store.state.mail.files[fileKey])
            });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/type";

.encrypt-error-alert {
    line-height: $line-height-medium;
    .stop-encryption {
        gap: 0;
        padding: 0;
    }
    .text {
        display: flex;
        align-items: center;
        gap: $sp-4;
    }
}
</style>
