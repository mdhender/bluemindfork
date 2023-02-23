<template>
    <chain-of-responsibility :is-responsible="activeFolder === MY_DRAFTS.key">
        <section class="mail-home-screen mail-drafts-screen" aria-labelledby="text-2">
            <div class="starter-text-and-actions">
                <div class="starter-main">
                    <h1 id="text-1">{{ $t("mail.message.starter") }}</h1>
                    <div id="text-2" class="description">{{ $t("mail.message.starter.draft.details") }}</div>
                </div>
                <div class="starter-links">
                    <div class="starter-link">
                        <bm-icon icon="inbox" />
                        <bm-button :to="{ name: 'v:mail:home', params: { folder: MY_INBOX.path } }" variant="link">
                            {{ $t("mail.message.starter.display.inbox") }}
                        </bm-button>
                    </div>
                    <div class="starter-link">
                        <bm-icon icon="paper-plane" />
                        <bm-button :to="{ name: 'v:mail:home', params: { folder: MY_SENT.path } }" variant="link">
                            {{ $t("mail.message.starter.display.sent") }}
                        </bm-button>
                    </div>
                </div>
            </div>
            <bm-illustration value="draft" size="lg" over-background />
        </section>
    </chain-of-responsibility>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { MY_DRAFTS, MY_INBOX, MY_SENT } from "~/getters";
import { BmButton, BmIcon, BmIllustration } from "@bluemind/ui-components";

import ChainOfResponsibility from "../ChainOfResponsibility";

export default {
    name: "MailDraftsScreen",
    components: { ChainOfResponsibility, BmButton, BmIcon, BmIllustration },
    computed: {
        ...mapState("mail", ["activeFolder"]),
        ...mapGetters("mail", { MY_DRAFTS, MY_INBOX, MY_SENT })
    },

    priority: 128
};
</script>
