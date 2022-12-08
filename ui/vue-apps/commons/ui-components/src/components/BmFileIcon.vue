<template>
    <bm-icon :icon="matchingIcon" :size="size" />
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
