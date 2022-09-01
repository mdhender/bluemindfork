<template>
    <div>
        <bm-table :items="tags" :fields="fields" :per-page="perPage" :current-page="currentPage" sort-by="label">
            <template #cell(color)="cell">
                <bm-icon icon="tag" size="xl" :style="'color: ' + cell.value + ';'" />
            </template>
            <template #cell(label)="cell">
                {{ cell.value }}
            </template>
            <template #cell(editable)="cell">
                <div v-if="cell.value" class="d-flex justify-content-end">
                    <bm-icon-button size="sm" icon="pencil" @click="$emit('edit', cell.item)" />
                    <bm-icon-button size="sm" icon="trash" @click="remove(cell.item)" />
                </div>
            </template>
        </bm-table>
        <bm-pagination v-model="currentPage" :total-rows="tags.length" :per-page="perPage" />
    </div>
</template>

<script>
import { BmIconButton, BmIcon, BmPagination, BmTable } from "@bluemind/styleguide";
export default {
    name: "PrefTagsTable",
    components: { BmIconButton, BmIcon, BmPagination, BmTable },
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
                    okVariant: "contained-accent",
                    cancelVariant: "text",
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
