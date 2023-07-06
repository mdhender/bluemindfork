<template>
    <div class="contact-list">
        <div class="overflow-auto">
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
                show-empty
                class="scroller-y"
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
                    />
                </template>

                <template #cell(name)="{ value, item }">
                    <div class="name d-none d-lg-flex align-items-center position-relative">
                        <span class="text-truncate text-nowrap position-absolute w-100">{{ value }}</span>
                    </div>
                    <div class="mobile-name-and-email d-lg-none">
                        <div class="name d-flex align-items-center position-relative h-50">
                            <span class="text-truncate text-nowrap position-absolute w-100">{{ value }}</span>
                        </div>
                        <div class="email d-flex align-items-center position-relative h-50">
                            <span class="text-truncate text-nowrap position-absolute w-100">{{ item.email }}</span>
                        </div>
                    </div>
                </template>

                <template #cell(email)="{ value }">
                    <div class="email d-flex align-items-center position-relative">
                        <span class="text-truncate text-nowrap position-absolute w-100">{{ value }}</span>
                    </div>
                </template>

                <template #cell(tel)="{ value }">
                    <div class="d-flex align-items-center position-relative">
                        <span class="text-truncate text-nowrap position-absolute w-100">{{ value }}</span>
                    </div>
                </template>
            </bm-table>
        </div>
        <bm-pagination v-model="currentPage" :total-rows="contacts.length" :per-page="perPage" />
    </div>
</template>

<script>
import { BmPagination, BmSpinner, BmTable, BmCheck, BmIllustration, BmButton } from "@bluemind/ui-components";
import AddressBookLabelIcon from "./AddressBookLabelIcon.vue";

export default {
    name: "ContactList",
    components: { BmTable, BmPagination, BmSpinner, BmCheck, BmIllustration, BmButton, AddressBookLabelIcon },
    props: {
        addressbook: { type: Object, required: true },
        contacts: { type: Array, required: true },
        loading: { type: Boolean, required: true },
        selected: { type: Array, required: true },
        userId: { type: String, required: true },
        search: { type: String, default: "" }
    },
    data() {
        return {
            fields: [
                { key: "selected", label: "", class: "selected-cell" },
                { key: "name", label: "", class: "name-cell" },
                { key: "email", label: "", class: "email-cell" },
                { key: "tel", label: "", class: "tel-cell" }
            ],
            perPage: 50,
            currentPage: 1
        };
    },
    computed: {
        searchNoResult() {
            return Boolean(this.search) && !this.loading && !this.contacts.length;
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
    $pagination-wrapper-height: $pagination-height + (2 * $pagination-padding-y);

    display: flex;
    flex-direction: column;
    overflow: hidden; // this should not scroll but for a reason, it does on Firefox
    .bm-table {
        background-color: $surface;
        margin-bottom: 0 !important;
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
    }
    thead {
        display: none;
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
    .tel-cell {
        width: 15%;
        @include until-lg {
            display: none;
        }
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
    .bm-pagination {
        padding: $pagination-padding-y 0 $pagination-padding-y $sp-5;
        // height: $pagination-wrapper-height; // TODO: Do we still need it ?
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
