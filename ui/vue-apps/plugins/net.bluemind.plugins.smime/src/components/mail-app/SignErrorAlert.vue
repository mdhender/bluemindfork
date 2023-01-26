<template>
    <div class="sign-error-alert">
        <div class="text">
            <div>{{ text }}</div>
            <div v-if="signError">({{ $t("common.error.code", { code: signError }) }})</div>
            <!-- TODO: doc link -->
            <bm-read-more href="" />
        </div>
        <bm-button class="stop-signature" variant="text" @click="stopSignature(ACTIVE_MESSAGE)">
            {{ $t("smime.mailapp.composer.stop_signature") }}
        </bm-button>
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import { BmButton, BmReadMore } from "@bluemind/ui-components";
import EncryptSignMixin from "../../mixins/EncryptSignMixin";

export default {
    name: "SignErrorAlert",
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
        signError() {
            return this.$store.state.mail.smime.signError;
        },
        text() {
            return this.$t("smime.mailapp.alert.sign_failed");
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/type";

.sign-error-alert {
    line-height: $line-height-medium;
    .stop-signature {
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
