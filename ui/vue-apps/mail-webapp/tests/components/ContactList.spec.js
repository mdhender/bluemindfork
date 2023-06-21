import { mount } from "@vue/test-utils";
import ContactList from "../../src/components/MailComposer/ContactList";
import { BmSpinner, BmTable } from "@bluemind/ui-components";
describe("CONTACT LIST COMPONENT", () => {
    test("ContactList is a Vue instance ", () => {
        const wrapper = ContactListSUT();
        expect(wrapper.vm).toBeDefined();
    });
    test("should show a spinner if no data", () => {
        const wrapper = ContactListSUT();
        expect(wrapper.getComponent(BmSpinner).exists()).toBeTruthy();
    });

    test("component should be a bmTable", () => {
        const wrapper = ContactListSUT();
        expect(wrapper.getComponent(BmTable).exists()).toBeTruthy();
    });

    test("element of list can be selected", async () => {
        const checkbox = ContactListSUT({})
            .withContacts()
            .mount()
            .find("tbody>[role='row']")
            .find("input[type='checkbox']");

        expect(checkbox.exists).toBeTruthy();
        expect(checkbox.element).not.toBeChecked();

        await checkbox.trigger("click");
        expect(checkbox.element).toBeChecked();
    });

    test("should display name, email, and telephone of contacts", () => {
        const wrapper = ContactListSUT({}).withContacts().mount();

        const row = wrapper.find("tbody>[role='row']");
        const cells = row.findAll("[role='cell']");
        const name = cells.at(1).text();
        const email = cells.at(2).text();
        const tel = cells.at(3).text();
        const firstLine = `${name}, ${email}, ${tel}`;

        expect(firstLine).toBe("Jhonny, jhonny.begood@juin.com, 0678541254");
    });

    test("should display a message if an addressbook is empty", () => {
        const wrapper = ContactListSUT({}).mount();
        expect(wrapper.text()).toEqual("le carnet est vide");
    });
});

function ContactListSUT(defaultValues) {
    if (!defaultValues) {
        return mount(ContactList, {
            propsData: { loading: true, contacts: [], userId: "USER_ANY_ID", addressbook: {} }
        });
    }

    const values = { contacts: [], ...defaultValues };
    return {
        withContacts() {
            return ContactListSUT({
                ...values,
                contacts: [...values.contacts, { name: "Jhonny", email: "jhonny.begood@juin.com", tel: "0678541254" }]
            });
        },

        mount() {
            return mount(ContactList, {
                propsData: {
                    loading: values.loading ?? false,
                    contacts: values.contacts,
                    userId: values.userId ?? "USER_ANY_ID",
                    addressbook: values.addressbook ?? {}
                },
                mocks: {
                    $t: () => "le carnet est vide"
                }
            });
        }
    };
}
