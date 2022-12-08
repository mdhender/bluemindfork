<template>
    <div v-if="files.length > 0" class="files-block">
        <div class="expand-and-header">
            <bm-button-expand
                :aria-label="$t('common.toggleAttachments')"
                :title="$t('common.toggleAttachments')"
                :expanded="isExpanded"
                size="sm"
                @click.prevent="toggleExpand"
            />
            <files-header :files="files" :max-size="maxSize" />
        </div>
        <bm-row v-if="seeMoreFiles" class="files-row">
            <bm-col v-for="file in files.slice(0, 2)" :key="file.key" lg="4" cols="12">
                <file-item
                    :file="file"
                    :compact="true"
                    @click-item="$emit('click-item', $event)"
                    @remote-content="$emit('remote-content')"
                >
                    <template #actions="slotProps">
                        <slot name="actions" :file="slotProps.file" />
                    </template>
                </file-item>
            </bm-col>
            <bm-col lg="4" cols="12" class="border-transparent">
                <bm-button
                    variant="outline"
                    class="w-100 h-100"
                    :title="$t('common.toggleAttachments')"
                    :aria-label="$t('common.toggleAttachments')"
                    @click="toggleExpand"
                >
                    + {{ $tc("common.attachments", files.length - 2, { count: files.length - 2 }) }}
                </bm-button>
            </bm-col>
        </bm-row>
        <bm-row v-else class="files-row">
            <bm-col v-for="(file, index) in files" :key="index" lg="4" cols="12" :compact="!isExpanded">
                <file-item
                    :file="file"
                    :compact="!isExpanded"
                    @click-item="$emit('click-item', $event)"
                    @remote-content="$emit('remote-content')"
                >
                    <template #actions="slotProps">
                        <slot name="actions" :file="slotProps.file" />
                    </template>
                    <template #overlay="slotProps">
                        <slot name="overlay" :hasPreview="slotProps.hasPreview" :file="slotProps.file" />
                    </template>
                </file-item>
            </bm-col>
        </bm-row>
        <!-- Save all button with i18n, please dont delete it 
            <bm-button
            variant="outline"
            class="mr-2 align-self-center"
            size="sm"
            @click="$emit('saveAllAttachments')"
        >
            {{ $t("common.save_all") }}
        </bm-button>-->
    </div>
</template>

<script>
import { BmButton, BmButtonExpand, BmCol, BmRow } from "@bluemind/ui-components";

import FileItem from "./FileItem";
import FilesHeader from "./FilesHeader";

export default {
    name: "FilesBlock",
    components: {
        BmButton,
        BmButtonExpand,
        BmCol,
        BmRow,
        FileItem,
        FilesHeader
    },
    props: {
        expanded: {
            type: Boolean,
            default: false
        },
        files: {
            type: Array,
            required: true
        },
        maxSize: {
            type: Number,
            default: null
        }
    },
    data() {
        return {
            isExpanded: this.expanded
        };
    },
    computed: {
        hasMoreThan3Files() {
            return this.files.length > 3;
        },
        seeMoreFiles() {
            return !this.isExpanded && this.hasMoreThan3Files;
        }
    },
    watch: {
        files() {
            this.isExpanded = this.expanded;
        }
    },

    methods: {
        async toggleExpand() {
            this.isExpanded = !this.isExpanded;
        }
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "~@bluemind/ui-components/src/css/variables";
@import "../MailViewer/_variables.scss";

.files-block {
    display: flex;
    flex-direction: column;
    gap: $sp-4;
    padding-top: $sp-4;
    padding-bottom: $sp-5;
    padding-left: $sp-2;
    padding-right: $inserts-padding-right;
    background-color: $neutral-bg-lo1;

    .expand-and-header {
        display: flex;
        gap: $sp-2;
    }

    .files-row {
        margin-left: calc(#{$icon-btn-width-compact-sm + $sp-2} - #{math.div($grid-gutter-width, 2)});
        margin-right: math.div(-$grid-gutter-width, 2);
        row-gap: $grid-gutter-width;
    }
}

.border-transparent {
    border: 1px solid transparent !important;
}
</style>
