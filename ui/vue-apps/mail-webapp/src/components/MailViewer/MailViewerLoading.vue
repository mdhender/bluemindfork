<template>
    <section class="mail-viewer-loading d-flex flex-column flex-grow-1 bg-surface">
        <mail-viewer-loading-header />
        <bm-row class="px-lg-6">
            <bm-col>
                <hr class="mail-viewer-splitter my-0" />
                <mail-viewer-content-loading />
            </bm-col>
        </bm-row>
    </section>
</template>
<script>
import MailViewerLoadingHeader from "./MailViewerLoadingHeader";
import MailViewerContentLoading from "../MailViewer/MailViewerContentLoading";
import { BmCol, BmRow } from "@bluemind/ui-components";

export default {
    name: "MailViewerLoading",
    components: {
        BmCol,
        BmRow,
        MailViewerLoadingHeader,
        MailViewerContentLoading
    }
};
</script>
