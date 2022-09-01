<template>
    <bm-form-group
        id="name-group"
        class="pref-filter-rule-modal-name"
        :label="$t('preferences.mail.filters.modal.name')"
        label-for="pref-filter-rule-modal-name-input"
        label-class="circled-number one d-flex align-items-center"
    >
        <bm-form-input
            id="pref-filter-rule-modal-name-input"
            ref="name-input"
            v-model="filter.name"
            :placeholder="$t('preferences.mail.filters.modal.name.placeholder')"
            class="col-5 pl-0"
            required
            @keypress.enter.prevent="$emit('submit')"
        />
    </bm-form-group>
</template>

<script>
import { BmFormGroup, BmFormInput } from "@bluemind/styleguide";
export default {
    name: "PrefFilterRuleModalName",
    components: { BmFormGroup, BmFormInput },
    props: {
        filter: {
            type: Object,
            required: true
        }
    }
};
</script>
