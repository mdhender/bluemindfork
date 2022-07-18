<template>
    <div class="video-file-viewer"><video controls :src="src" /></div>
</template>
<script>
import FileViewerMixin from "../FileViewerMixin";

export default {
    name: "VideoFileViewer",
    mixins: [FileViewerMixin],
    $capabilities: ["video/*"]
};
</script>
<style lang="scss">
.video-file-viewer {
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
