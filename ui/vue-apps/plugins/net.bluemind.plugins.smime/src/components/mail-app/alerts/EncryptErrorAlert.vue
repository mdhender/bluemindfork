<template>
    <composer-alert class="encrypt-error-alert" :code="encryptError" :text="text" :doc="doc">
        <bm-button class="stop-encryption" variant="text" @click="stopEncryption(ACTIVE_MESSAGE)">
            {{ $t("smime.mailapp.composer.stop_encryption") }}
        </bm-button>
    </composer-alert>
</template>

<script>
import { BmButton } from "@bluemind/ui-components";
import { CRYPTO_HEADERS } from "../../../lib/constants";
import { mapGetters } from "vuex";
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
        },
        doc() {
            return this.missingCertificates ? this.missingCertificateLink : this.linkFromCode(this.encryptError);
        }
    }
};
</script>
