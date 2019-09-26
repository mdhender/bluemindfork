import DateTimeFormat from "../src/DateTimeFormat";

describe('DateTimeFormat', () => {

    test('should display time in fr format (2-digit)', () => {
        const date = new Date(2019, 1, 1, 1, 0, 0);
        const formattedTime = DateTimeFormat.formatTime(date, 'fr');
        expect(formattedTime).toBe("01:00");
    });

    test('should display date in fr format (2-digit)', () => {
        const date = new Date(2019, 1, 1, 1, 0, 0);
        const formattedTime = DateTimeFormat.formatDate(date, 'fr');
        expect(formattedTime).toBe("01/02/2019");
    });

    test('should display full date in fr format (2-digit)', () => {
        const date = new Date(2019, 1, 1, 1, 0, 0);
        const formattedTime = DateTimeFormat.formatDateWithWeekday(date, 'fr');
        expect(formattedTime).toBe("ven. 01/02/2019");
    });
});