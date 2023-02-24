<template>
    <composer-alert
        class="invalid-identity-alert"
        :text="$t('smime.mailapp.alert.invalid_identity')"
        :doc="emailAddressesDontMatchLink"
    >
        <bm-button variant="text" @click="stopEncryptionAndSignature()">
            {{ $t("smime.mailapp.composer.stop_encryption_and_signature") }}
        </bm-button>
    </composer-alert>
</template>

<script>
import { mapGetters } from "vuex";
import { BmButton } from "@bluemind/ui-components";
import DocLinkMixin from "../../../mixins/DocLinkMixin";
import EncryptSignMixin from "../../../mixins/EncryptSignMixin";
import ComposerAlert from "./ComposerAlert";

export default {
    name: "InvalidIdentityAlert",
    components: { BmButton, ComposerAlert },
    mixins: [DocLinkMixin, EncryptSignMixin],
    props: {
        alert: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", { ACTIVE_MESSAGE: "ACTIVE_MESSAGE" })
    },
    methods: {
        stopEncryptionAndSignature() {
            this.stopSignature(this.ACTIVE_MESSAGE);
            this.stopEncryption(this.ACTIVE_MESSAGE);
        }
    }
};
</script>
