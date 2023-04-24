<template>
    <bm-icon
        v-if="hasSignatureHeader"
        :icon="isVerified ? 'check-stamp' : 'exclamation-sample'"
        :title="tooltip"
        class="is-sender-trusted mr-3"
    />
</template>

<script>
import { BmIcon } from "@bluemind/ui-components";
import { hasSignatureHeader, isVerified } from "../../lib/helper";

export default {
    name: "TrustedSender",
    components: { BmIcon },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        hasSignatureHeader() {
            return hasSignatureHeader(this.message.headers);
        },
        isVerified() {
            return isVerified(this.message.headers);
        },
        tooltip() {
            const suffix = this.isVerified ? "trusted_sender" : "untrusted_sender";
            return this.$t("smime.mailapp.viewer." + suffix);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables.scss";
.is-sender-trusted.fa-check-stamp {
    color: $primary-fg;
}
</style>
