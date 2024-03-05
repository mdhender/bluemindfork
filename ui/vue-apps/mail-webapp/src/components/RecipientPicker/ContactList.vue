<template>
    <div class="contact-list flex-fill scroller-y-stable">
        <bm-table
            ref="contactTable"
            :items="contacts"
            :selected="selected"
            :fields="fields"
            :busy="loading"
            :per-page="perPage"
            :current-page="currentPage"
            :fill="false"
            selectable
            selected-variant=""
            select-mode="range"
            show-empty
            @row-selected="$emit('selected', $event)"
        >
            <template #table-busy>
                <div class="text-center">
                    <bm-spinner />
                </div>
            </template>

            <template #empty>
                <div v-if="searchNoResult" class="empty-search-result">
                    <bm-illustration value="spider" size="sm" over-background />
                    <i18n path="recipient_picker.search.no_result">
                        <template #search>
                            <br />
                            <span class="search-pattern">"{{ search }}"</span>
                            <br />
                        </template>
                    </i18n>
                    <bm-button variant="text" size="sm" @click="$emit('reset-search')">{{
                        $t("recipient_picker.search.reset")
                    }}</bm-button>
                </div>
                <i18n v-else path="recipient_picker.addressbook.empty" class="no-result">
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
                    @keydown.native.space.prevent.stop="rowSelected ? unselectRow() : selectRow()"
                />
            </template>

            <template #cell(name)="{ value, item }">
                <div v-highlight="search" class="name desktop-only d-flex align-items-center position-relative">
                    <span class="text-truncate text-nowrap position-absolute w-100">{{ value }}</span>
                </div>
                <div v-highlight="search" class="mobile-name-and-email mobile-only">
                    <div class="name d-flex align-items-center position-relative h-50">
                        <span class="text-truncate text-nowrap position-absolute w-100">{{ value }}</span>
                    </div>
                    <div class="email d-flex align-items-center position-relative h-50">
                        <span class="text-truncate text-nowrap position-absolute w-100">{{ item.email }}</span>
                    </div>
                </div>
            </template>

            <template #cell(email)="{ value }">
                <div v-highlight="search" class="email d-flex align-items-center position-relative">
                    <span class="text-truncate text-nowrap position-absolute w-100">{{ value }}</span>
                </div>
            </template>

            <template #cell(company)="{ value }">
                <div v-highlight="search" class="d-flex align-items-center position-relative">
                    <span class="text-truncate text-nowrap position-absolute w-100">{{ value }}</span>
                </div>
            </template>
        </bm-table>
        <bm-pagination
            ref="pagination"
            :total-rows="contacts.length"
            :per-page="perPage"
            :value="currentPage"
            @input="$emit('page-changed', $event)"
        />
    </div>
</template>

<script>
import {
    BmPagination,
    BmSpinner,
    BmTable,
    BmCheck,
    BmIllustration,
    BmButton,
    Highlight
} from "@bluemind/ui-components";
import AddressBookLabelIcon from "./AddressBookLabelIcon.vue";

export default {
    name: "ContactList",
    components: { BmTable, BmPagination, BmSpinner, BmCheck, BmIllustration, BmButton, AddressBookLabelIcon },
    directives: { Highlight },
    props: {
        addressbook: { type: Object, required: true },
        contacts: { type: Array, required: true },
        loading: { type: Boolean, required: true },
        selected: { type: Array, required: true },
        userId: { type: String, required: true },
        search: { type: String, default: "" },
        perPage: { type: Number, default: 50 },
        currentPage: { type: Number, default: 1 }
    },
    data() {
        return {
            fields: [
                { key: "selected", label: "", class: "selected-cell" },
                { key: "name", label: "", class: "name-cell" },
                { key: "email", label: "", class: "email-cell" },
                { key: "company", label: "", class: "company-cell" }
            ]
        };
    },

    computed: {
        searchNoResult() {
            return Boolean(this.search) && !this.loading && !this.contacts.length;
        }
    },
    watch: {
        currentPage() {
            document.querySelector(".contact-list").scrollTo({ top: 0 });
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/responsiveness";
@import "@bluemind/ui-components/src/css/utils/typography";
@import "@bluemind/ui-components/src/css/utils/variables";

.contact-list {
    $contact-height: base-px-to-rem(30);
    $pagination-padding-y: $sp-5;

    display: flex;
    flex-direction: column;
    overflow: scroll;
    .bm-table {
        background-color: $surface;
        margin-bottom: 0 !important;
        tr {
            border-bottom: 1px solid $neutral-fg-lo3 !important;
        }
        td {
            height: $contact-height !important;
            span {
                line-height: $line-height-sm;
            }
        }
        tbody > tr.b-table-row-selected > td {
            background-color: $secondary-bg-lo1;
        }
        tbody > tr.b-table-row-selected:hover > td {
            background-color: $secondary-bg;
        }
        thead {
            display: none;
        }
        .no-result {
            display: flex;
            flex-wrap: wrap;
            gap: 0 base-px-to-rem(5);
            padding: $sp-8 $sp-5 0;
            justify-content: center;
            text-align: center;
            @include regular-high;
            border-bottom: none !important;
        }
        .bm-spinner {
            padding-top: $sp-8;
        }
    }
    .selected-cell {
        width: base-px-to-rem(40);
    }
    .name-cell {
        .name {
            color: $neutral-fg-hi1;
        }
        .email {
            color: $neutral-fg-lo1;
        }
        .mobile-name-and-email {
            height: base-px-to-rem(34);
            margin: base-px-to-rem(4) 0 base-px-to-rem(5);
            .email {
                @include caption;
            }
        }
    }
    .email-cell {
        @include until-lg {
            display: none;
        }
    }
    .company-cell {
        width: 15%;
        @include until-lg {
            display: none;
        }
    }

    tr.b-table-empty-row,
    tr.b-table-busy-slot {
        background-color: $backdrop;
        border-bottom: none !important;
    }
    .bm-pagination {
        padding: $pagination-padding-y 0 $pagination-padding-y $sp-5;
        margin: 0;
        background-color: $surface-bg;
    }

    .empty-search-result {
        display: flex;
        flex-direction: column;
        align-items: center;
        text-align: center;
        padding-top: $sp-7;
        gap: $sp-6;

        .search-pattern {
            color: $primary-fg;
            @include bold;
            word-break: break-all;
        }

        .bm-illustration {
            max-width: 160px;
        }
    }
    .no-result,
    .empty-search-result {
        cursor: default;
    }
}
</style>
