<template>
    <chain-of-responsibility :is-responsible="activeFolder === MY_TEMPLATES.key">
        <section class="mail-home-screen mail-templates-screen" aria-labelledby="text-1 text-2 text-3">
            <div class="starter-text-and-actions">
                <div class="starter-main">
                    <h1 id="text-1">{{ $t("mail.message.starter.templates") }}</h1>
                    <div id="text-2">{{ $t("mail.message.starter.templates.details") }}</div>
                    <div id="text-3">{{ $t("common.or") }}</div>
                    <new-message template full />
                </div>
                <div class="starter-links">
                    <div class="starter-link">
                        <bm-icon icon="inbox" />
                        <bm-button :to="{ name: 'v:mail:home', params: { folder: MY_INBOX.path } }" variant="link">
                            {{ $t("mail.message.starter.display.inbox") }}
                        </bm-button>
                    </div>
                    <div class="starter-link">
                        <bm-icon icon="pencil" />
                        <bm-button :to="{ name: 'v:mail:home', params: { folder: MY_DRAFTS.path } }" variant="link">
                            {{ $t("mail.message.starter.display.drafts") }}
                        </bm-button>
                    </div>
                </div>
            </div>
            <bm-illustration value="templates" size="lg" over-background />
        </section>
    </chain-of-responsibility>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { MY_INBOX, MY_DRAFTS, MY_TEMPLATES } from "~/getters";
import { BmButton, BmIcon, BmIllustration } from "@bluemind/ui-components";

import NewMessage from "~/components/NewMessage";
import ChainOfResponsibility from "../ChainOfResponsibility";

export default {
    name: "MailTemplatesScreen",
    components: { ChainOfResponsibility, BmButton, BmIcon, BmIllustration, NewMessage },
    computed: {
        ...mapState("mail", ["activeFolder"]),
        ...mapGetters("mail", { MY_INBOX, MY_DRAFTS, MY_TEMPLATES })
    },

    priority: 128
};
</script>
