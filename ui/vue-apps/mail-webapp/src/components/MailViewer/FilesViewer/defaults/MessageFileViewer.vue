<template>
    <mail-viewer-content
        v-if="previewMessage"
        class="message-file-viewer bg-surface px-7 py-6 m-auto"
        :message="previewMessage"
    />
</template>

<script>
import { IMPORT_EML } from "~/actions";
import FileViewerMixin from "../FileViewerMixin";

export default {
    name: "MessageFileViewer",
    mixins: [FileViewerMixin],
    $capabilities: ["message/rfc822"],
    data() {
        return { previewMessage: null };
    },
    watch: {
        src: {
            handler: async function (emlUrl) {
                if (emlUrl) {
                    this.previewMessage = await this.$store.dispatch(`mail/${IMPORT_EML}`, { emlUrl });
                }
            },
            immediate: true
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "../../variables";

.message-file-viewer {
    width: 95%;
    min-height: 100%;
    .text-html-file-viewer,
    .text-plain-file-viewer {
        width: unset !important;
        padding-left: $avatar-width + $single-mail-avatar-main-gap;
    }
}
</style>
