<template>
    <section
        class="mail-message-starter h-100 d-none d-lg-flex"
        aria-labelledby="mail-message-starter mail-message-starter-or"
    >
        <div class="d-flex flex-column justify-content-center h-100 text-center w-100">
            <div class="d-flex flex-grow-1 flex-shrink-0 flex-column justify-space-evenly">
                <h1 id="mail-message-starter">{{ $t("mail.message.starter") }}</h1>
                <h1 id="mail-message-starter-or">{{ $t("common.or") }}</h1>
            </div>
            <div class="flex-grow-1 flex-shrink-0 overflow-hidden d-flex flex-column align-items-center">
                <div class="bg-surface py-6 d-table">
                    <div class="d-table-cell px-6">
                        <div class="pb-5">{{ $t("mail.message.starter.write") }}</div>
                        <new-message size="lg" />
                    </div>
                    <div class="d-table-cell px-6">
                        <div class="pb-5">{{ $t("mail.message.starter.display") }}</div>
                        <bm-button
                            :to="{ name: 'v:mail:home', params: { folder: MY_DRAFTS.path } }"
                            variant="fill"
                            size="lg"
                            icon="pencil"
                        >
                            {{ $t("mail.message.starter.display.drafts") }}
                        </bm-button>
                    </div>
                </div>
            </div>
            <div
                class="w-100 flex-shrink-1"
                :style="'flex-basis: 321px;background: url(' + emptyMessageIllustration + ') no-repeat center top'"
            />
        </div>
    </section>
</template>

<script>
import { BmButton } from "@bluemind/ui-components";
import emptyMessageIllustration from "../../assets/home-page.png";
import NewMessage from "./NewMessage";
import { mapGetters, mapMutations } from "vuex";
import { MY_DRAFTS } from "~/getters";
import { RESET_ACTIVE_MESSAGE, UNSET_CURRENT_CONVERSATION } from "~/mutations";

export default {
    name: "MailMessageStarter",
    components: {
        BmButton,
        NewMessage
    },
    data() {
        return { emptyMessageIllustration };
    },
    computed: {
        ...mapGetters("mail", { MY_DRAFTS })
    },
    created() {
        this.RESET_ACTIVE_MESSAGE();
        this.UNSET_CURRENT_CONVERSATION();
    },
    methods: {
        ...mapMutations("mail", { RESET_ACTIVE_MESSAGE, UNSET_CURRENT_CONVERSATION })
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.mail-message-starter h1 {
    color: $primary-fg-hi1;
    font-size: 2rem;
}

.mail-message-starter svg.hexagon polygon {
    fill: $surface-bg;
}

.mail-message-starter .flex-grow-3 {
    flex-grow: 3;
}

.mail-message-starter .justify-space-evenly {
    justify-content: space-evenly;
}
</style>
