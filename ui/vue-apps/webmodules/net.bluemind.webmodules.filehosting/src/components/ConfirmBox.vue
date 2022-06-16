<template>
    <div class="fh-confirm-box" visible title-class="ml-2">
        <div class="mr-4">
            <div class="d-flex align-items-center mb-3">
                <bm-icon icon="file" size="2x" class="mr-2 text-secondary" />
                <span class="text-secondary mr-1 font-size-h1">&#8226;</span>
                <span class="text-secondary font-size-h1">&#8226;</span>
                <bm-icon icon="interrogation" size="2x" class="text-secondary" />
                <span class="text-neutral-fg-lo2 mr-1 font-size-h1">&#8226;</span>
                <span class="text-neutral-fg-lo2 mr-1 font-size-h1">&#8226;</span>
                <bm-icon icon="chevron-right" size="lg" class="text-neutral-fg-lo2" />
                <bm-icon icon="cloud" class="ml-2 text-secondary" size="2x" />
            </div>
            <div class="mb-4">
                <slot name="text"> </slot>
            </div>
            <div v-for="(attachment, idx) in attachments" :key="idx" class="position-relative mb-3">
                <fh-attachment-item :attachment="attachment" />
            </div>
        </div>
    </div>
</template>
<script>
import { BmIcon } from "@bluemind/styleguide";
import FhAttachmentItem from "./AttachmentItem";

export default {
    name: "FhConfirmBox",
    components: { BmIcon, FhAttachmentItem },
    props: {
        attachments: {
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
    .font-size-h1 {
        font-size: $h1-font-size;
    }
}
</style>
