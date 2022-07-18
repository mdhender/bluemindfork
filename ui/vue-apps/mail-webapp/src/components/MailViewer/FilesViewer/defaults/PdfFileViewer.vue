<template>
    <div class="pdf-file-viewer">
        <iframe :src="src" frameBorder="0" toolbar="0" />
    </div>
</template>
<script>
import FileViewerMixin from "../FileViewerMixin";

export default {
    name: "PdfFileViewer",
    mixins: [FileViewerMixin],
    $capabilities: ["application/pdf"]
};
</script>
<style lang="scss">
.pdf-file-viewer {
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
