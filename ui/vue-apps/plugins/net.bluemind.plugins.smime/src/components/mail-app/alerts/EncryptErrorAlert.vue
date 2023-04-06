<template>
    <composer-alert class="encrypt-error-alert" :code="encryptError" :text="text" :doc="doc">
        <bm-button class="stop-encryption" variant="text" @click="stopEncryption(message)">
            {{ $t("smime.mailapp.composer.stop_encryption") }}
        </bm-button>
    </composer-alert>
</template>

<script>
import { BmButton } from "@bluemind/ui-components";
import { CRYPTO_HEADERS } from "../../../lib/constants";
import EncryptSignMixin from "../../../mixins/EncryptSignMixin";
import DocLinkMixin from "../../../mixins/DocLinkMixin";
import ComposerAlert from "./ComposerAlert";

export default {
    name: "EncryptErrorAlert",
    components: { BmButton, ComposerAlert },
    mixins: [DocLinkMixin, EncryptSignMixin],
    props: {
        alert: {
            type: Object,
            required: true
        }
    },
    computed: {
        message() {
            return this.alert.payload;
        },
        missingCertificates() {
            return (
                this.$store.state.mail.smime.missingCertificates.length > 0 ||
                this.encryptError & CRYPTO_HEADERS.CERTIFICATE_RECIPIENT_NOT_FOUND
            );
        },
        recipientsCount() {
            return this.message.to.length + this.message.bcc.length + this.message.cc.length;
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
                ? this.$t("alert.smime.encrypt_failed")
                : this.allInvalid
                ? this.$t("alert.smime.recipients.all_invalid")
                : this.$t("alert.smime.recipients.some_invalid");
        },
        doc() {
            return this.missingCertificates ? this.missingCertificateLink : this.linkFromCode(this.encryptError);
        }
    }
};
</script>
