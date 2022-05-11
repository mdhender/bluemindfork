<template>
    <div class="fh-confirm-box" visible title-class="ml-2">
        <div class="mr-4">
            <div class="d-flex align-items-center mb-3">
                <bm-icon icon="file" size="2x" class="mr-2 text-primary" />
                <span class="text-primary mr-1 font-size-h1">&#8226;</span>
                <span class="text-primary font-size-h1">&#8226;</span>
                <bm-icon icon="interrogation" size="2x" class="text-primary" />
                <span class="text-secondary mr-1 font-size-h1">&#8226;</span>
                <span class="text-secondary mr-1 font-size-h1">&#8226;</span>
                <bm-icon icon="chevron-right" size="lg" class="text-secondary" />
                <bm-icon icon="cloud" class="ml-2 text-primary" size="2x" />
            </div>
            <div class="mb-4">
                <slot name="text"> </slot>
            </div>
            <div v-for="(attachment, idx) in attachments" :key="idx" class="position-relative mt-2">
                <fh-attachment-item :attachment="attachment" />
            </div>
        </div>
    </div>
</template>
<script>
import { BmIcon } from "@bluemind/styleguide";
import FhAttachmentItem from "./FhAttachmentItem";

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
    .progress {
        background: $secondary;
    }
    .font-size-h1 {
        font-size: $h1-font-size;
    }
}
</style>
