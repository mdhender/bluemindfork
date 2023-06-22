<template>
    <div class="contact-list">
        <div class="table-wrapper scroller-y">
            <bm-table
                ref="contactTable"
                :items="contacts"
                :fields="fields"
                :busy="loading"
                :per-page="perPage"
                :current-page="currentPage"
                :fill="false"
                selectable
                selected-variant=""
                show-empty
                class="text-truncate scroller-y"
                @row-selected="onRowSelected"
            >
                <template #table-busy>
                    <div class="text-center">
                        <bm-spinner />
                    </div>
                </template>

                <template #empty>
                    <i18n path="recipientPicker.addressbook.empty" class="no-result">
                        <address-book-label-icon
                            :address-book="addressbook"
                            :user-id="userId"
                            class="font-weight-bold text-truncate"
                        />
                    </i18n>
                </template>

                <template #cell(selected)="{ rowSelected, selectRow, unselectRow }">
                    <bm-check
                        :checked="rowSelected"
                        @click.native.prevent.stop="rowSelected ? unselectRow() : selectRow()"
                    />
                </template>
            </bm-table>
        </div>
        <div class="pagination-wrapper">
            <bm-pagination v-model="currentPage" :total-rows="contacts.length" :per-page="perPage" />
        </div>
    </div>
</template>

<script>
import { BmTable, BmPagination, BmSpinner, BmCheck /* BmIllustration */ } from "@bluemind/ui-components";
import AddressBookLabelIcon from "./AddressBookLabelIcon.vue";
export default {
    name: "ContactList",
    components: { BmTable, BmPagination, BmSpinner, BmCheck /* BmIllustration */, AddressBookLabelIcon },
    props: {
        addressbook: { type: Object, default: () => ({}) },
        contacts: { type: Array, required: true },
        loading: { type: Boolean, required: true },
        selected: { type: Array, required: true },
        userId: { type: String, required: true }
    },
    data() {
        return {
            fields: [
                { key: "selected", label: "", class: "selected-cell" },
                { key: "name", label: "", class: "name-cell text-truncate" },
                { key: "email", label: "", class: "email-cell text-truncate" },
                { key: "tel", label: "", class: "tel-cell text-truncate" }
            ],
            perPage: 50,
            currentPage: 1
        };
    },
    watch: {
        selected: {
            handler: function (value) {
                const selectedUids = value.map(v => v.uid);
                this.contacts.forEach((c, index) => {
                    selectedUids.includes(c.uid)
                        ? this.$refs.contactTable.selectRow(index)
                        : this.$refs.contactTable.unselectRow(index);
                });
            },
            immediate: true
        }
    },
    methods: {
        onRowSelected(items) {
            this.$emit("update:selected", items.map(toContact));
        }
    }
};

function toContact(contactItem) {
    return { address: contactItem.email, dn: contactItem.name, uid: contactItem.uid };
}
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/variables";

.contact-list {
    $contact-height: base-px-to-rem(30);
    $pagination-padding-y: $sp-5;
    $pagination-wrapper-height: $pagination-height + (2 * $pagination-padding-y);

    display: flex;
    flex-direction: column;
    overflow: hidden; // this should not scroll but for a reason, it does on Firefox
    .bm-table {
        background-color: $surface;
        margin-bottom: 0 !important;
        td {
            height: $contact-height !important;
        }
        tbody > tr.b-table-row-selected > td {
            background-color: $secondary-bg-lo1;
        }
        tbody > tr.b-table-row-selected:hover > td {
            background-color: $secondary-bg;
        }
    }
    thead {
        display: none;
    }
    .selected-cell {
        width: base-px-to-rem(40);
    }
    .no-result {
        display: flex;
        flex-wrap: wrap;
        gap: 0 base-px-to-rem(5);
        padding: $sp-8 $sp-5 0;
        justify-content: center;
        text-align: center;
    }
    .b-table-empty-row {
        background-color: $backdrop;
        border-bottom: none !important;
    }
    .pagination-wrapper {
        padding-top: $pagination-padding-y;
        height: $pagination-wrapper-height;
    }
}
</style>
