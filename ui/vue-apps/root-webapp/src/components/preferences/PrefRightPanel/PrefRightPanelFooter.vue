<template>
    <div v-if="HAS_CHANGED" class="d-flex mt-auto pl-5 py-3 border-top border-secondary">
        <bm-button
            type="submit"
            variant="primary"
            :disabled="!HAS_CHANGED || HAS_ERROR || HAS_NOT_VALID"
            @click.prevent="SAVE"
        >
            {{ $t("common.save") }}
        </bm-button>
        <bm-button type="reset" variant="simple-dark" class="ml-3" :disabled="!HAS_CHANGED" @click.prevent="CANCEL">
            {{ $t("common.cancel") }}
        </bm-button>
        <div v-if="STATUS === 'error'" class="ml-5 text-danger d-flex align-items-center font-weight-bold">
            <bm-icon icon="exclamation-circle" class="mr-1" /> {{ $t("preferences.save.error") }}
        </div>
        <div v-if="STATUS === 'saved'" class="ml-5 text-success d-flex align-items-center font-weight-bold">
            <bm-icon icon="exclamation-circle" class="mr-1" /> {{ $t("preferences.save.success") }}
        </div>
    </div>
</template>

<script>
import { BmButton, BmIcon } from "@bluemind/styleguide";
import { mapActions, mapGetters } from "vuex";

export default {
    name: "PrefRightPanelFooter",
    components: { BmButton, BmIcon },
    computed: {
        ...mapGetters("preferences/fields", ["HAS_CHANGED", "HAS_ERROR", "HAS_NOT_VALID"]),
        ...mapGetters("preferences", ["STATUS"])
    },
    methods: {
        ...mapActions("preferences", ["CANCEL", "SAVE"])
    }
};
</script>
