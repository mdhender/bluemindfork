<template>
    <article class="mail-message-panel overflow-x-hidden d-flex" :aria-label="$t('mail.application.region.message')">
        <mail-message v-if="ACTIVE_MESSAGE && ACTIVE_MESSAGE.loading === LoadingStatus.LOADED" class="flex-grow-1" />
        <mail-viewer-loading v-else />
    </article>
</template>

<script>
import { loadingStatusUtils } from "@bluemind/mail";
import MailMessage from "./MailMessage";
import MailViewerLoading from "../MailViewer/MailViewerLoading";
import { ACTIVE_MESSAGE } from "~/getters";
import { mapGetters } from "vuex";

export default {
    name: "MailMessagePanel",
    components: { MailMessage, MailViewerLoading },
    data() {
        return { LoadingStatus: loadingStatusUtils.LoadingStatus };
    },
    computed: {
        ...mapGetters("mail", { ACTIVE_MESSAGE })
    }
};
</script>

<style lang="scss">
.mail-message-panel {
    min-height: 100%;
    .overflow-x-hidden {
        overflow-x: hidden;
    }
}
</style>
