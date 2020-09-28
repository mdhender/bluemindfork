<template>
    <article
        v-if="(currentMessageKey && isADraft) || (message && !isADraft)"
        class="mail-thread d-flex flex-column"
        :aria-label="$t('mail.application.region.messagethread')"
    >
        <mail-component-alert
            v-if="!areRemoteImagesUnblocked(currentMessageKey) && showBlockedImagesAlert"
            icon="exclamation-circle"
            @close="setShowBlockedImagesAlert(false)"
        >
            {{ $t("mail.content.alert.images.blocked") }}
            &nbsp;
            <a href="#" @click.prevent="showImages()">{{ $t("mail.content.alert.images.show") }}</a>
        </mail-component-alert>
        <mail-component-alert
            v-if="!folderOfCurrentMessage.writable && !isReadOnlyAlertDismissed"
            icon="info-circle-plain"
            @close="isReadOnlyAlertDismissed = true"
        >
            {{ $t("mail.content.alert.readonly") }}
        </mail-component-alert>
        <mail-composer v-if="isADraft" :message-key="currentMessageKey" />
        <mail-viewer v-else-if="message" />
        <div />
    </article>
</template>

<script>
import { mapGetters, mapMutations, mapState } from "vuex";

import { ItemUri } from "@bluemind/item-uri";

import MailComponentAlert from "../MailComponentAlert";
import MailComposer from "../MailComposer";
import MailViewer from "../MailViewer";

export default {
    name: "MailThread",
    components: {
        MailComponentAlert,
        MailComposer,
        MailViewer
    },
    data() {
        return {
            isReadOnlyAlertDismissed: false
        };
    },
    computed: {
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapGetters("mail-webapp/currentMessage", { message: "message" }),
        ...mapState("mail-webapp", ["showBlockedImagesAlert"]),
        ...mapGetters("mail-webapp", ["areRemoteImagesUnblocked"]),
        ...mapState("mail", ["folders", "messages"]),
        folderOfCurrentMessage() {
            return this.folders[ItemUri.container(this.currentMessageKey)];
        },
        isADraft() {
            return this.currentMessageKey && this.messages[this.currentMessageKey]
                ? this.messages[this.currentMessageKey].composing
                : false;
        }
    },
    watch: {
        message() {
            this.isReadOnlyAlertDismissed = false;
        }
    },
    methods: {
        ...mapMutations("mail-webapp", ["setShowBlockedImagesAlert", "unblockRemoteImages"]),
        showImages() {
            this.unblockRemoteImages(this.currentMessageKey);
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-thread {
    min-height: 100%;

    .mail-component-alert {
        margin-bottom: $sp-1;
    }

    .mail-composer ~ .mail-viewer {
        @media (max-width: map-get($grid-breakpoints, "lg")) {
            display: none !important;
        }
    }

    .mail-composer {
        @media (min-width: map-get($grid-breakpoints, "lg")) {
            height: auto !important;
        }
    }
}
</style>
