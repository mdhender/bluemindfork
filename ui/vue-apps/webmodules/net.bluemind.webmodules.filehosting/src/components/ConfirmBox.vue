<template>
    <div class="fh-confirm-box" visible title-class="ml-2">
        <div class="mr-4">
            <div class="d-flex align-items-center mb-3">
                <bm-icon icon="file" size="xl" class="mr-2 text-secondary" />
                <span class="text-secondary mr-3 dot-font-size">&#8226;</span>
                <span class="text-secondary dot-font-size">&#8226;</span>
                <bm-icon icon="interrogation" size="xl" class="text-secondary" />
                <span class="text-neutral-fg-lo2 mr-3 dot-font-size">&#8226;</span>
                <span class="text-neutral-fg-lo2 mr-3 dot-font-size">&#8226;</span>
                <bm-icon icon="chevron-right" size="lg" class="text-neutral-fg-lo2" />
                <bm-icon icon="cloud" class="ml-2 text-secondary" size="xl" />
            </div>
            <div class="mb-4">
                <slot name="text"> </slot>
            </div>
            <div v-for="(file, idx) in files" :key="idx" class="position-relative mb-5">
                <detachment-item :file="file" />
            </div>
        </div>
    </div>
</template>
<script>
import { BmIcon } from "@bluemind/styleguide";
import DetachmentItem from "./DetachmentItem";

export default {
    name: "FhConfirmBox",
    components: { BmIcon, DetachmentItem },
    props: {
        files: {
            type: Array,
            required: true
        }
    }
};
</script>
<style lang="scss" scoped>
@import "@bluemind/styleguide/css/_variables.scss";

.fh-confirm-box {
    .text-neutral-fg-lo2 {
        color: $neutral-fg-lo2;
    }
    .progress {
        background: $neutral-bg;
    }
    .dot-font-size {
        font-size: $h2-font-size;
    }
}
</style>
