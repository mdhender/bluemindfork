<template>
    <div class="pdf-part-viewer">
        <iframe :src="src" frameBorder="0" toolbar="0" />
    </div>
</template>
<script>
import PartViewerMixin from "../PartViewerMixin";

export default {
    name: "PdfPartViewer",
    mixins: [PartViewerMixin],
    $capabilities: ["application/pdf"]
};
</script>
<style lang="scss">
.pdf-part-viewer {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;

    iframe {
        min-height: 100%;
        width: 100%;
    }
}
</style>
