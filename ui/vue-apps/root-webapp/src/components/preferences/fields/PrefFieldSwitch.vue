<template>
    <div class="pref-field-switch">
        <div>
            <bm-form-checkbox
                v-model="localUserSettings[setting]"
                class="d-inline-block"
                :value="options.true.value"
                :unchecked-value="options.false.value"
                switch
            />
            {{ options[currentBooleanValue].name }}
        </div>
        <div class="d-flex">
            <img class="pt-3" :src="options[currentBooleanValue].image" alt="" />
            <!-- eslint-disable-next-line vue/no-v-html -->
            <span class="text-secondary pl-5 pt-3" v-html="options[currentBooleanValue].desc" />
        </div>
    </div>
</template>

<script>
import { BmFormCheckbox } from "@bluemind/styleguide";
import PrefFieldMixin from "../mixins/PrefFieldMixin";

export default {
    name: "PrefFieldSwitch",
    components: { BmFormCheckbox },
    mixins: [PrefFieldMixin],
    computed: {
        currentBooleanValue() {
            return this.options.true.value === this.localUserSettings[this.setting];
        }
    }
};
</script>
