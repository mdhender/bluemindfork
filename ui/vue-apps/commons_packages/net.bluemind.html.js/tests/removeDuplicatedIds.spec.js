import removeDuplicatedIds from "../src/removeDuplicatedIds";

describe("Remove duplicated ids in HTML", () => {
    test("Duplicated ids are removed", () => {
        const html = `<div id="AAA">
    <div id="AAA">
        <div id="AAA">
            <div id="CCC">
            </div>
        </div>
    </div>
</div>
<div id="BBB">
    <div id="AAA">
        <div id="BBB">
            <div id="AAA">
                <div id="BBB">
                    <div id="CCC">
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<div id="CCC">
    <div id="BBB">
        <div id="AAA">
            <div id="BBB">
                <div id="AAA">
                    <div id="BBB">
                        <div id="CCC">
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>`;

        const expectedResult = `<div id="AAA">
    <div>
        <div>
            <div id="CCC">
            </div>
        </div>
    </div>
</div>
<div id="BBB">
    <div>
        <div>
            <div>
                <div>
                    <div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<div>
    <div>
        <div>
            <div>
                <div>
                    <div>
                        <div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>`;

        expect(removeDuplicatedIds(html)).toEqual(expectedResult);
    });
});
