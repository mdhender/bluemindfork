<template>
    <div class="audio-part-viewer"><audio controls :src="src" /></div>
</template>
<script>
import PartViewerMixin from "../PartViewerMixin";

export default {
    name: "AudioPartViewer",
    mixins: [PartViewerMixin],
    $capabilities: ["audio/*"]
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
