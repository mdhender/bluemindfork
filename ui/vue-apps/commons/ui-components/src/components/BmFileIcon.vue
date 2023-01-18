<template>
    <div class="bm-file-icon">
        <bm-icon :icon="matchingIcon" :size="size" />
    </div>
</template>

<script>
import { MimeType } from "@bluemind/email";
import BmIcon from "./BmIcon";

export default {
    name: "BmFileIcon",
    components: { BmIcon },
    props: {
        file: {
            type: Object,
            required: true
        },
        size: {
            type: String,
            default: "md"
        }
    },
    computed: {
        matchingIcon() {
            let mime = this.file.mime;
            if (!mime) {
                mime = MimeType.getFromFilename(this.file.name);
            }
            return MimeType.matchingIcon(mime);
        }
    }
};
</script>

<style lang="scss">
@import "../css/_variables.scss";

.bm-file-icon {
    display: flex;
    background-color: $lightest;
}
</style>
