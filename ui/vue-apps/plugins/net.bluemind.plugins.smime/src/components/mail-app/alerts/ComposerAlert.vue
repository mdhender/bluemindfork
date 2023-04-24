<template>
    <div class="composer-alert">
        <div class="text high">
            <div>{{ text }}</div>
            <div v-if="code">({{ $t("common.error.code", { code }) }})</div>
            <bm-read-more :href="doc" />
        </div>
        <slot />
    </div>
</template>

<script>
import { BmReadMore } from "@bluemind/ui-components";

export default {
    name: "ComposerAlert",
    components: { BmReadMore },
    props: {
        code: {
            type: Number,
            default: null
        },
        doc: {
            type: String,
            default: null
        },
        text: {
            type: String,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/type";

.composer-alert {
    line-height: $line-height-medium;
    .text {
        display: flex;
        align-items: center;
        gap: $sp-4;
    }
}
</style>
