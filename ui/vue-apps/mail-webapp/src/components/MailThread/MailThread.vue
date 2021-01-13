<template>
    <article
        v-if="(currentMessageKey && isADraft) || (message && !isADraft)"
        class="mail-thread d-flex flex-column"
        :aria-label="$t('mail.application.region.messagethread')"
    >
        <mail-component-alert
            v-if="showRemoteImagesAlert"
            icon="exclamation-circle"
            @close="SET_SHOW_REMOTE_IMAGES_ALERT(false)"
        >
            {{ $t("mail.content.alert.images.blocked") }}
            &nbsp;
            <a href="#" @click.prevent="showRemoteImages">{{ $t("mail.content.alert.images.show") }}</a>
            <br />
            <a href="#" @click.prevent="trustSender">{{
                $t("mail.content.alert.images.trust.sender", { sender: message.from.address })
            }}</a>
        </mail-component-alert>
        <mail-component-alert
            v-if="!folderOfCurrentMessage.writable && !isReadOnlyAlertDismissed"
            icon="info-circle-plain"
            @close="isReadOnlyAlertDismissed = true"
        >
            {{ $t("mail.content.alert.readonly") }}
        </mail-component-alert>
        <mail-composer v-if="isADraft" :message-key="currentMessageKey" />
        <mail-viewer v-else-if="message" :message-key="currentMessageKey" />
        <div />
    </article>
</template>

<script>
import { mapMutations, mapState } from "vuex";

import { createFromRecipient, VCardAdaptor } from "@bluemind/contact";
import { inject } from "@bluemind/inject";
import { ItemUri } from "@bluemind/item-uri";

import { SET_BLOCK_REMOTE_IMAGES, SET_SHOW_REMOTE_IMAGES_ALERT } from "~mutations";
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
        ...mapState("mail", ["folders", "messages"]),
        ...mapState("mail", { showRemoteImagesAlert: state => state.consultPanel.remoteImages.showAlert }),
        message() {
            return this.messages[this.currentMessageKey];
        },
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
        },
        currentMessageKey() {
            this.SET_SHOW_REMOTE_IMAGES_ALERT(false);
            this.SET_BLOCK_REMOTE_IMAGES(false);
        }
    },
    methods: {
        ...mapMutations("mail", { SET_BLOCK_REMOTE_IMAGES, SET_SHOW_REMOTE_IMAGES_ALERT }),
        showRemoteImages() {
            this.SET_SHOW_REMOTE_IMAGES_ALERT(false);
            this.SET_BLOCK_REMOTE_IMAGES(false);
        },
        trustSender() {
            this.showRemoteImages();
            const contact = createFromRecipient(this.message.from);
            const myContactsAddressbookContainerUid = "book:Contacts_" + inject("UserSession").userId;
            inject("AddressBookPersistence", myContactsAddressbookContainerUid).create(
                contact.uid,
                VCardAdaptor.toVCard(contact)
            );
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-thread {
    min-height: 100%;

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
