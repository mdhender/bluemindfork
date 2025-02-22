<template>
    <div class="file-item-content">
        <bm-container
            :class="{ 'cursor-pointer': !isUploading(file) }"
            role="button"
            tabindex="0"
            @mouseover="hover = true"
            @mouseleave="hover = false"
            @click="$emit('click-item', file)"
            @keypress.enter="$emit('click-item', file)"
        >
            <file-thumbnail
                v-if="!compact"
                :file="file"
                :preview="isAllowedToPreview && !hasBlockedRemoteContent"
                :class="{ muted: isUploading(file) }"
            >
                <template v-if="hover" #overlay="{ hasPreview }">
                    <slot name="overlay" :has-preview="hasPreview" :file="file" />
                </template>
            </file-thumbnail>
            <file-infos :file="file" :class="{ muted: isUploading(file) }">
                <template #actions>
                    <slot name="actions" :file="file" />
                </template>
            </file-infos>
            <bm-progress
                v-if="isUploading(file)"
                :value="progress.loaded"
                :max="progress.total"
                :animated="progress.animated"
                :variant="errorMessage ? 'danger' : 'secondary'"
            />
        </bm-container>
        <div v-if="errorMessage(file)" class="row px-1">
            <bm-notice class="w-100" :text="errorMessage(file)" />
        </div>
    </div>
</template>

<script>
import { BmContainer, BmProgress, BmNotice } from "@bluemind/ui-components";
import { fileUtils } from "@bluemind/mail";
import { PreviewMixin } from "~/mixins";
import FileThumbnail from "./FileItem/FileThumbnail";
import FileInfos from "./FileItem/FileInfos";
const { FileStatus } = fileUtils;

export default {
    name: "FileItemContent",
    components: {
        BmContainer,
        BmProgress,
        BmNotice,
        FileThumbnail,
        FileInfos
    },
    mixins: [PreviewMixin],
    props: {
        compact: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return { hover: false };
    },
    computed: {
        progress() {
            return this.file.progress || {};
        }
    },
    watch: {
        file: {
            handler() {
                if (this.hasRemoteContent) {
                    this.$emit("remote-content");
                }
            },
            immediate: true
        }
    },
    methods: {
        errorMessage(file) {
            return file.status === FileStatus.ERROR ? this.$t("alert.mail.message.draft.attach.error") : undefined;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.file-item-content {
    .container {
        position: relative;

        background-color: $surface;
        outline: $input-border-width solid $neutral-fg-lo3;
        outline-offset: -$input-border-width;

        display: flex;
        flex-direction: column;
        padding: $sp-3 $sp-4;
        gap: $sp-3;

        &:hover {
            background-color: $neutral-bg;
            outline-color: $neutral-fg-lo2;
        }

        &:focus {
            outline: $input-border-width dashed $neutral-fg;
        }

        &.cursor-pointer {
            cursor: pointer;
        }

        .progress {
            position: absolute;
            top: 0;
            margin-left: -0.5rem;
            margin-right: -0.5rem;
            height: 0.125rem;
            width: 100%;
            background-color: transparent;
        }

        .muted {
            opacity: 0.5;
        }

        .remove-button {
            margin-left: $sp-2;
        }
    }
    &.active .container {
        outline: 2 * $input-border-width solid $secondary-fg;
        outline-offset: -2 * $input-border-width;
    }
}
</style>
