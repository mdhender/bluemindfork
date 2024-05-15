import { mount } from "@vue/test-utils";
import AddressBookList from "../../src/components/RecipientPicker/AddressBookList";

describe("Addressbooks list [RECIPIENT PICKER]", () => {
    it("should be a vue component instance", () => {
        const wrapper = AddressbookListSUT().mount();
        expect(wrapper.exists()).toBeTruthy();
        expect(wrapper).toBeDefined();
        expect(wrapper.vm).toBeDefined();
    });

    it("should render name of each addressbook", () => {
        const wrapper = AddressbookListSUT().withDefaultsContainers().mount();

        expect(wrapper.text()).toContain("Mes contacts", "Contacts collectés", "Collectés par George Abitbol");
    });

    it("Annuaire should be selected by default", async () => {
        const wrapper = AddressbookListSUT().withDefaultsContainers().mount();

        // Addressbooklist does not update selection himself but emit an event (parent should handle the change)
        await wrapper.setProps({
            selectedAddressbook: wrapper.emitted("selected")[0][0]
        });
        expect(wrapper.find(".active").text()).toEqual("Mes contacts");
    });

    it("shared addressbooks should contain the name of the owner in addition of addressbook type", () => {
        const spy = jest.fn((path, args) => path.split(".").pop() + " " + args.user);
        const wrapper = AddressbookListSUT()
            .withSharedCollectedAddressbook()
            .withSharedDefaultPersonnalAddressbook()
            .withSharedOtherAddressbook()
            .mount(spy);
        expect(spy).toHaveBeenCalledTimes(3);
        expect(wrapper.text()).toContain("collected George Abitbol");
    });

    describe("SVG Illustration by type of addressbook", () => {
        describe("Owned addressBooks", () => {
            test("Directory addressbook", () => {
                const wrapper = AddressbookListSUT()._withDirectoryAdressbook().mount();
                expect(wrapper.find("svg").attributes("data-test-id")).toEqual("buildings");
            });
            test("Collected addressbook", () => {
                const wrapper = AddressbookListSUT()._withCollectedContacts().mount();
                expect(wrapper.find("svg").attributes("data-test-id")).toEqual("user-mail");
            });
            test("Personnal addressbook", () => {
                const wrapper = AddressbookListSUT()._withAddressbooks().mount();
                expect(wrapper.find("svg").attributes("data-test-id")).toEqual("user");
            });
        });

        describe("Shared addressboooks", () => {
            it("shared collected addressbook", () => {
                const wrapper = AddressbookListSUT().withSharedCollectedAddressbook().mount();
                expect(wrapper.find("svg").attributes("data-test-id")).toEqual("user-mail-shared");
            });
            it("shared personnal addressbook", () => {
                const wrapper = AddressbookListSUT().withSharedOtherAddressbook().mount();
                expect(wrapper.find("svg").attributes("data-test-id")).toEqual("user-shared");
            });

            test("others Addressbook", () => {
                const wrapper = AddressbookListSUT().withSharedOtherAddressbook().mount();
                expect(wrapper.find("svg").attributes("data-test-id")).toEqual("user-shared");
            });
        });
    });

    describe("Sorting order of addressbooks list", () => {
        it("owned addressbooks should be ordered by type", () => {
            const wrapper = AddressbookListSUT().withDefaultsContainers().mount();

            expect(wrapper.findAll("[role='listitem']").at(0).text()).toEqual("Annuaire");
            expect(wrapper.findAll("[role='listitem']").at(1).text()).toEqual("Mes contacts");
            expect(wrapper.findAll("[role='listitem']").at(2).text()).toEqual("Contacts collectés");
        });

        it("shared addressbooks should be ordered after those owned by currentUser", () => {
            let spy = jest.fn(() => "George Abitbol");
            const wrapper = AddressbookListSUT()
                .withDefaultsContainers()
                .withSharedCollectedAddressbook()
                .withSharedOtherAddressbook()
                .mount(spy);

            const listItems = wrapper.findAll("[role='listitem']");
            expect(spy).toHaveBeenCalledTimes(2);
            expect(listItems.at(3).text()).toContain("George Abitbol");
            expect(listItems.at(4).text()).toContain("George Abitbol");
        });
    });
    it("should emit selected item id when clicked", async () => {
        const wrapper = AddressbookListSUT().withDefaultsContainers().mount();
        wrapper.findAll("[role='listitem']").at(0).trigger("click");

        expect(wrapper.emitted("selected").pop()).toContain("addressbook_75a0d5b3.internal");
    });

    describe("Keyboard Navigation", () => {
        it("should go to next list item when arrowDown is used", () => {
            const wrapper = AddressbookListSUT().withDefaultsContainers().mount();

            wrapper.find('[role="listitem"]').trigger("focus");
            expect(wrapper.find('[role="listitem"]').element).toHaveFocus();

            wrapper.trigger("keydown", { key: "Down" });
            expect(wrapper.findAll("[role='listitem']").at(1).element).toHaveFocus();
        });
        it("should go to previous list item when arrowUp is used", () => {
            const wrapper = AddressbookListSUT().withDefaultsContainers().mount();

            wrapper.findAll('[role="listitem"]').at(1).trigger("focus");
            expect(wrapper.findAll('[role="listitem"]').at(1).element).toHaveFocus();

            wrapper.trigger("keydown", { key: "Up" });
            expect(wrapper.findAll("[role='listitem']").at(0).element).toHaveFocus();
        });

        it("should not do anything when pressing ArrowDown if focus is on last browsable element ", () => {
            const wrapper = AddressbookListSUT().withDefaultsContainers().mount();

            wrapper.findAll('[role="listitem"]').at(2).trigger("focus");
            expect(wrapper.findAll('[role="listitem"]').at(2).element).toHaveFocus();

            wrapper.trigger("keydown.down");
            expect(wrapper.findAll("[role='listitem']").at(2).element).toHaveFocus();
        });
        it("should not do anything when pressing ArrowUp if focus is on first browsable element ", () => {
            const wrapper = AddressbookListSUT().withDefaultsContainers().mount();

            wrapper.find('[role="listitem"]').trigger("focus");
            expect(wrapper.find('[role="listitem"]').element).toHaveFocus();

            wrapper.trigger("keydown.up");
            expect(wrapper.find("[role='listitem']").element).toHaveFocus();
        });

        it("should select element when pressing Enter while focus is on element", async () => {
            const wrapper = AddressbookListSUT().withDefaultsContainers().mount();
            const RANDOM_ELEMENT = 1;

            wrapper.findAll("[role='listitem']").at(RANDOM_ELEMENT).trigger("focus");
            await wrapper.trigger("keydown", { key: "Down" });
            await wrapper.trigger("keydown", { key: "Enter" });

            expect(wrapper.findAll("[role='listitem']").at(RANDOM_ELEMENT + 1).element).toHaveFocus();
            expect(wrapper.emitted().selected[0]).toContain("book:Contacts_79E5C4EB-060F-46CB-88F9-F218E7F139F7");
        });
    });
});

