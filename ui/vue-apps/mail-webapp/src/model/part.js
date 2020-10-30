export function setAddresses(structure) {
    structure.address = "TEXT";
    setAddressesForChildren(structure.children);
}

function setAddressesForChildren(children, base = "") {
    children
        .filter(part => !isTemporaryPart(part))
        .forEach((part, index) => {
            part.address = base ? base + "." + (index + 1) : index + 1 + "";
            if (part.children) {
                setAddressesForChildren(part.children, part.address);
            }
        });
}

export function isTemporaryPart(part) {
    /*
     * if part is only uploaded, its address is an UID
     * if part is built in EML, its address is something like "1.1"
     * if address is not defined, it's a multipart so considered as built in EML
     */
    return part.address ? !/^([0-9]+)(\.[0-9]+)*$/.test(part.address) : false;
}
