<template>
    <chain-of-responsibility :is-responsible="activeFolder === MY_TRASH.key">
        <section class="mail-home-screen mail-trash-screen" aria-labelledby="text-1 text-2">
            <div class="starter-text-and-actions">
                <div class="starter-main">
                    <h1 id="text-1">{{ $t("mail.message.starter") }}</h1>
                    <div id="text-2">{{ $t("common.or") }}</div>
                    <empty-folder-action v-slot="action">
                        <bm-button
                            variant="fill-danger"
                            size="lg"
                            icon="broom"
                            :disabled="isTrashEmpty"
                            @click="action.execute"
                        >
                            {{ $t("mail.actions.empty_trash.label") }}
                        </bm-button>
                    </empty-folder-action>
                </div>
                <div class="starter-links">
                    <div class="starter-link">
                        <bm-icon icon="inbox" />
                        <bm-button :to="{ name: 'v:mail:home', params: { folder: MY_INBOX.path } }" variant="link">
                            {{ $t("mail.message.starter.display.inbox") }}
                        </bm-button>
                    </div>
                </div>
            </div>
            <bm-illustration :value="isTrashEmpty ? 'trash-empty' : 'trash-filled'" size="lg" over-background />
        </section>
    </chain-of-responsibility>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { CONVERSATION_LIST_COUNT, FOLDER_HAS_CHILDREN, MY_INBOX, MY_TRASH } from "~/getters";
import { BmButton, BmIcon, BmIllustration } from "@bluemind/ui-components";

import EmptyFolderAction from "~/components/MailFolder/EmptyFolderAction";
import ChainOfResponsibility from "../../ChainOfResponsibility";

export default {
    name: "MailTrashScreen",
    components: { ChainOfResponsibility, EmptyFolderAction, BmButton, BmIcon, BmIllustration },
    computed: {
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapGetters("mail", { CONVERSATION_LIST_COUNT, FOLDER_HAS_CHILDREN, MY_INBOX, MY_TRASH }),
        isTrashEmpty() {
            const folder = this.folders[this.activeFolder];
            return this.CONVERSATION_LIST_COUNT === 0 && !this.FOLDER_HAS_CHILDREN(folder);
        }
    },

    priority: 128
};
</script>
