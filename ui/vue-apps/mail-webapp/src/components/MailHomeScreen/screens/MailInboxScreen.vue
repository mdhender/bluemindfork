<template>
    <chain-of-responsibility :is-responsible="activeFolder === MY_INBOX.key">
        <section class="mail-home-screen mail-inbox-screen" aria-labelledby="text-1 text-2">
            <div class="starter-text-and-actions">
                <div class="starter-main">
                    <h1 id="text-1">{{ $t("mail.message.starter") }}</h1>
                    <div id="text-2">{{ $t("common.or") }}</div>
                    <new-message full />
                </div>
                <div class="starter-links">
                    <bm-button
                        size="lg"
                        icon="documents"
                        :to="{ name: 'v:mail:home', params: { folder: MY_TEMPLATES.path } }"
                        variant="link"
                    >
                        {{ $t("mail.message.starter.display.templates") }}
                    </bm-button>
                    <bm-button
                        size="lg"
                        icon="pencil"
                        :to="{ name: 'v:mail:home', params: { folder: MY_DRAFTS.path } }"
                        variant="link"
                    >
                        {{ $t("mail.message.starter.display.drafts") }}
                    </bm-button>
                </div>
            </div>
            <bm-illustration value="inbox" size="lg" over-background />
            <active-folder-count class="after-illustration" />
        </section>
    </chain-of-responsibility>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { MY_INBOX, MY_DRAFTS, MY_TEMPLATES } from "~/getters";
import { BmButton, BmIcon, BmIllustration } from "@bluemind/ui-components";

import NewMessage from "~/components/NewMessage";
import ChainOfResponsibility from "../../ChainOfResponsibility";
import ActiveFolderCount from "../ActiveFolderCount";

export default {
    name: "MailInboxScreen",
    components: { ActiveFolderCount, BmButton, BmIllustration, ChainOfResponsibility, NewMessage },
    computed: {
        ...mapState("mail", ["activeFolder"]),
        ...mapGetters("mail", { MY_INBOX, MY_DRAFTS, MY_TEMPLATES })
    },

    priority: 128
};
</script>
