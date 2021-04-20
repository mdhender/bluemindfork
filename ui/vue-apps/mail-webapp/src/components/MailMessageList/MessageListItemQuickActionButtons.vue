<template>
    <bm-button-toolbar class="message-list-item-quick-action-buttons">
        <bm-button-group>
            <template v-if="folderOfMessage.writable">
                <bm-button
                    :aria-label="$tc('mail.actions.remove.aria')"
                    :title="$tc('mail.actions.remove.aria')"
                    class="p-1 mr-2"
                    variant="inline-secondary"
                    @click.shift.exact.prevent.stop="REMOVE_MESSAGES(message)"
                    @click.exact.prevent.stop="MOVE_MESSAGES_TO_TRASH(message)"
                >
                    <bm-icon icon="trash" size="lg" />
                </bm-button>
                <bm-button
                    v-if="!message.flags.includes(Flag.SEEN)"
                    class="p-1"
                    :aria-label="$tc('mail.actions.mark_read.aria')"
                    :title="$tc('mail.actions.mark_read.aria')"
                    variant="inline-secondary"
                    @click.prevent.stop="MARK_MESSAGE_AS_READ(message)"
                >
                    <bm-icon icon="read" size="lg" />
                </bm-button>
                <bm-button
                    v-else
                    class="p-1"
                    :aria-label="$tc('mail.actions.mark_unread.aria')"
                    :title="$tc('mail.actions.mark_unread.aria')"
                    variant="inline-secondary"
                    @click.prevent.stop="MARK_MESSAGE_AS_UNREAD(message)"
                >
                    <bm-icon icon="unread" size="lg" />
                </bm-button>
                <bm-button
                    v-if="!message.flags.includes(Flag.FLAGGED)"
                    class="p-1 ml-2"
                    :aria-label="$tc('mail.actions.mark_flagged.aria')"
                    :title="$tc('mail.actions.mark_flagged.aria')"
                    variant="inline-secondary"
                    @click.prevent.stop="MARK_MESSAGE_AS_FLAGGED(message)"
                >
                    <bm-icon icon="flag-outline" size="lg" />
                </bm-button>
                <bm-button
                    v-else
                    class="p-1 ml-2"
                    :aria-label="$tc('mail.actions.mark_unflagged.aria')"
                    :title="$tc('mail.actions.mark_unflagged.aria')"
                    variant="inline-secondary"
                    @click.prevent.stop="MARK_MESSAGE_AS_UNFLAGGED(message)"
                >
                    <bm-icon class="text-warning" icon="flag-fill" size="lg" />
                </bm-button>
            </template>
        </bm-button-group>
    </bm-button-toolbar>
</template>

<script>
import { BmButtonToolbar, BmButtonGroup, BmButton, BmIcon } from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";
import { Flag } from "@bluemind/email";
import { MY_TRASH } from "~getters";
import { RemoveMixin } from "~mixins";

import {
    MARK_MESSAGE_AS_FLAGGED,
    MARK_MESSAGE_AS_READ,
    MARK_MESSAGE_AS_UNFLAGGED,
    MARK_MESSAGE_AS_UNREAD
} from "~actions";
export default {
    name: "MessageListItemQuickActionButtons",
    components: {
        BmButtonToolbar,
        BmButtonGroup,
        BmButton,
        BmIcon
    },
    mixins: [RemoveMixin],
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            Flag
        };
    },
    computed: {
        ...mapState("mail", ["folders", "activeFolder"]),
        ...mapGetters("mail", { MY_TRASH }),
        folderOfMessage() {
            return this.folders[this.message.folderRef.key];
        }
    },
    methods: {
        ...mapActions("mail", {
            MARK_MESSAGE_AS_FLAGGED,
            MARK_MESSAGE_AS_READ,
            MARK_MESSAGE_AS_UNFLAGGED,
            MARK_MESSAGE_AS_UNREAD
        })
    }
};
</script>
<style lang="scss">
.message-list-item-quick-action-buttons {
    .hovershadow:hover {
        box-shadow: 0px 2px 4px rgba(0, 0, 0, 0.25);
    }
}
</style>
