<template>
    <bm-form class="mail-composer-loading m-lg-3 flex-grow-1 d-flex flex-column bg-surface">
        <h3 class="d-none d-lg-flex card-header px-2 py-1">{{ $t("mail.main.new") }}</h3>
        <mail-composer-recipients-loading class="pl-3" />
        <hr class="mail-composer-splitter m-0" />
        <mail-viewer-content-loading class="flex-grow-1" />
        <mail-composer-footer-loading />
    </bm-form>
</template>

<script>
import { BmForm } from "@bluemind/styleguide";

import MailComposerFooterLoading from "./MailComposerFooterLoading";
import MailComposerRecipientsLoading from "./MailComposerRecipientsLoading";
import MailViewerContentLoading from "../MailViewer/MailViewerContentLoading";

export default {
    name: "MailComposerLoading",
    components: { BmForm, MailComposerFooterLoading, MailComposerRecipientsLoading, MailViewerContentLoading }
};
</script>
