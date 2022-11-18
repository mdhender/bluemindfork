<template>
    <bm-icon v-if="isVerified" icon="check-stamp" class="trusted-sender" />
</template>

<script>
import { BmIcon } from "@bluemind/ui-components";
import { CRYPTO_HEADERS, SIGNED_HEADER_NAME } from "../../lib/constants";

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
            const cryptoHeader = this.message.headers.find(header => header.name === SIGNED_HEADER_NAME);
            return cryptoHeader?.values.find(value => value === CRYPTO_HEADERS.VERIFIED);
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
