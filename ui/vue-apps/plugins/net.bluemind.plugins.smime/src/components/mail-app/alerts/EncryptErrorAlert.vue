<template>
    <composer-alert class="encrypt-error-alert" :code="encryptError" :text="text" :doc="docLink">
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
import ComposerAlert from "./ComposerAlert";

export default {
    name: "EncryptErrorAlert",
    components: { BmButton, ComposerAlert },
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
        },
        docLink() {
            const base =
                "https://doc.bluemind.net/release/5.0/guide_de_l_administrateur/resolution_de_problemes/resolution_des_problemes_smime";
            if (this.missingCertificates) {
                return base + "#missing-other-user-certificate";
            }
            switch (this.encryptError) {
                case CRYPTO_HEADERS.MY_INVALID_CERTIFICATE:
                    return base + "#my-invalid-certificate";
                case CRYPTO_HEADERS.CERTIFICATE_RECIPIENT_NOT_FOUND:
                    return base + "#missing-other-user-certificate";
                case CRYPTO_HEADERS.INVALID_CERTIFICATE_RECIPIENT:
                    return base + "#invalid-other-user-certificate";
                case CRYPTO_HEADERS.ENCRYPT_FAILURE:
                    return base + "#encrypt-failure";
            }
            return base;
        }
    }
};
</script>
