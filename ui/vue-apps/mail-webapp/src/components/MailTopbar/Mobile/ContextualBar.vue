<template>
    <div class="contextual-bar d-flex align-items-center">
        <bm-icon-button
            variant="compact-on-fill-primary"
            size="lg"
            class="d-lg-none"
            icon="arrow-back"
            @click="back()"
        />
        <div class="slot-wrapper">
            <slot />
        </div>
    </div>
</template>

<script>
import { mapMutations } from "vuex";
import { BmIconButton } from "@bluemind/ui-components";
import { UNSELECT_ALL_CONVERSATIONS, UNSET_CURRENT_CONVERSATION } from "~/mutations";

export default {
    name: "ContextualBar",
    components: {
        BmIconButton
    },
    methods: {
        ...mapMutations("mail", { UNSELECT_ALL_CONVERSATIONS, UNSET_CURRENT_CONVERSATION }),
        back() {
            this.$emit("back");
            if (this.SELECTION_IS_EMPTY) {
                this.UNSET_CURRENT_CONVERSATION();
            } else {
                this.UNSELECT_ALL_CONVERSATIONS();
            }
            this.$router.navigate({ name: "v:mail:home", params: { search: null } });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.contextual-bar {
    & > .bm-icon-button {
        border-right: $input-border-width solid $fill-primary-fg-lo2;
    }
    & > .slot-wrapper {
        padding-left: $sp-5;
    }
}
</style>
