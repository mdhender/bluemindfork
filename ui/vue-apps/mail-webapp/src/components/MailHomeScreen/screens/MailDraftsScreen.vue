<template>
    <chain-of-responsibility :is-responsible="activeFolder === MY_DRAFTS.key">
        <section class="mail-home-screen mail-drafts-screen" aria-labelledby="text-2">
            <div class="starter-text-and-actions">
                <div class="starter-main">
                    <h1 id="text-1">{{ $t("mail.message.starter") }}</h1>
                    <div id="text-2" class="description">{{ $t("mail.message.starter.draft.details") }}</div>
                </div>
                <div class="starter-links">
                    <bm-button
                        size="lg"
                        icon="inbox"
                        :to="{ name: 'v:mail:home', params: { folder: MY_INBOX.path } }"
                        variant="link"
                    >
                        {{ $t("mail.message.starter.display.inbox") }}
                    </bm-button>
                    <bm-button
                        size="lg"
                        icon="sent"
                        :to="{ name: 'v:mail:home', params: { folder: MY_SENT.path } }"
                        variant="link"
                    >
                        {{ $t("mail.message.starter.display.sent") }}
                    </bm-button>
                </div>
            </div>
            <bm-illustration value="draft" size="lg" over-background />
            <active-folder-count class="after-illustration" />
        </section>
    </chain-of-responsibility>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { MY_DRAFTS, MY_INBOX, MY_SENT } from "~/getters";
import { BmButton, BmIcon, BmIllustration } from "@bluemind/ui-components";

import ChainOfResponsibility from "../../ChainOfResponsibility";
import ActiveFolderCount from "../ActiveFolderCount";

export default {
    name: "MailDraftsScreen",
    components: { ActiveFolderCount, BmButton, BmIllustration, ChainOfResponsibility },
    computed: {
        ...mapState("mail", ["activeFolder"]),
        ...mapGetters("mail", { MY_DRAFTS, MY_INBOX, MY_SENT })
    },

    priority: 128
};
</script>
