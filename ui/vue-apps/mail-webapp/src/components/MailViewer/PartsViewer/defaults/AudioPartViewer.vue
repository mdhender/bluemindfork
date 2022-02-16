<template>
    <div class="audio-part-viewer"><audio controls :src="src" /></div>
</template>
<script>
import { getPartPreviewUrl } from "@bluemind/email";
import PartViewerMixin from "../PartViewerMixin";

export default {
    name: "AudioPartViewer",
    mixins: [PartViewerMixin],
    $capabilities: ["audio/*"],
    computed: {
        src() {
            return getPartPreviewUrl(this.message.folderRef.uid, this.message.remoteRef.imapUid, this.part);
        }
    }
};
</script>
<style lang="scss">
.audio-part-viewer {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
}
</style>
