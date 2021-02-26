<template>
    <bm-form-checkbox
        v-model="localUserSettings[setting]"
        class="pref-field-check"
        value="true"
        unchecked-value="false"
    >
        {{ options.label }}
    </bm-form-checkbox>
</template>

<script>
import { BmFormCheckbox } from "@bluemind/styleguide";
import PrefFieldMixin from "../mixins/PrefFieldMixin";

export default {
    name: "PrefFieldCheck",
    components: {
        BmFormCheckbox
    },
    mixins: [PrefFieldMixin]
};
</script>
