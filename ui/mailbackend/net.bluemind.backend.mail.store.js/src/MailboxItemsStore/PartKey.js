export const PartKey = {
    encode(address, message) {
        return message + "/" + address;
    }
};
