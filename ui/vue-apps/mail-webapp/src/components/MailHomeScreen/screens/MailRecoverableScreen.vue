<template>
    <chain-of-responsibility :is-responsible="isRecoverableItems">
        <section class="mail-home-screen mail-recoverable-screen" aria-labelledby="text-1 text-2">
            <div class="starter-text-and-actions">
                <div class="starter-main">
                    <h1 id="text-1">{{ $t("mail.message.starter") }}</h1>
                    <div id="text-2" class="description">{{ $t("mail.message.starter.recoverable.details") }}</div>
                </div>
                <div class="starter-links">
                    <bm-button
                        icon="arrow-left"
                        :to="$router.relative({ name: 'v:mail:home', params: { filter: null } }, $route)"
                        variant="link"
                    >
                        {{ $tc("mail.recoverable.back") }}
                    </bm-button>
                </div>
            </div>
            <bm-illustration value="recoverable-messages" size="lg" over-background />
            <div class="after-illustration" />
        </section>
    </chain-of-responsibility>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { CONVERSATION_LIST_DELETED_FILTER_ENABLED, MY_TRASH } from "~/getters";
import { BmButton, BmIcon, BmIllustration } from "@bluemind/ui-components";

import EmptyFolderAction from "~/components/MailFolder/EmptyFolderAction";
import ChainOfResponsibility from "../../ChainOfResponsibility";

export default {
    name: "MailRecoverableScreen",
    components: { ChainOfResponsibility, BmButton, BmIllustration },
    computed: {
        ...mapState("mail", ["activeFolder"]),
        ...mapGetters("mail", { CONVERSATION_LIST_DELETED_FILTER_ENABLED, MY_TRASH }),
        isRecoverableItems() {
            return this.activeFolder === this.MY_TRASH.key && this.CONVERSATION_LIST_DELETED_FILTER_ENABLED;
        }
    },

    priority: 132
};
</script>
