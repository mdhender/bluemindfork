import { mount } from "@vue/test-utils";
import ContactList from "../../src/components/RecipientPicker/ContactList";
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

    test("click on row select an element", async () => {
        const row = ContactListSUT({}).withContacts().mount().find("tbody>[role='row']");

        expect(row.exists).toBeTruthy();
        expect(row.find("input[type='checkbox']").element).not.toBeChecked();

        await row.trigger("click");
        expect(row.find("input[type='checkbox']").element).toBeChecked();
    });

    test("Contact list can have selected contacts at component initialisation", async () => {
        const wrapper = ContactListSUT({})
            .withContacts([
                { name: "George Abitbol", email: "george@devenv.dev.bluemind.net", company: "" },
                { name: "Bertrand", email: "beber@avril.com", company: "OrangeBody" },
                { name: "Ferdinand", email: "F.1@badass.com", company: "CyanHair" },
                { name: "Jhonny", email: "jhonny.begood@juin.com", company: "BlueMind" },
                { name: "Alex", email: "laude.beer@paradox.com", company: "YellowNose" },
                { name: "Julien", email: "jul@nope.com", company: "PinkEye" }
            ])
            .withSelected([{ name: "George Abitbol" }])
            .mount();

        await wrapper.vm.$nextTick(); //Default selection require awaiting for nextRender to be visible

        expect(rowByText(wrapper, "George").find('input[type="checkbox"]').element).toBeChecked();
    });

    test("should display name, email, and company of contacts", () => {
        const wrapper = ContactListSUT({}).withContacts().mount();

        const cells = wrapper.find("tbody>[role='row']").findAll("[role='cell']");
        const name = cells.at(1).text().split(" ")[0];
        const email = cells.at(2).text();
        const company = cells.at(3).text();

        expect(`${name}, ${email}, ${company}`).toBe("Jhonny, jhonny.begood@juin.com, BlueMind");
    });

    test("should display a message if an addressbook is empty", () => {
        const wrapper = ContactListSUT({}).mount();
        expect(wrapper.text()).toEqual("le carnet FAKE ADDRESSBOOK est vide");
    });
    test("should display a message if search returns no result", () => {
        const wrapper = ContactListSUT({}).withContacts().searchingFor("George").mount();

        expect(wrapper.text()).toEqual("NO RESULT");
    });
    test("reset search can be done in empty search result page", async () => {
        const wrapper = ContactListSUT({}).withContacts().searchingFor("George").mount();
        expect(wrapper.find("button").exists()).toBeTruthy();

        await wrapper.find("button").trigger("click");
        expect(wrapper.emitted("reset-search")).toBeTruthy();
    });
});

function rowByText(wrapper, textPattern) {
    return wrapper
        .findAll("tr[role='row']")
        .filter(node => node.text().match(textPattern))
        .at(0);
}

function ContactListSUT(defaultValues) {
    if (!defaultValues) {
        return mount(ContactList, {
            propsData: { loading: true, contacts: [], userId: "USER_ANY_ID", addressbook: {}, search: "", selected: [] }
        });
    }

    const values = { contacts: [], selected: [], ...defaultValues };
    return {
        withContacts(contacts) {
            return ContactListSUT({
                ...values,
                contacts: [
                    ...values.contacts,
                    ...(contacts ?? [{ name: "Jhonny", email: "jhonny.begood@juin.com", company: "BlueMind" }])
                ]
            });
        },
        withSelected(selection) {
            if (!values.contacts.length) throw new Error("withContacts() need to be called before selection!");

            selection.forEach(s => {
                const [key, match] = Object.entries(s)[0];
                const selectedIndex = values.contacts.findIndex(c => c[key] === match);
                values.selected.push(values.contacts[selectedIndex]); // ⛔ SELECTED CONTACTS MUST BE PASSED AS REFERENCE
            });

            return ContactListSUT({
                ...values
            });
        },

        searchingFor(searchPattern) {
            return ContactListSUT({
                ...values,
                contacts: [],
                search: searchPattern
            });
        },
        mount() {
            return mount(ContactList, {
                propsData: {
                    loading: values.loading ?? false,
                    contacts: values.contacts ?? [],
                    userId: values.userId ?? "USER_ANY_ID",
                    addressbook: values.addressbook ?? { name: "FAKE ADDRESSBOOK" },
                    selected: values.selected ?? [],
                    search: values.search ?? ""
                },
                mocks: {
                    $t: (path, args) =>
                        path.includes("search") ? "NO RESULT" : " le carnet " + args.addressBookName + " est vide"
                },

                stubs: {
                    i18n: true
                }
            });
        }
    };
}
