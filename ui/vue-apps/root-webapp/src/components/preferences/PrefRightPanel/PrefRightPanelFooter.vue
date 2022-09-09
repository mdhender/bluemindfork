<template>
    <div v-if="HAS_CHANGED" class="pref-right-panel-footer modal-footer">
        <bm-button
            type="submit"
            variant="contained-accent"
            :disabled="!HAS_CHANGED || HAS_ERROR || HAS_NOT_VALID"
            @click.prevent="save"
        >
            {{ $t("common.save") }}
        </bm-button>
        <bm-button type="reset" variant="text" class="ml-3" :disabled="!HAS_CHANGED" @click.prevent="CANCEL">
            {{ $t("common.cancel") }}
        </bm-button>
    </div>
</template>

<script>
import { BmButton } from "@bluemind/styleguide";
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
@import "~@bluemind/styleguide/css/mixins/_responsiveness";
@import "~@bluemind/styleguide/css/_variables";
@import "../_variables";

.pref-right-panel-footer.modal-footer {
    justify-content: flex-start;
    border-top: 1px solid $neutral-fg !important;
    padding-left: $prefs-padding-left;
    @include from-lg {
        padding-left: $prefs-padding-left-lg;
    }
}
</style>
