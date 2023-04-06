<template>
    <composer-alert class="sign-error-alert" :code="signError" :text="$t('alert.smime.sign_failed')" doc="">
        <bm-button class="stop-signature" variant="text" @click="stopSignature(message)">
            {{ $t("smime.mailapp.composer.stop_signature") }}
        </bm-button>
    </composer-alert>
</template>

<script>
import { BmButton } from "@bluemind/ui-components";
import ComposerAlert from "./ComposerAlert";
import EncryptSignMixin from "../../../mixins/EncryptSignMixin";

export default {
    name: "SignErrorAlert",
    components: { BmButton, ComposerAlert },
    mixins: [EncryptSignMixin],
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
        signError() {
            return this.$store.state.mail.smime.signError;
        }
    }
};
</script>
