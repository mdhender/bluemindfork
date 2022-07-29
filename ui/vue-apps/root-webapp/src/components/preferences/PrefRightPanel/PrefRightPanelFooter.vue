<template>
    <div v-if="HAS_CHANGED" class="d-flex mt-auto pl-5 py-3 border-top border-neutral">
        <bm-button
            type="submit"
            variant="secondary"
            :disabled="!HAS_CHANGED || HAS_ERROR || HAS_NOT_VALID"
            @click.prevent="save"
        >
            {{ $t("common.save") }}
        </bm-button>
        <bm-button type="reset" variant="simple-neutral" class="ml-3" :disabled="!HAS_CHANGED" @click.prevent="CANCEL">
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
