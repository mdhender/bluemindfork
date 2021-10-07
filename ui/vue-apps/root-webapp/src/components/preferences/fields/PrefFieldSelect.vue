<template>
    <bm-form-select v-model="localUserSettings[setting]" class="pref-field-select" :options="options.choices" />
</template>

<script>
import { BmFormSelect } from "@bluemind/styleguide";
import PrefFieldMixin from "../mixins/PrefFieldMixin";

export default {
    name: "PrefFieldSelect",
    components: {
        BmFormSelect
    },
    mixins: [PrefFieldMixin]
};
</script>
