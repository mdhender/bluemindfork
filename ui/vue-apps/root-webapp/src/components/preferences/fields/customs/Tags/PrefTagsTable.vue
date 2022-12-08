<template>
    <div class="pref-tags-table">
        <bm-table :items="tags" :fields="fields" :per-page="perPage" :current-page="currentPage" sort-by="label">
            <template #cell(color)="cell">
                <bm-icon icon="tag" size="xl" :style="'color: ' + cell.value + ';'" />
            </template>
            <template #cell(label)="cell">
                <div class="text-truncate" :title="cell.value">
                    {{ cell.value }}
                </div>
            </template>
            <template #cell(editable)="cell">
                <div v-if="cell.value" class="actions">
                    <bm-icon-button variant="compact" icon="pencil" @click="$emit('edit', cell.item)" />
                    <bm-icon-button variant="compact" icon="trash" @click="remove(cell.item)" />
                </div>
            </template>
        </bm-table>
        <bm-pagination v-model="currentPage" :total-rows="tags.length" :per-page="perPage" />
    </div>
</template>

<script>
import { BmIconButton, BmIcon, BmPagination, BmTable } from "@bluemind/ui-components";
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
                { key: "color", label: "", class: "color-cell" },
                { key: "label", label: "", class: "label-cell" },
                { key: "editable", label: "", class: "actions-cell" }
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
                    okVariant: "fill-accent",
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

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.pref-tags-table {
    .b-table {
        max-width: base-px-to-rem(480);
        table-layout: fixed;

        thead {
            display: none;
        }
    }
    .color-cell {
        width: base-px-to-rem(90);
    }
    .label-cell {
        width: 100%;
    }
    .actions-cell {
        width: base-px-to-rem(90);
    }
    .actions-cell .actions {
        display: flex;
        justify-content: end;
        gap: $sp-5;
    }
}
</style>
