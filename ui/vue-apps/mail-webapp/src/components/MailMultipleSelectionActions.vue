<template>
    <div class="mail-multiple-selection-actions">
        <bm-alert-area :alerts="alerts" class="w-100" @remove="REMOVE">
            <template v-slot="context">
                <component :is="context.alert.renderer" :alert="context.alert" />
            </template>
        </bm-alert-area>
        <div
            class="h-100 mt-7 d-flex flex-column text-center align-items-center"
            :style="'background: url(' + multipleSelectionIllustration + ') no-repeat center top'"
        >
            <div class="font-weight-bold mt-7 mb-4">
                <h1>{{ mainText }}</h1>
            </div>

            <div
                v-if="ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE"
                class="bg-surface pt-2 py-5 px-6 actions-button w-75 mt-7"
            >
                <div class="arrow-up" />
                <bm-button
                    v-if="showMarkAsReadInMain() || showMarkAsReadInOthers()"
                    variant="outline"
                    :title="markAsReadAriaText()"
                    :aria-label="markAsReadAriaText()"
                    icon="read"
                    @click="markAsRead()"
                    >{{ markAsReadText }}
                </bm-button>
                <bm-button
                    v-if="showMarkAsUnreadInMain() || showMarkAsUnreadInOthers()"
                    variant="outline"
                    :title="markAsUnreadAriaText()"
                    :aria-label="markAsUnreadAriaText()"
                    icon="unread"
                    @click="markAsUnread()"
                >
                    {{ markAsUnreadText }}
                </bm-button>
                <bm-button
                    v-if="showMarkAsFlaggedInMain || showMarkAsFlaggedInOthers"
                    class="mark-as-flagged-button"
                    variant="outline"
                    :title="markAsFlaggedAriaText()"
                    :aria-label="markAsFlaggedAriaText()"
                    icon="flag-fill"
                    @click="markAsFlagged()"
                >
                    {{ markAsFlaggedText }}
                </bm-button>
                <bm-button
                    v-if="showMarkAsUnflaggedInMain || showMarkAsUnflaggedInOthers"
                    variant="outline"
                    :title="markAsUnflaggedAriaText()"
                    :aria-label="markAsUnflaggedAriaText()"
                    icon="flag-outline"
                    @click="markAsUnflagged()"
                >
                    {{ markAsUnflaggedText }}
                </bm-button>
                <bm-button
                    variant="outline"
                    :title="removeAriaText()"
                    :aria-label="removeAriaText()"
                    icon="trash"
                    @click.exact="moveToTrash"
                    @click.shift.exact="remove"
                    >{{ removeText }}
                </bm-button>
                <bm-button variant="outline" :title="moveAriaText()" icon="folder" @click.exact="openMoveFolderModal"
                    >{{ moveText }}
                </bm-button>
            </div>

            <bm-button variant="text" class="my-6" @click="removeSelection">
                {{ $t("common.cancel.selection") }}
            </bm-button>

            <hr v-if="!ALL_CONVERSATIONS_ARE_SELECTED" class="w-75 border-neutral" />

            <div v-if="!ALL_CONVERSATIONS_ARE_SELECTED" class="mt-3">
                <div v-if="!CONVERSATION_LIST_IS_SEARCH_MODE" class="d-inline-flex px-3 align-items-center">
                    <div>{{ $t("mail.message.select.all.folder") }}</div>
                    <mail-folder-icon
                        :mailbox="mailboxes[currentFolder.mailboxRef.key]"
                        :folder="currentFolder"
                        class="font-weight-bold ml-3"
                    >
                        {{ currentFolder.path }}
                    </mail-folder-icon>
                </div>
                <div v-else class="d-inline-flex px-3 align-items-center">
                    <div>{{ $t("mail.message.select.all.search") }}</div>
                    <bm-icon icon="search" class="mx-3" />
                    <span class="font-weight-bold">"{{ conversationList.search.pattern }}"</span>
                </div>
                <bm-button @click="SET_SELECTION(CONVERSATION_LIST_ALL_KEYS)">
                    {{ $t("common.select.all") }}
                </bm-button>
            </div>
        </div>
        <choose-folder-modal
            ref="move-modal"
            :ok-title="moveText"
            :cancel-title="$t('common.cancel')"
            :title="moveModalTitle"
            :mailboxes="[mailbox]"
            :default-folders="defaultFolders"
            :is-excluded="isExcluded"
            @ok="move"
        />
    </div>
</template>

