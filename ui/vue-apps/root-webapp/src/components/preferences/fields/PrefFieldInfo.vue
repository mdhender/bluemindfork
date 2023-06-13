<template>
    <div class="pref-field-info d-flex">
        <bm-icon icon="info-circle" class="mr-4" />
        <p class="d-flex flex-column">
            <template v-for="(line, index) in lines"> {{ line }}<br :key="index" /> </template>
            <bm-read-more v-if="readMoreLink" :href="readMoreLink" />
        </p>
    </div>
</template>

<script>
import { BmReadMore, BmIcon } from "@bluemind/ui-components";

export default {
    name: "PrefFieldInfo",
    components: { BmIcon, BmReadMore },
    props: {
        lines: { type: Array, required: true },
        readMoreLink: { type: String, default: undefined }
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-field-info {
    color: $neutral-fg-lo1;
    margin-bottom: $sp-5 + $sp-3 !important;
    p {
        line-height: base-px-to-rem(16);
        margin-bottom: 0;
    }
}
</style>