function AddressbookListSUT(data = { addressbooks: [] }) {
    const values = { ...data };

    return {
        withDefaultsContainers() {
            return AddressbookListSUT()._withDirectoryAdressbook()._withAddressbooks()._withCollectedContacts();
        },

        _withDirectoryAdressbook() {
            return AddressbookListSUT({
                ...values,
                addressbooks: [
                    ...values.addressbooks,
                    {
                        uid: "addressbook_75a0d5b3.internal",
                        name: "Annuaire",
                        owner: "addressbook_75a0d5b3.internal",
                        type: "addressbook",
                        defaultContainer: true,
                        domainUid: "75a0d5b3.internal",
                        ownerDisplayname: "Directory",
                        ownerDirEntryPath: "75a0d5b3.internal/addressbooks/addressbook_75a0d5b3.internal"
                    }
                ]
            });
        },

        _withAddressbooks() {
            return AddressbookListSUT({
                ...values,
                addressbooks: [
                    ...values.addressbooks,
                    {
                        uid: "book:Contacts_79E5C4EB-060F-46CB-88F9-F218E7F139F7",
                        name: "Mes contacts",
                        owner: "79E5C4EB-060F-46CB-88F9-F218E7F139F7",
                        type: "addressbook",
                        defaultContainer: true,
                        domainUid: "75a0d5b3.internal",
                        ownerDisplayname: "Jean Giono",
                        ownerDirEntryPath: "75a0d5b3.internal/users/79E5C4EB-060F-46CB-88F9-F218E7F139F7"
                    }
                ]
            });
        },

        _withCollectedContacts() {
            return AddressbookListSUT({
                ...values,
                addressbooks: [
                    ...values.addressbooks,
                    {
                        uid: "book:CollectedContacts_79E5C4EB-060F-46CB-88F9-F218E7F139F7",
                        name: "Contacts collectés",
                        owner: "79E5C4EB-060F-46CB-88F9-F218E7F139F7",
                        type: "addressbook",
                        defaultContainer: false,
                        domainUid: "75a0d5b3.internal",
                        ownerDisplayname: "Jean Giono",
                        ownerDirEntryPath: "75a0d5b3.internal/users/79E5C4EB-060F-46CB-88F9-F218E7F139F7"
                    }
                ]
            });
        },

        withSharedOtherAddressbook() {
            return AddressbookListSUT({
                ...values,
                addressbooks: [
                    ...values.addressbooks,
                    {
                        uid: "17607AE1-078E-4B2C-BB2A-15FC92CE88A1",
                        name: "createdtoshare",
                        owner: "A574855D-907D-4B73-83C3-E2F1D794B50F",
                        type: "addressbook",
                        defaultContainer: false,
                        readOnly: false,
                        domainUid: "75a0d5b3.internal",
                        ownerDisplayname: "George Abitbol",
                        ownerDirEntryPath: "75a0d5b3.internal/users/A574855D-907D-4B73-83C3-E2F1D794B50F"
                    }
                ]
            });
        },

        withSharedDefaultPersonnalAddressbook() {
            return AddressbookListSUT({
                ...values,
                addressbooks: [
                    ...values.addressbooks,
                    {
                        uid: "book:Contacts_A574855D-907D-4B73-83C3-E2F1D794B50F",
                        name: "Mes contacts",
                        owner: "A574855D-907D-4B73-83C3-E2F1D794B50F",
                        type: "addressbook",
                        defaultContainer: true,
                        readOnly: false,
                        domainUid: "75a0d5b3.internal",
                        ownerDisplayname: "George Abitbol",
                        ownerDirEntryPath: "75a0d5b3.internal/users/A574855D-907D-4B73-83C3-E2F1D794B50F"
                    }
                ]
            });
        },

        withSharedCollectedAddressbook() {
            return AddressbookListSUT({
                ...values,
                addressbooks: [
                    ...values.addressbooks,
                    {
                        uid: "book:CollectedContacts_A574855D-907D-4B73-83C3-E2F1D794B50F",
                        name: "Contacts collectés",
                        owner: "A574855D-907D-4B73-83C3-E2F1D794B50F",
                        type: "addressbook",
                        defaultContainer: false,
                        readOnly: false,
                        domainUid: "75a0d5b3.internal",
                        ownerDisplayname: "George Abitbol",
                        ownerDirEntryPath: "75a0d5b3.internal/users/A574855D-907D-4B73-83C3-E2F1D794B50F"
                    }
                ]
            });
        },

        mount(translateStub = () => "") {
            return mount(AddressBookList, {
                propsData: {
                    userId: "79E5C4EB-060F-46CB-88F9-F218E7F139F7",
                    addressbooks: values.addressbooks
                },
                mocks: {
                    $t: translateStub
                },
                attachTo: document.body
            });
        }
    };
}
