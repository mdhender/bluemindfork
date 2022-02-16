<template>
    <div class="video-part-viewer"><video controls :src="src" /></div>
</template>
<script>
import { getPartPreviewUrl } from "@bluemind/email";
import PartViewerMixin from "../PartViewerMixin";

export default {
    name: "VideoPartViewer",
    mixins: [PartViewerMixin],
    $capabilities: ["video/*"],
    computed: {
        src() {
            return getPartPreviewUrl(this.message.folderRef.uid, this.message.remoteRef.imapUid, this.part);
        }
    }
};
</script>
<style lang="scss">
.video-part-viewer {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;

    video {
        max-height: 100%;
        max-width: 100%;
    }
}
</style>
