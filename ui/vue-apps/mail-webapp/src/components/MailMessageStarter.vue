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
                <div class="bg-white py-3 d-table">
                    <div class="d-table-cell px-4">
                        <div class="pb-2">{{ $t("mail.message.starter.write") }}</div>
                        <new-message />
                    </div>
                    <div class="d-table-cell px-4">
                        <div class="pb-2">{{ $t("mail.message.starter.display") }}</div>
                        <bm-button
                            :to="{ name: 'v:mail:home', params: { folder: MY_DRAFTS.path } }"
                            variant="secondary"
                        >
                            <bm-label-icon icon="pencil">{{ $t("mail.message.starter.display.drafts") }}</bm-label-icon>
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
import { BmButton, BmLabelIcon } from "@bluemind/styleguide";
import emptyMessageIllustration from "../../assets/home-page.png";
import NewMessage from "./NewMessage";
import { mapGetters } from "vuex";
import { MY_DRAFTS } from "~getters";

export default {
    name: "MailMessageStarter",
    components: {
        BmButton,
        BmLabelIcon,
        NewMessage
    },
    data() {
        return {
            emptyMessageIllustration
        };
    },
    computed: {
        ...mapGetters("mail", { MY_DRAFTS })
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.mail-message-starter {
    font-size: $font-size-lg;
}

.mail-message-starter h1 {
    color: $info-dark;
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
