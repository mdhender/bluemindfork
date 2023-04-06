<template>
    <composer-alert
        class="invalid-identity-alert"
        :text="$t('alert.smime.invalid_identity')"
        :doc="emailAddressesDontMatchLink"
    >
        <bm-button variant="text" @click="stopEncryptionAndSignature()">
            {{ $t("smime.mailapp.composer.stop_encryption_and_signature") }}
        </bm-button>
    </composer-alert>
</template>

<script>
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
        message() {
            return this.alert.payload;
        }
    },
    methods: {
        stopEncryptionAndSignature() {
            this.stopSignature(this.message);
            this.stopEncryption(this.message);
        }
    }
};
</script>