<script>
import { CLEAR, INFO, REMOVE } from "@bluemind/alert.store";
import { BmAlertArea, BmButton, BmLabelIcon, BmIcon } from "@bluemind/ui-components";
import { mapActions, mapState, mapGetters, mapMutations } from "vuex";
import { folderUtils } from "@bluemind/mail";
import MailFolderIcon from "./MailFolderIcon";
import multipleSelectionIllustration from "../../assets/multiple-selection.png";

import { ActionTextMixin, FlagMixin, MoveMixin, RemoveMixin, SelectionMixin } from "~/mixins";

import {
    ALL_CONVERSATIONS_ARE_SELECTED,
    ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE,
    CONVERSATIONS_ACTIVATED,
    CONVERSATION_LIST_ALL_KEYS,
    CONVERSATION_LIST_IS_SEARCH_MODE,
    MAILBOX_INBOX,
    MAILBOX_JUNK,
    SELECTION_KEYS
} from "~/getters";

import { SET_SELECTION, UNSELECT_ALL_CONVERSATIONS } from "~/mutations";
import ChooseFolderModal from "./ChooseFolderModal";

const { DEFAULT_FOLDERS } = folderUtils;

export default {
    name: "MailMultipleSelectionActions",
    components: {
        BmAlertArea,
        BmButton,
        BmIcon,
        BmLabelIcon,
        MailFolderIcon,
        ChooseFolderModal
    },
    mixins: [ActionTextMixin, FlagMixin, MoveMixin, RemoveMixin, SelectionMixin],
    data() {
        return {
            isReadOnlyAlertDismissed: false,
            multipleSelectionIllustration,
            defaultFolders: []
        };
    },
    computed: {
        ...mapState("mail", ["activeFolder", "folders", "mailboxes", "conversationList"]),
        ...mapState("mail", { messages: ({ conversations }) => conversations.messages }),
        ...mapGetters("mail", {
            ALL_CONVERSATIONS_ARE_SELECTED,
            ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE,
            CONVERSATIONS_ACTIVATED,
            CONVERSATION_LIST_ALL_KEYS,
            CONVERSATION_LIST_IS_SEARCH_MODE,
            SELECTION_KEYS
        }),
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "right-panel") }),
        currentFolder() {
            return this.folders[this.activeFolder];
        },
        mainText() {
            const count = this.SELECTION_KEYS.length;
            return this.CONVERSATIONS_ACTIVATED
                ? this.$t("mail.conversations.selected", { count })
                : this.$t("mail.message.selected", { count });
        },
        readOnlyAlert() {
            return {
                alert: { name: "mail.READ_ONLY_FOLDER", uid: "READ_ONLY_FOLDER" },
                options: { area: "right-panel", renderer: "DefaultAlert" }
            };
        },
        mailbox() {
            return this.mailboxes[this.currentFolder.mailboxRef.key];
        },
        moveModalTitle() {
            return this.CONVERSATIONS_ACTIVATED
                ? this.$tc("mail.actions.move.conversations.aria", 2)
                : this.$tc("mail.actions.move.aria", 2);
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
        this.CLEAR("right-panel");
    },
    methods: {
        ...mapMutations("mail", { SET_SELECTION, UNSELECT_ALL_CONVERSATIONS }),
        ...mapActions("alert", { REMOVE, CLEAR, INFO }),
        removeSelection() {
            this.UNSELECT_ALL_CONVERSATIONS();
        },
        isExcluded(folder) {
            return folder && folder.path === this.currentFolder.path;
        },
        openMoveFolderModal() {
            const junk = this.$store.getters[`mail/${MAILBOX_JUNK}`](this.mailbox);
            const inbox = this.$store.getters[`mail/${MAILBOX_INBOX}`](this.mailbox);
            this.defaultFolders =
                this.currentFolder.imapName.toLowerCase() === DEFAULT_FOLDERS.INBOX.toLowerCase()
                    ? [junk]
                    : [inbox, junk];
            this.$refs["move-modal"].show();
        },
        move(folder) {
            this.MOVE_CONVERSATIONS({ conversations: this.selected, folder });
        }
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "@bluemind/ui-components/src/css/mixins/_responsiveness.scss";
@import "~@bluemind/ui-components/src/css/variables";

$arrow-width: 3rem;
$arrow-height: math.div($arrow-width, 2);

.mail-multiple-selection-actions {
    @include until-lg {
        display: none;
    }

    h1 {
        color: $primary-fg-hi1;
        font-size: 2rem;
    }
    hr {
        height: 1px;
    }
    .actions-button {
        .btn {
            margin: $sp-4 $sp-3;
        }
        .mark-as-flagged-button .fa-flag-fill {
            color: $warning-fg;
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
                border-width: 0 calc($arrow-width / 2) $arrow-height;
                border-bottom-color: $surface;
            }
        }
    }
}
</style>
