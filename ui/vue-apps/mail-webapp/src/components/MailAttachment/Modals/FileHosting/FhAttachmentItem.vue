<template>
    <div class="fh-attachment-item">
        <div>
            <bm-label-icon icon="file" icon-size="2x" class="mt-2">
                <h2 class="text-secondary">{{ attachment.fileName }}</h2>
            </bm-label-icon>
            <slot name="item-actions" />
        </div>
        <span class="text-secondary ml-4 text-right">
            {{ displaySize(attachment.progress.loaded) }} / {{ displaySize(attachment.progress.total) }}
        </span>
        <bm-progress :value="attachment.progress.loaded" :max="attachment.progress.total" />
    </div>
</template>

<script>
import { BmLabelIcon, BmProgress } from "@bluemind/styleguide";
import { computeUnit } from "@bluemind/file-utils";

export default {
    name: "FhAttachmentItem",
    components: { BmLabelIcon, BmProgress },
    props: {
        attachment: {
            type: Object,
            required: true
        }
    },
    methods: {
        displaySize(size) {
            return computeUnit(size, this.$i18n);
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.fh-attachment-item {
    .progress {
        position: absolute;
        margin-bottom: $sp-2;
        top: 0;
        height: 0.125rem;
        width: 100%;
    }
}
</style>
