<template>
    <div class="untrusted-sender-alert">
        {{ $t("alert.smime.untrusted") }}
        <bm-button v-if="!isAlreadyDisplayed" variant="link" class="ml-4" @click="displayMessage">
            {{ $t("common.display_anyway") }}
        </bm-button>
    </div>
</template>

<script>
import { DISPLAY_UNTRUSTED } from "../../../store/mutationTypes";
import { BmButton } from "@bluemind/ui-components";

export default {
    name: "UntrustedSenderAlert",
    components: { BmButton },
    props: {
        alert: {
            type: Object,
            required: true
        }
    },
    computed: {
        messageKey() {
            return this.alert.payload;
        },
        isAlreadyDisplayed() {
            return this.$store.state.mail.smime.displayUntrusted.indexOf(this.messageKey) !== -1;
        }
    },
    methods: {
        displayMessage() {
            this.$store.commit("mail/" + DISPLAY_UNTRUSTED, this.messageKey);
        }
    }
};
</script>

<style lang="scss">
.untrusted-sender-alert {
    display: flex;
    align-items: center;
}
</style>
