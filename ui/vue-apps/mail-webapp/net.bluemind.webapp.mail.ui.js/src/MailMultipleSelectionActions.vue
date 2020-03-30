<template>
    <div
        class="mail-multiple-selection-actions h-100 mt-5 d-flex flex-column text-center align-items-center"
        :style="'background: url(' + multipleSelectionIllustration + ') no-repeat center top'"
    >
        <div class="font-weight-bold mt-5 mb-2">
            <h1>{{ selectedMessageKeys.length }}</h1>
            <h1>{{ $t("mail.message.selected") }}</h1>
        </div>

        <div class="bg-white py-2 px-3 actions-button w-75 mt-4">
            <div class="arrow-up" />
            <bm-button
                v-if="!areAllSelectedMessagesRead"
                variant="outline-secondary"
                @click="markAsRead(selectedMessageKeys)"
            >
                <bm-label-icon icon="read">{{
                    $tc("mail.actions.mark_as_read", selectedMessageKeys.length)
                }}</bm-label-icon>
            </bm-button>
            <bm-button
                v-if="!areAllSelectedMessagesUnread"
                variant="outline-secondary"
                @click="markAsUnread(selectedMessageKeys)"
            >
                <bm-label-icon icon="unread">
                    {{ $tc("mail.actions.mark_as_unread", selectedMessageKeys.length) }}
                </bm-label-icon>
            </bm-button>
            <!-- TODO: uncomment when ready
             <bm-button variant="outline-secondary">
                <bm-label-icon icon="forbidden"> {{ $t("mail.actions.spam") }} </bm-label-icon>
            </bm-button>
            <bm-button variant="outline-secondary">
                <bm-label-icon icon="trash"> {{ $t("mail.actions.remove") }} </bm-label-icon>
            </bm-button>
            <bm-button variant="outline-secondary">
                <bm-label-icon icon="forward"> {{ $t("common.forward") }} </bm-label-icon>
            </bm-button>
            <bm-button variant="outline-secondary">
                <bm-label-icon icon="flag-outline"> {{ $t("mail.actions.followup") }} </bm-label-icon>
            </bm-button>-->
        </div>

        <bm-button variant="link" class="my-4" @click="removeSelection">
            <h2>{{ $t("common.cancel.selection") }}</h2>
        </bm-button>

        <hr v-if="areAllSelected" class="w-75 border-dark" />

        <div v-if="areAllSelected" class="mt-3">
            <h3 v-if="!isSearch" class="d-inline px-3 align-middle">
                {{ $t("mail.message.select.all.folder") }}
                <mail-folder-icon :shared="currentFolder.isShared" :folder="currentFolder.value">
                    <span class="font-weight-bold">{{ currentFolder.value.fullName }}</span>
                </mail-folder-icon>
            </h3>
            <h3 v-else class="d-inline px-3 align-middle">
                {{ $t("mail.message.select.all.search") }}
                <bm-icon icon="search" /> <span class="font-weight-bold">"{{ search.pattern }}"</span>
            </h3>
            <bm-button @click="addAllToSelectedMessages(itemKeys)">{{ $t("common.select.all") }}</bm-button>
        </div>
    </div>
</template>

<script>
import { BmButton, BmLabelIcon, BmIcon } from "@bluemind/styleguide";
import { mapActions, mapState, mapGetters, mapMutations } from "vuex";
import MailFolderIcon from "./MailFolderIcon";
import multipleSelectionIllustration from "../assets/multiple-selection.png";

export default {
    name: "MailMultipleSelectionActions",
    components: {
        BmButton,
        BmIcon,
        BmLabelIcon,
        MailFolderIcon
    },
    data() {
        return {
            multipleSelectionIllustration
        };
    },
    computed: {
        ...mapState("mail-webapp", ["selectedMessageKeys", "search"]),
        ...mapState("mail-webapp/messages", ["itemKeys"]),
        ...mapGetters("mail-webapp", ["currentFolder", "areAllSelectedMessagesRead", "areAllSelectedMessagesUnread"]),
        isSearch() {
            return this.$route.path.includes("/search");
        },
        areAllSelected() {
            return this.selectedMessageKeys.length !== this.itemKeys.length;
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["markAsRead", "markAsUnread"]),
        ...mapMutations("mail-webapp", ["addAllToSelectedMessages", "deleteAllSelectedMessages"]),
        ...mapMutations("mail-webapp/currentMessage", { clearCurrentMessage: "clear" }),
        removeSelection() {
            this.deleteAllSelectedMessages();
            this.clearCurrentMessage();
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.mail-multiple-selection-actions h1 {
    color: $info-dark;
    font-size: 2rem;
}
.mail-multiple-selection-actions hr {
    height: 1px;
}

.mail-multiple-selection-actions .actions-button button {
    padding-right: $sp-2;
    padding-left: $sp-2;
    margin-right: $sp-1;
    margin-left: $sp-1;
    margin-bottom: $sp-2;
    margin-top: $sp-2;
}
$arrow-width: 3rem;
$arrow-height: $arrow-width / 2;

.mail-multiple-selection-actions .actions-button {
    .arrow-up {
        position: relative;
        display: block;
        width: $arrow-width;
        height: $arrow-height;
        bottom: $arrow-height + $sp-2;
        left: calc(50% - #{$arrow-width});

        &::before {
            position: absolute;
            content: "";
            border-color: transparent;
            border-style: solid;
            bottom: 0;
            border-width: 0 ($arrow-width / 2) $arrow-height;
            border-bottom-color: $white;
        }
    }
}
</style>
