<template>
    <bm-icon v-if="isVerified" icon="check-stamp" class="trusted-sender" />
</template>

<script>
import { BmIcon } from "@bluemind/ui-components";
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
        isVerified() {
            // FIXME after merge with feat/decrypt
            const cryptoHeader = this.message.headers.find(header => header.name === "X-BM-Signed");
            return cryptoHeader?.values.find(value => value === "VERIFIED");
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.trusted-sender.fa-check-stamp {
    color: $primary-fg;
}
</style>
