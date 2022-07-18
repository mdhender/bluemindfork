<template>
    <div class="audio-file-viewer"><audio controls :src="src" /></div>
</template>
<script>
import FileViewerMixin from "../FileViewerMixin";

export default {
    name: "AudioFileViewer",
    mixins: [FileViewerMixin],
    $capabilities: ["audio/*"]
};
</script>
<style lang="scss">
.audio-file-viewer {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
}
</style>
