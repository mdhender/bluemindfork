<template>
    <bm-form-group id="terminal-group" class="pref-filter-rule-modal-terminal mt-5">
        <bm-form-checkbox v-model="filter.terminal" :value="true" :unchecked-value="false">
            {{ $t("preferences.mail.filters.modal.terminal") }}
        </bm-form-checkbox>
        <template #description>
            <div class="d-flex">
                <span class="align-self-center mr-1">
                    {{ $t("preferences.mail.filters.modal.terminal.desc") }}
                </span>
                <bm-button variant="link">
                    <small>{{ $t("preferences.mail.filters.modal.terminal.desc.link") }}</small>
                </bm-button>
            </div>
        </template>
    </bm-form-group>
</template>

<script>
import { BmButton, BmFormCheckbox, BmFormGroup } from "@bluemind/styleguide";

export default {
    name: "PrefFilterRuleModalTerminal",
    components: { BmButton, BmFormCheckbox, BmFormGroup },
    props: {
        filter: {
            type: Object,
            required: true
        }
    }
};
</script>
