<template>
    <composer-alert class="sign-error-alert" :code="signError" :text="$t('alert.smime.sign_failed')" doc="">
        <bm-button class="stop-signature" variant="text" @click="stopSignature(ACTIVE_MESSAGE)">
            {{ $t("smime.mailapp.composer.stop_signature") }}
        </bm-button>
    </composer-alert>
</template>

<script>
import { mapGetters } from "vuex";
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
        ...mapGetters("mail", { ACTIVE_MESSAGE: "ACTIVE_MESSAGE" }),
        signError() {
            return this.$store.state.mail.smime.signError;
        }
    }
};
</script>
