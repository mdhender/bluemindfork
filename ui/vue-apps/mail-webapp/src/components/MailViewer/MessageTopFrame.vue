<template>
    <div class="message-top-frame">
        <reports-viewer :message="message" />
    </div>
</template>

<script>
import ReportsViewer from "./ReportsViewer";

/**
 * TODO: accept extensions
 */
export default {
    name: "MessageTopFrame",
    components: { ReportsViewer },
    props: { message: { type: Object, required: true } }
};
</script>
