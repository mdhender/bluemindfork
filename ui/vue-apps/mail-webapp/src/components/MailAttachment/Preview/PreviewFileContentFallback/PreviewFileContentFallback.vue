<template>
    <div class="preview-file-content-fallback">
        <default-fallback
            v-if="!isViewable(file)"
            :icon="matchingIcon"
            class="file-type"
            :text="$t('mail.preview.nopreview.type')"
        />
        <default-fallback v-else-if="isLarge" icon="weight" :text="$t('mail.preview.nopreview.large')" />
        <default-fallback v-else-if="hasBlockedRemoteContent" icon="exclamation-circle" />
        <default-fallback v-else icon="spam" :text="$t('mail.preview.nopreview')" />
    </div>
</template>

<script>
import { partUtils } from "@bluemind/mail";
import { MimeType } from "@bluemind/email";

import { PreviewMixin } from "~/mixins";
import DefaultFallback from "./DefaultFallback";

const { isViewable } = partUtils;

export default {
    name: "PreviewFileContent",
    components: { DefaultFallback },
    mixins: [PreviewMixin],
    data() {
        return {};
    },
    computed: {
        matchingIcon() {
            return MimeType.matchingIcon(this.file.mime);
        }
    },
    methods: {
        isViewable
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.preview-file-content-fallback {
    min-height: 100%;
    justify-content: center;
    display: flex;

    .default-fallback {
        .blocked-preview {
            color: $lowest;
            background-color: $neutral-bg;
        }

        .file-type > svg {
            color: $highest;
            background-color: $neutral-bg;
        }
    }
}
</style>
