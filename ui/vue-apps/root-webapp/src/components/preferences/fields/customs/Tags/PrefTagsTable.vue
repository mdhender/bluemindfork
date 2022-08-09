<template>
    <div>
        <bm-table :items="tags" :fields="fields" :per-page="perPage" :current-page="currentPage" sort-by="label">
            <template #cell(color)="cell">
                <bm-icon icon="tag" size="2x" :style="'color: ' + cell.value + ';'" />
            </template>
            <template #cell(label)="cell">
                {{ cell.value }}
            </template>
            <template #cell(editable)="cell">
                <div v-if="cell.value" class="d-flex justify-content-end">
                    <bm-button variant="inline-neutral" @click="$emit('edit', cell.item)">
                        <bm-icon icon="pencil" size="lg" />
                    </bm-button>
                    <bm-button variant="inline-neutral" @click="remove(cell.item)">
                        <bm-icon icon="trash" size="lg" />
                    </bm-button>
                </div>
            </template>
        </bm-table>
        <bm-pagination v-model="currentPage" :total-rows="tags.length" :per-page="perPage" />
    </div>
</template>

<script>
import { BmButton, BmIcon, BmPagination, BmTable } from "@bluemind/styleguide";
export default {
    name: "PrefTagsTable",
    components: { BmButton, BmIcon, BmPagination, BmTable },
    props: {
        tags: {
            type: Array,
            required: true
        },
        perPage: {
            type: Number,
            default: 5
        }
    },
    data() {
        return {
            currentPage: 1,
            fields: [
                { key: "color", label: "" },
                { key: "label", label: "", class: "align-middle" },
                { key: "editable", label: "" }
            ]
        };
    },
    methods: {
        async remove(item) {
            const confirm = await this.$bvModal.msgBoxConfirm(
                this.$t("preferences.general.tags.remove.desc", { name: item.label }),
                {
                    title: this.$t("preferences.general.tags.remove", { name: item.label }),
                    okTitle: this.$t("common.delete"),
                    cancelTitle: this.$t("common.cancel"),
                    okVariant: "secondary",
                    cancelVariant: "simple-neutral",
                    centered: true,
                    hideHeaderClose: false,
                    autoFocusButton: "ok"
                }
            );

            if (confirm) {
                this.$emit("remove", item);
            }
        }
    }
};
</script>
