<template>
    <div v-if="files.length > 0" class="files-block p-2">
        <div class="d-flex align-items-center">
            <bm-button
                variant="inline-neutral"
                :aria-label="$t('common.toggleAttachments')"
                :title="$t('common.toggleAttachments')"
                @click.prevent="toggleExpand"
            >
                <bm-icon :icon="isExpanded ? 'caret-down' : 'caret-right'" size="xs" />
            </bm-button>
            <files-header :files="files" :max-size="maxSize" />
        </div>
        <bm-row v-if="seeMoreFiles" class="ml-3 mr-1">
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
            <bm-col lg="4" cols="12" class="pt-2 border-transparent">
                <bm-button
                    variant="outline-neutral"
                    class="w-100 h-100 py-2"
                    :title="$t('common.toggleAttachments')"
                    :aria-label="$t('common.toggleAttachments')"
                    @click="toggleExpand"
                >
                    + {{ $tc("common.attachments", files.length - 2, { count: files.length - 2 }) }}
                </bm-button>
            </bm-col>
        </bm-row>
        <bm-row v-else class="ml-3 mr-1">
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
            variant="outline-neutral"
            class="mr-2 align-self-center"
            size="sm"
            @click="$emit('saveAllAttachments')"
        >
            {{ $t("common.save_all") }}
        </bm-button>-->
    </div>
</template>

<script>
import { BmButton, BmCol, BmIcon, BmRow } from "@bluemind/styleguide";

import FileItem from "./FileItem";
import FilesHeader from "./FilesHeader";

export default {
    name: "FilesBlock",
    components: {
        BmButton,
        BmCol,
        BmIcon,
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
@import "@bluemind/styleguide/css/_variables.scss";

.files-block {
    background-color: $neutral-bg-lo1;
}
.files-block .col-4,
.files-block .col-lg-4 {
    padding-right: $sp-1 !important;
    padding-left: $sp-1 !important;
}

.border-transparent {
    border: 1px solid transparent !important;
}
</style>
