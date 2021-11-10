<template>
    <div>
        <bm-form-group label-for="calendar-type" :label="$t('preferences.calendar.my_calendars.type')">
            <bm-form-select
                :value="value.settings.type"
                :options="possibleTypes"
                class="w-100"
                :disabled="isDefault"
                @input="onTypeChange"
            />
        </bm-form-group>
        <bm-form-group
            v-if="value.settings.type === 'externalIcs'"
            :label="$t('common.external_ics.url')"
            label-for="calendar-ics-url"
        >
            <bm-form-input
                id="calendar-ics-url"
                :value="value.settings.icsUrl"
                type="text"
                class="mb-1"
                required
                @input="onIcsUrlChange"
            />
            <bm-spinner v-if="checkUrlStatus === 'LOADING'" :size="0.2" />
            <template v-if="checkUrlStatus === 'VALID'">
                <bm-icon icon="check-circle" size="lg" class="text-success" /> {{ $t("common.valid_url") }}
            </template>
            <template v-if="checkUrlStatus === 'ERROR'">
                <bm-icon icon="exclamation-circle" size="lg" class="text-danger" /> {{ $t("common.invalid_url") }}
            </template>
        </bm-form-group>
        <bm-form-group :label="$t('common.color')" label-for="calendar-color">
            <!-- FIXME: required is an unknown prop for BmFormColorPicker ? -->
            <bm-form-color-picker
                id="calendar-color"
                type="text"
                required
                :value="value.settings.bm_color"
                :pick-default="isNew"
                @input="onColorChange"
            />
        </bm-form-group>
    </div>
</template>

<script>
import debounce from "lodash/debounce";
import { BmFormColorPicker, BmFormGroup, BmFormInput, BmFormSelect, BmIcon, BmSpinner } from "@bluemind/styleguide";

export default {
    name: "CreateOrUpdateCalendar",
    components: { BmFormColorPicker, BmFormGroup, BmFormInput, BmFormSelect, BmIcon, BmSpinner },
    props: {
        value: {
            type: Object,
            required: true
        },
        isDefault: {
            type: Boolean,
            required: true
        },
        isNew: {
            type: Boolean,
            required: true
        }
    },
    data() {
        return {
            possibleTypes: [
                { text: this.$t("common.simple"), value: "internal" },
                { text: this.$t("common.external_ics"), value: "externalIcs" }
            ],
            checkUrlStatus: "IDLE",
            checkIcsUrlValidity: debounce(url => {
                this.checkUrlStatus = "LOADING";
                try {
                    new URL(url);
                    fetch("calendar/checkIcs?url=" + url).then(res => {
                        if (res.status !== 200) {
                            this.checkUrlStatus = "ERROR";
                        } else {
                            this.checkUrlStatus = "VALID";
                        }
                    });
                } catch (e) {
                    this.checkUrlStatus = "ERROR";
                }
            }, 1000)
        };
    },
    computed: {
        isValid() {
            return this.value.settings.type !== "externalIcs" || this.checkUrlStatus === "VALID";
        }
    },
    watch: {
        isValid: {
            handler() {
                this.$emit("is-valid", this.isValid);
            },
            immediate: true
        }
    },
    created() {
        this.checkUrlStatus = this.value.settings.icsUrl ? "VALID" : "IDLE";
    },
    methods: {
        onColorChange(newColor) {
            const newContainer = { ...this.value };
            newContainer.settings.bm_color = newColor;
            this.$emit("input", newContainer);
        },
        onTypeChange(newType) {
            const newContainer = { ...this.value };
            newContainer.settings.type = newType;
            if (newType === "internal") {
                newContainer.settings.icsUrl = "";
            }
            this.$emit("input", newContainer);
        },
        onIcsUrlChange(newUrl) {
            const newContainer = { ...this.value };
            newContainer.settings.icsUrl = newUrl;
            this.$emit("input", newContainer);
            this.checkIcsUrlValidity(newUrl);
        }
    }
};
</script>
