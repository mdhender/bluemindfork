<template>
    <div class="file-item-content">
        <bm-container
            class="text-condensed py-2 px-2 mt-2"
            :class="{ 'cursor-pointer': !isUploading(file) }"
            @mouseover="hover = true"
            @mouseleave="hover = false"
            @click="$emit('click-item', file)"
        >
            <file-thumbnail
                v-if="!compact"
                :file="file"
                :preview="isAllowedToPreview && !hasBlockedRemoteContent"
                :class="{ muted: isUploading(file) }"
            >
                <template v-if="hover" #overlay="{ hasPreview }">
                    <slot name="overlay" :hasPreview="hasPreview" :file="file" />
                </template>
            </file-thumbnail>
            <file-infos :file="file" :class="{ muted: isUploading(file) }">
                <template v-if="hover" #actions>
                    <slot name="actions" :file="file" />
                </template>
            </file-infos>
            <bm-progress
                v-if="isUploading(file)"
                :value="file.progress.loaded"
                :max="file.progress.total"
                :animated="file.progress.animated"
                :variant="errorMessage ? 'danger' : 'secondary'"
            />
        </bm-container>
        <div v-if="errorMessage(file)" class="row px-1">
            <bm-notice class="w-100" :text="errorMessage(file)" />
        </div>
    </div>
</template>

<script>
import { BmContainer, BmProgress, BmNotice } from "@bluemind/styleguide";
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
@import "@bluemind/styleguide/css/_variables.scss";

.file-item {
    .container {
        position: relative;
        background-color: $surface;
        border: 1px solid $neutral-fg-lo3;

        &:hover {
            background-color: $neutral-bg;
            border-color: $neutral-fg-lo3;
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

        .file-text {
            line-height: 1.085em;
        }
        .cancel-button,
        .remove-button {
            margin-left: $sp-2;
        }
    }
}
</style>
