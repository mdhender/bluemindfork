<template>
    <div class="mail-multiple-selection-actions">
        <bm-alert-area :alerts="alerts" class="w-100" @remove="REMOVE">
            <template v-slot="context">
                <component :is="context.alert.renderer" :alert="context.alert" />
            </template>
        </bm-alert-area>
        <div
            class="h-100 mt-5 d-flex flex-column text-center align-items-center"
            :style="'background: url(' + multipleSelectionIllustration + ') no-repeat center top'"
        >
            <div class="font-weight-bold mt-5 mb-2">
                <h1>{{ mainText }}</h1>
            </div>

            <div v-if="ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE" class="bg-white py-2 px-3 actions-button w-75 mt-4">
                <div class="arrow-up" />
                <bm-button
                    v-if="showMarkAsRead"
                    variant="outline-secondary"
                    :title="markAsReadAriaText()"
                    :aria-label="markAsReadAriaText()"
                    @click="markAsRead()"
                >
                    <bm-label-icon icon="read"> {{ markAsReadText }} </bm-label-icon>
                </bm-button>
                <bm-button
                    v-if="showMarkAsUnread"
                    variant="outline-secondary"
                    :title="markAsUnreadAriaText()"
                    :aria-label="markAsUnreadAriaText()"
                    @click="markAsUnread()"
                >
                    <bm-label-icon icon="unread"> {{ markAsUnreadText }} </bm-label-icon>
                </bm-button>
                <bm-button
                    v-if="showMarkAsFlagged"
                    variant="outline-secondary"
                    :title="markAsFlaggedAriaText()"
                    :aria-label="markAsFlaggedAriaText()"
                    @click="markAsFlagged()"
                >
                    <bm-label-icon icon="flag-outline"> {{ markAsFlaggedText }} </bm-label-icon>
                </bm-button>
                <bm-button
                    v-if="showMarkAsUnflagged"
                    variant="outline-secondary"
                    :title="markAsUnflaggedAriaText()"
                    :aria-label="markAsUnflaggedAriaText()"
                    @click="markAsUnflagged()"
                >
                    <bm-label-icon icon="flag-fill"> {{ markAsUnflaggedText }} </bm-label-icon>
                </bm-button>
                <bm-button
                    variant="outline-secondary"
                    :title="removeAriaText()"
                    :aria-label="removeAriaText()"
                    @click.exact="moveToTrash"
                    @click.shift.exact="remove"
                >
                    <bm-label-icon icon="trash"> {{ removeText }} </bm-label-icon>
                </bm-button>
            </div>

            <bm-button variant="inline-secondary" class="my-4" @click="removeSelection">
                {{ $t("common.cancel.selection") }}
            </bm-button>

            <hr v-if="!ALL_CONVERSATIONS_ARE_SELECTED" class="w-75 border-dark" />

            <div v-if="!ALL_CONVERSATIONS_ARE_SELECTED" class="mt-3">
                <h3 v-if="!CONVERSATION_LIST_IS_SEARCH_MODE" class="d-inline px-3 align-middle">
                    {{ $t("mail.message.select.all.folder") }}
                    <mail-folder-icon
                        :shared="isFolderOfMailshare(currentFolder)"
                        :folder="currentFolder"
                        class="font-weight-bold ml-1"
                    >
                        {{ currentFolder.path }}
                    </mail-folder-icon>
                </h3>
                <h3 v-else class="d-inline px-3 align-middle">
                    {{ $t("mail.message.select.all.search") }}
                    <bm-icon icon="search" /><span class="font-weight-bold"
                        >"{{ conversationList.search.pattern }}"</span
                    >
                </h3>
                <bm-button @click="SET_SELECTION(CONVERSATION_LIST_ALL_KEYS)">
                    {{ $t("common.select.all") }}
                </bm-button>
            </div>
        </div>
    </div>
</template>

<script>
import { CLEAR, INFO, REMOVE } from "@bluemind/alert.store";
import { BmAlertArea, BmButton, BmLabelIcon, BmIcon } from "@bluemind/styleguide";
import { mapActions, mapState, mapGetters, mapMutations } from "vuex";
import MailFolderIcon from "./MailFolderIcon";
import multipleSelectionIllustration from "../../assets/multiple-selection.png";

import { ActionTextMixin, FlagMixin, RemoveMixin } from "~/mixins";

import {
    ALL_CONVERSATIONS_ARE_SELECTED,
    ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE,
    MY_TRASH,
    CONVERSATIONS_ACTIVATED,
    CONVERSATION_LIST_ALL_KEYS,
    CONVERSATION_LIST_IS_SEARCH_MODE,
    SELECTION_KEYS
} from "~/getters";
import { SET_SELECTION, UNSELECT_ALL_CONVERSATIONS } from "~/mutations";
import { MailboxType } from "~/model/mailbox";

export default {
    name: "MailMultipleSelectionActions",
    components: {
        BmAlertArea,
        BmButton,
        BmIcon,
        BmLabelIcon,
        MailFolderIcon
    },
    mixins: [ActionTextMixin, FlagMixin, RemoveMixin],
    data() {
        return {
            isReadOnlyAlertDismissed: false,
            multipleSelectionIllustration
        };
    },
    computed: {
        ...mapState("mail", ["activeFolder", "folders", "mailboxes", "conversationList"]),
        ...mapState("mail", { messages: ({ conversations }) => conversations.messages }),
        ...mapGetters("mail", {
            ALL_CONVERSATIONS_ARE_SELECTED,
            ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE,
            MY_TRASH,
            CONVERSATION_LIST_ALL_KEYS,
            CONVERSATION_LIST_IS_SEARCH_MODE,
            SELECTION_KEYS
        }),
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "mail-multiple-selection-actions") }),
        currentFolder() {
            return this.folders[this.activeFolder];
        },
        mainText() {
            const count = this.SELECTION_KEYS.length;
            return this.$store.getters[`mail/${CONVERSATIONS_ACTIVATED}`]
                ? this.$t("mail.conversations.selected", { count })
                : this.$t("mail.message.selected", { count });
        },
        readOnlyAlert() {
            return {
                alert: { name: "mail.READ_ONLY_FOLDER", uid: "READ_ONLY_FOLDER" },
                options: { area: "mail-multiple-selection-actions", renderer: "DefaultAlert" }
            };
        }
    },
    watch: {
        ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE: {
            immediate: true,
            handler(value) {
                if (!value) {
                    this.INFO(this.readOnlyAlert);
                } else {
                    this.REMOVE(this.readOnlyAlert.alert);
                }
            }
        }
    },
    destroyed() {
        this.CLEAR("mail-multiple-selection-actions");
    },
    methods: {
        ...mapMutations("mail", { SET_SELECTION, UNSELECT_ALL_CONVERSATIONS }),
        ...mapActions("alert", { REMOVE, CLEAR, INFO }),
        removeSelection() {
            this.UNSELECT_ALL_CONVERSATIONS();
        },
        isFolderOfMailshare(folder) {
            return this.mailboxes[folder.mailboxRef.key].type === MailboxType.MAILSHARE;
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

$arrow-width: 3rem;
$arrow-height: $arrow-width / 2;

.mail-multiple-selection-actions {
    h1 {
        color: $info-dark;
        font-size: 2rem;
    }
    hr {
        height: 1px;
    }
    .actions-button {
        .btn {
            margin: $sp-2 $sp-1;
        }
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
}
</style>
