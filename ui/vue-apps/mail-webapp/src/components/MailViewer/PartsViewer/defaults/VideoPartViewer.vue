<template>
    <div class="video-part-viewer"><video controls :src="src" /></div>
</template>
<script>
import PartViewerMixin from "../PartViewerMixin";

export default {
    name: "VideoPartViewer",
    mixins: [PartViewerMixin],
    $capabilities: ["video/*"]
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
        margin: 0 auto;
    }
}
</style>
