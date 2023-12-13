<template>
    <div class="mail-multiple-selection-screen">
        <bm-alert-area :alerts="[]" class="w-100" @remove="REMOVE">
            <template #default="context">
                <component :is="context.alert.renderer" :alert="context.alert" />
            </template>
        </bm-alert-area>
        <section class="mail-home-screen" aria-labelledby="text-1">
            <div class="starter-text-and-actions">
                <div class="starter-main">
                    <h1 id="text-1">{{ mainText }}</h1>
                </div>
            </div>
            <div class="illustration-and-actions">
                <div class="actions">
                    <bm-button
                        variant="text"
                        :title="markAsUnreadAriaText()"
                        :aria-label="markAsUnreadAriaText()"
                        icon="unread"
                        @click="markAsUnread()"
                    >
                        {{ markAsUnreadText }}
                    </bm-button>
                    <bm-button
                        variant="text"
                        :title="markAsReadAriaText()"
                        :aria-label="markAsReadAriaText()"
                        icon="read"
                        @click="markAsRead()"
                    >
                        {{ markAsReadText }}
                    </bm-button>
                    <bm-button
                        class="mark-as-flagged-button"
                        variant="text"
                        :title="markAsFlaggedAriaText()"
                        :aria-label="markAsFlaggedAriaText()"
                        icon="flag-fill"
                        @click="markAsFlagged()"
                    >
                        {{ markAsFlaggedText }}
                    </bm-button>
                    <bm-button
                        variant="text"
                        :title="markAsUnflaggedAriaText()"
                        :aria-label="markAsUnflaggedAriaText()"
                        icon="flag-outline"
                        @click="markAsUnflagged()"
                    >
                        {{ markAsUnflaggedText }}
                    </bm-button>
                    <bm-button variant="text" :title="moveAriaText()" icon="folder" @click.exact="openMoveFolderModal">
                        {{ moveText }}
                    </bm-button>
                    <bm-button
                        variant="text"
                        :title="removeAriaText()"
                        :aria-label="removeAriaText()"
                        icon="trash"
                        @click.exact="moveToTrash"
                        @click.shift.exact="remove"
                    >
                        {{ removeText }}
                    </bm-button>
                </div>
                <bm-illustration :value="illustration" size="lg" over-background />
            </div>
            <div class="cancel-selection-btn-wrapper">
                <bm-button variant="text" size="sm" icon="cross" @click="cancelSelection">
                    {{ $t("common.cancel.selection") }}
                </bm-button>
            </div>
        </section>
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
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { CLEAR, INFO, REMOVE } from "@bluemind/alert.store";
import { CONVERSATIONS_ACTIVATED, MAILBOX_INBOX, MAILBOX_JUNK, SELECTION_KEYS } from "~/getters";
import { UNSELECT_ALL_CONVERSATIONS } from "~/mutations";

import { folderUtils } from "@bluemind/mail";

import { BmAlertArea, BmButton, BmIllustration } from "@bluemind/ui-components";

import { ActionTextMixin, FlagMixin, MoveMixin, RemoveMixin, SelectionMixin } from "~/mixins";
import ChooseFolderModal from "./ChooseFolderModal";

const { DEFAULT_FOLDERS } = folderUtils;

export default {
    name: "MailMultipleSelectionScreen",
    components: {
        BmAlertArea,
        BmButton,
        BmIllustration,
        ChooseFolderModal
    },
    mixins: [ActionTextMixin, FlagMixin, MoveMixin, RemoveMixin, SelectionMixin],
    data() {
        return {
            isReadOnlyAlertDismissed: false,
            defaultFolders: []
        };
    },
    computed: {
        ...mapState("mail", ["activeFolder", "folders", "mailboxes"]),
        ...mapGetters("mail", {
            CONVERSATIONS_ACTIVATED,
            MAILBOX_INBOX,
            MAILBOX_JUNK,
            SELECTION_KEYS
        }),
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "right-panel") }),

        selectionSize() {
            return this.SELECTION_KEYS.length;
        },
        illustration() {
            if (2 <= this.selectionSize && this.selectionSize <= 4) {
                return `stack-${this.selectionSize}`;
            }
            if (this.selectionSize <= 100) {
                return "stack-small";
            }
            return "stack-large";
        },
        mainText() {
            return this.CONVERSATIONS_ACTIVATED
                ? this.$t("mail.conversations.selected", { count: this.selectionSize })
                : this.$t("mail.message.selected", { count: this.selectionSize });
        },
        readOnlyAlert() {
            return {
                alert: { name: "mail.READ_ONLY_FOLDER", uid: "READ_ONLY_FOLDER" },
                options: { area: "right-panel", renderer: "DefaultAlert" }
            };
        },
        currentFolder() {
            return this.folders[this.activeFolder];
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
        ...mapMutations("mail", { UNSELECT_ALL_CONVERSATIONS }),
        ...mapActions("alert", { REMOVE, CLEAR, INFO }),
        cancelSelection() {
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
    },

    priority: 1024
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-multiple-selection-screen {
    height: 100%;
    display: flex;
    flex-direction: column;

    .mail-home-screen {
        .starter-text-and-actions {
            flex: 0 1 base-px-to-rem(80);
        }

        .cancel-selection-btn-wrapper {
            display: flex;
            flex-direction: column;
            flex: 0 1 auto;
            min-height: base-px-to-rem(24) + $sp-6;

            &:before {
                flex: 0 1 $sp-7;
                content: "";
            }
            &:after {
                flex: 0 1 base-px-to-rem(136);
                content: "";
            }
        }

        .illustration-and-actions {
            align-self: flex-start;
            margin: 0 auto;

            display: flex;
            gap: $sp-7;

            .actions {
                display: flex;
                flex-direction: column;
                align-items: flex-start;
                gap: $sp-2;
                padding-top: base-px-to-rem(44);
                padding-left: $sp-6;

                .mark-as-flagged-button .icon-flag-fill {
                    color: $warning-fg;
                }
            }

            .bm-illustration {
                position: relative;
                width: 334px;
                height: 310px;

                & > svg {
                    position: absolute;
                    left: -96px;
                    top: -62px;
                }
            }
        }
    }
}
</style>
