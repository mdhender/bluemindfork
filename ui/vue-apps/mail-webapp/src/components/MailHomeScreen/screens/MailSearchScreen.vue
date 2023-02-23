<template>
    <chain-of-responsibility :is-responsible="CONVERSATION_LIST_IS_SEARCH_MODE">
        <section class="mail-home-screen mail-search-screen" aria-labelledby="text-1">
            <div class="starter-text-and-actions">
                <div class="starter-main">
                    <h1 id="text-1">{{ mainText }}</h1>
                    <bm-button variant="text" size="sm" icon="cross" @click="leaveSearch">
                        {{ $t("common.search.leave") }}
                    </bm-button>
                </div>
                <div class="starter-links"></div>
            </div>
            <bm-illustration :value="illustration" size="lg" over-background />
        </section>
    </chain-of-responsibility>
</template>

<script>
import { mapGetters } from "vuex";
import { CONVERSATION_LIST_IS_RESOLVED, CONVERSATION_LIST_IS_SEARCH_MODE, CONVERSATION_LIST_COUNT } from "~/getters";

import { BmButton, BmIllustration } from "@bluemind/ui-components";

import ChainOfResponsibility from "../ChainOfResponsibility";

export default {
    name: "MailSearchScreen",
    components: { ChainOfResponsibility, BmButton, BmIllustration },
    computed: {
        ...mapGetters("mail", {
            CONVERSATION_LIST_IS_RESOLVED,
            CONVERSATION_LIST_IS_SEARCH_MODE,
            CONVERSATION_LIST_COUNT
        }),

        illustration() {
            return this.CONVERSATION_LIST_COUNT > 0 ? "search-true" : "search-false";
        },
        mainText() {
            if (!this.CONVERSATION_LIST_IS_RESOLVED) {
                return this.$t("common.searching");
            }
            if (this.CONVERSATION_LIST_COUNT === 0) {
                return this.$t("mail.message.found_none");
            }
            return this.$tc("mail.message.found", this.CONVERSATION_LIST_COUNT, {
                count: this.CONVERSATION_LIST_COUNT
            });
        }
    },
    methods: {
        leaveSearch() {
            this.$router.navigate({ name: "v:mail:home", params: { search: null } });
        }
    },

    priority: 129
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.mail-home-screen.mail-search-screen {
    .bm-illustration {
        position: relative;
        overflow: hidden;
        height: 310px;

        & > svg {
            position: absolute;
            top: -65px;
        }
    }
}
</style>
