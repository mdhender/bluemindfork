<template>
    <div class="encrypt-error-alert">
        <div class="text">
            <div>{{ text }}</div>
            <div v-if="!missingCertificates && encryptError">
                ({{ $t("common.error.code", { code: encryptError }) }})
            </div>
            <!-- TODO: doc link -->
            <bm-read-more href="" />
        </div>
        <bm-button class="stop-encryption" variant="text" @click="stopEncryption(ACTIVE_MESSAGE)">
            {{ $t("smime.mailapp.composer.stop_encryption") }}
        </bm-button>
    </div>
</template>

<script>
import { BmButton } from "@bluemind/ui-components";
import { BmReadMore } from "@bluemind/ui-components";
import { CRYPTO_HEADERS } from "../../lib/constants";
import { mapGetters } from "vuex";
import EncryptSignMixin from "../../mixins/EncryptSignMixin";

export default {
    name: "EncryptErrorAlert",
    components: { BmButton, BmReadMore },
    mixins: [EncryptSignMixin],
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
