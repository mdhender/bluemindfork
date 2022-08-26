<template>
    <div v-if="maxSize" class="files-header">
        <bm-icon icon="paper-clip" :class="paperClipColor" />
        <div class="size-info bold" :class="isTooHeavy ? 'text-danger' : ''">
            <div>
                {{ $tc("common.attachments", files.length, { count: files.length }) }}
            </div>
            <div>({{ displaySize(filesWeight) }} / {{ displaySize(maxSize) }})</div>
            <bm-icon v-if="isTooHeavy" icon="exclamation-circle" />
        </div>
        <bm-progress :value="filesWeight" :max="maxSize" class="align-self-center" :variant="filesWeightColor" />
    </div>
    <div v-else>
        <bm-icon icon="paper-clip" />
        <div class="size-info bold">
            {{ $tc("common.attachments", files.length, { count: files.length }) }}
        </div>
    </div>
</template>

<script>
import { BmIcon, BmProgress } from "@bluemind/styleguide";
import { displayWithUnit } from "@bluemind/file-utils";

export default {
    name: "FilesHeader",
    components: { BmProgress, BmIcon },
    props: {
        files: {
            type: Array,
            required: true
        },
        maxSize: {
            type: Number,
            default: null
        }
    },
    computed: {
        filesWeight() {
            return this.files.map(attachment => attachment.size).reduce((total, size) => total + size, 0);
        },
        filesWeightInPercent() {
            return (this.filesWeight * 100) / this.maxSize;
        },
        filesWeightColor() {
            let color = "success";
            if (this.filesWeightInPercent > 100) {
                color = "danger";
            } else if (this.isHeavy) {
                color = "warning";
            }
            return color;
        },
        paperClipColor() {
            if (this.isTooHeavy) {
                return "text-danger";
            } else if (this.isHeavy) {
                return "text-warning";
            }
            return "";
        },
        isTooHeavy() {
            return this.filesWeight > this.maxSize;
        },
        isHeavy() {
            return this.filesWeightInPercent > 50 && this.filesWeightInPercent <= 100;
        }
    },
    methods: {
        displaySize(size) {
            size = Math.ceil(size / 100000) * 100000;
            return displayWithUnit(size, 6, this.$i18n);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.files-header {
    display: flex;
    align-items: center;
    height: $icon-btn-height-sm;

    & > .bm-icon {
        margin-right: $sp-2 + $sp-3;
    }
    .size-info {
        display: flex;
        gap: $sp-3;
        margin-right: $sp-3;
    }
    > .progress {
        height: base-px-to-rem(3);
    }
}
</style>
