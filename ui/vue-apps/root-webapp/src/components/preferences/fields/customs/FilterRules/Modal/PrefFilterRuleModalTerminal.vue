<template>
    <bm-form-group id="terminal-group" class="pref-filter-rule-modal-terminal mt-5">
        <bm-form-checkbox v-model="filter.terminal" :value="true" :unchecked-value="false">
            {{ $t("preferences.mail.filters.modal.terminal") }}
        </bm-form-checkbox>
        <pref-read-more-label :href="readMore" :text="$t('preferences.mail.filters.modal.terminal.desc')" />
    </bm-form-group>
</template>

<script>
import { BmFormCheckbox, BmFormGroup } from "@bluemind/styleguide";
import PrefReadMoreLabel from "../../../PrefReadMoreLabel";

export default {
    name: "PrefFilterRuleModalTerminal",
    components: { BmFormCheckbox, BmFormGroup, PrefReadMoreLabel },
    props: {
        filter: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            readMore:
                "https://doc.bluemind.net/master/Guide_de_l_utilisateur/La_messagerie/Appliquer_des_regles_de_tri_et_d_actions#ordonner-les-filtres-automatiques"
        };
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-filter-rule-modal-terminal {
    .pref-read-more-label {
        color: $neutral-fg;
    }
}
</style>
