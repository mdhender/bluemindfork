<template>
    <bm-icon
        v-if="hasSignatureHeader"
        :icon="isVerified ? 'stamp' : 'stamp-exclamation'"
        :title="tooltip"
        :variant="isVerified ? 'primary' : 'warning'"
        class="mr-3"
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
