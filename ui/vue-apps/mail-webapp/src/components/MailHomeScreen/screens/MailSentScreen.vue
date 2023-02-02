<template>
    <chain-of-responsibility :is-responsible="activeFolder === MY_SENT.key">
        <section class="mail-home-screen mail-sent-screen" aria-labelledby="text-2">
            <div class="starter-text-and-actions">
                <div class="starter-text">
                    <h1 id="text-1">{{ $t("mail.message.starter") }}</h1>
                    <div id="text-2" class="description">{{ $t("mail.message.starter.sent.details") }}</div>
                </div>
                <div class="starter-links">
                    <div class="starter-link">
                        <bm-icon icon="inbox" />
                        <bm-button :to="{ name: 'v:mail:home', params: { folder: MY_INBOX.path } }" variant="link">
                            {{ $t("mail.message.starter.display.inbox") }}
                        </bm-button>
                    </div>
                    <div class="starter-link">
                        <bm-icon icon="clock" />
                        <bm-button :to="{ name: 'v:mail:home', params: { folder: MY_OUTBOX.path } }" variant="link">
                            {{ $t("mail.message.starter.display.outbox") }}
                        </bm-button>
                    </div>
                </div>
            </div>
            <bm-illustration value="sent-filled" size="lg" over-background />
        </section>
    </chain-of-responsibility>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { MY_INBOX, MY_OUTBOX, MY_SENT } from "~/getters";
import { BmButton, BmIcon, BmIllustration } from "@bluemind/ui-components";

import ChainOfResponsibility from "../ChainOfResponsibility";

export default {
    name: "MailSentScreen",
    components: { ChainOfResponsibility, BmButton, BmIcon, BmIllustration },
    computed: {
        ...mapState("mail", ["activeFolder"]),
        ...mapGetters("mail", { MY_INBOX, MY_OUTBOX, MY_SENT })
    },

    priority: 128
};
</script>
