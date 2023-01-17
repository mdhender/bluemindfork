<template>
    <div class="bm-read-more">
        <div v-if="text">
            {{ text }}
        </div>
        <bm-button variant="link" :href="href" target="_blank">
            <bm-icon icon="popup" class="mr-3" />{{ $t("common.read_more") }}
        </bm-button>
    </div>
</template>
<script>
import { BmButton, BmIcon } from "@bluemind/ui-components";

export default {
    name: "BmReadMore",
    components: { BmButton, BmIcon },
    props: {
        href: { type: String, required: true },
        text: { type: String, default: null }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.bm-read-more {
    color: $neutral-fg-lo1;
    display: flex;
    flex-wrap: wrap;
    gap: $sp-1 $sp-3;
}
</style>
