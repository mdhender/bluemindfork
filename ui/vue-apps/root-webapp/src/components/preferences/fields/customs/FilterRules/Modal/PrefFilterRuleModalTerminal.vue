<template>
    <bm-form-group id="terminal-group" class="pref-filter-rule-modal-terminal mt-5">
        <bm-form-checkbox v-model="terminal" :value="true" :unchecked-value="false">
            {{ $t("preferences.mail.filters.modal.terminal") }}
        </bm-form-checkbox>
        <bm-read-more :href="readMore" :text="$t('preferences.mail.filters.modal.terminal.desc')" />
    </bm-form-group>
</template>

<script>
import { BmFormCheckbox, BmFormGroup, BmReadMore } from "@bluemind/ui-components";

export default {
    name: "PrefFilterRuleModalTerminal",
    components: { BmFormCheckbox, BmFormGroup, BmReadMore },
    props: {
        filter: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            readMore:
                "https://doc.bluemind.net/release/5.1/guide_de_l_utilisateur/la_messagerie/appliquer_des_regles_de_tri_et_d_actions#ordonner-les-filtres-automatiques"
        };
    },
    computed: {
        terminal: {
            get() {
                return this.filter.terminal;
            },
            set(terminal) {
                this.$emit("update:filter", { ...this.filter, terminal });
            }
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-filter-rule-modal-terminal {
    .pref-read-more {
        margin-top: $sp-4;
    }
}
</style>
