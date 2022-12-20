<template>
    <bm-icon v-if="isVerified(message.headers)" icon="check-stamp" class="trusted-sender" />
</template>

<script>
import { BmIcon } from "@bluemind/ui-components";
import { isVerified } from "../../lib/helper";

export default {
    name: "TrustedSender",
    components: { BmIcon },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return { isVerified };
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables.scss";
.trusted-sender.fa-check-stamp {
    color: $primary-fg;
}
</style>
