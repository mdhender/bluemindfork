<template>
    <div v-if="HAS_CHANGED" class="pref-right-panel-footer">
        <bm-button type="reset" variant="text" :disabled="!HAS_CHANGED" @click.prevent="CANCEL">
            {{ $t("common.cancel") }}
        </bm-button>
        <bm-button
            type="submit"
            variant="fill-accent"
            :disabled="!HAS_CHANGED || HAS_ERROR || HAS_NOT_VALID"
            @click.prevent="save"
        >
            {{ $t("common.save") }}
        </bm-button>
    </div>
</template>

<script>
import { BmButton } from "@bluemind/ui-components";
import { mapActions, mapGetters } from "vuex";

export default {
    name: "PrefRightPanelFooter",
    components: { BmButton },
    computed: {
        ...mapGetters("preferences/fields", ["HAS_CHANGED", "HAS_ERROR", "HAS_NOT_VALID"])
    },
    methods: {
        ...mapActions("preferences", ["CANCEL", "SAVE"]),
        save() {
            this.SAVE().then(() => this.$emit("saved"));
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-right-panel-footer {
    background-color: $surface-hi1;
    box-shadow: $box-shadow-sm;
    padding: $sp-4;
    @include from-lg {
        padding: $sp-5 $sp-6;
    }

    display: flex;
    justify-content: flex-end;
    gap: $sp-5 $sp-7;
}
</style>
