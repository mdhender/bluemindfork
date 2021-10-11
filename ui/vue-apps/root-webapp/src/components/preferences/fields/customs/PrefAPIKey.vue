<template>
    <div class="pref-api-key">
        <div class="mb-2">{{ $t("preferences.security.api_key.desc") }}</div>
        <bm-button variant="outline-secondary" @click="openModal">
            <bm-icon icon="plus" />
            {{ $t("preferences.security.api_key.generate") }}</bm-button
        >

        <bm-modal
            v-model="showModal"
            :title="$t('preferences.security.api_key.generate')"
            :ok-title="$t('preferences.security.api_key.generate.modal.ok_button')"
            :ok-disabled="!projectLabelValid"
            :cancel-title="$t('common.cancel')"
            centered
            @ok="generateAPIKey"
        >
            <bm-form class="mt-4">
                <bm-form-group
                    :label="$t('preferences.security.api_key.generate.modal.project_label')"
                    label-for="project-label"
                >
                    <bm-form-input
                        id="project-label"
                        ref="project-label-input"
                        v-model="projectLabel"
                        autofocus
                        @keypress.prevent.enter="generateAndClose"
                    />
                </bm-form-group>
            </bm-form>
        </bm-modal>

        <div class="pt-2">
            <bm-list-group>
                <bm-list-group-item
                    v-for="(key, index) in keys"
                    :key="key.sid"
                    class="row d-flex align-items-center p-0"
                    :class="{ 'border-top': index === 0 }"
                >
                    <bm-col cols="1" class="d-flex justify-content-center">
                        <bm-icon class="text-primary" icon="key" size="lg" />
                    </bm-col>
                    <bm-col cols="3">
                        {{ key.displayName }}
                    </bm-col>
                    <bm-col cols="5" class="d-flex justify-content-center bg-extra-light align-self-stretch"
                        ><div class="align-self-center">{{ key.sid }}</div>
                    </bm-col>
                    <bm-col cols="2" class="d-flex justify-content-center bg-extra-light align-self-stretch">
                        <bm-button
                            class="align-self-center"
                            :variant="lastCopiedKeyIndex === index ? 'success' : 'outline-secondary'"
                            @click="copyKey(index)"
                        >
                            <bm-icon :icon="lastCopiedKeyIndex === index ? 'check' : 'copy'" />
                            <span class="pl-1">{{
                                lastCopiedKeyIndex === index ? $t("common.copied") : $t("common.copy")
                            }}</span>
                        </bm-button>
                    </bm-col>
                    <bm-col cols="1" class="d-flex justify-content-center">
                        <bm-button variant="inline-secondary" @click="remove(index)">
                            <bm-icon icon="trash" size="lg" />
                        </bm-button>
                    </bm-col>
                </bm-list-group-item>
            </bm-list-group>
        </div>
    </div>
</template>

<script>
import {
    BmButton,
    BmCol,
    BmForm,
    BmFormGroup,
    BmFormInput,
    BmIcon,
    BmListGroup,
    BmListGroupItem,
    BmModal
} from "@bluemind/styleguide";
import { inject } from "@bluemind/inject";

export default {
    name: "PrefAPIKey",
    components: { BmButton, BmCol, BmForm, BmFormGroup, BmFormInput, BmIcon, BmListGroup, BmListGroupItem, BmModal },
    data() {
        return {
            showModal: false,
            projectLabel: "",
            keys: [],
            lastCopiedKeyIndex: -1
        };
    },
    computed: {
        projectLabelValid() {
            return this.projectLabel.trim().length > 0;
        }
    },
    async created() {
        this.keys = await inject("APIKeysPersistence").list();
    },
    methods: {
        async generateAPIKey() {
            const key = await inject("APIKeysPersistence").create(this.projectLabel);
            this.keys.push(key);
        },
        generateAndClose() {
            if (this.projectLabelValid) {
                this.generateAPIKey();
                this.showModal = false;
            }
        },
        async remove(index) {
            const key = this.keys[index];
            await inject("APIKeysPersistence").remove(key.sid);
            this.keys.splice(index, 1);
            if (index === this.lastCopiedKeyIndex) {
                this.lastCopiedKeyIndex = -1;
            } else if (index < this.lastCopiedKeyIndex) {
                this.lastCopiedKeyIndex--;
            }
        },
        copyKey(index) {
            navigator.clipboard.writeText(this.keys[index].sid);
            this.lastCopiedKeyIndex = index;
        },
        openModal() {
            this.projectLabel = "";
            this.showModal = true;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-api-key {
    .list-group-item {
        border-bottom: 1px solid $light !important;
        &.border-top {
            border-top: 1px solid $light !important;
        }
        min-height: 3em;
    }
}
</style>
