<template>
    <div v-if="maxSize" class="files-header">
        <bm-icon icon="paper-clip" class="mr-1 ml-2" :class="paperClipColor" size="lg" />
        <span :class="isTooHeavy ? 'text-danger font-weight-bold' : ''">
            {{
                $tc("common.attachments", files.length, {
                    count: files.length
                })
            }}
            <span class="attachements-weigth"> ({{ displaySize(filesWeight) }} / {{ displaySize(maxSize) }})</span>
            <bm-icon v-if="isTooHeavy" icon="exclamation-circle" />
        </span>
        <bm-progress
            :value="filesWeight"
            :max="maxSize"
            height="2px"
            class="pl-1 align-self-center"
            :variant="filesWeightColor"
        />
    </div>
    <div v-else>
        <bm-icon icon="paper-clip" class="mr-1 ml-2" size="lg" />
        <span class="font-weight-bold pr-2">
            {{ $tc("common.attachments", files.length, { count: files.length }) }}
        </span>
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
