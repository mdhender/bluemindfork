import signatureUtils from "../signature";

describe("Signature", () => {
    test("Trim signature", () => {
        const signature = `
  
        <div><br></div>
        <div><style></style></div>
        <div><i><b><br></b></i></div>

        <div style="height: 20px; background-image: url('http://fakeurl.bluemind.net/image.png')"></div>
        <div>-=My=-</div>
        <div><br></div>
        <div><br></div>
        <div><style></style></div>
        <div>-=Awesome=-</div>
        <div><br></div>
        <div>-=Signature=-</div>
        <div><p><img src="http://fakeurl.bluemind.net/image.png"></p></div>
  
        <div><style></style></div>

        <div><br></div>
`;
        const trimmed = `<div style="height: 20px; background-image: url('http://fakeurl.bluemind.net/image.png')"></div>
        <div>-=My=-</div>
        <div><br></div>
        <div><br></div>
        <div><style></style></div>
        <div>-=Awesome=-</div>
        <div><br></div>
        <div>-=Signature=-</div>
        <div><p><img src="http://fakeurl.bluemind.net/image.png"></p></div>`;

        expect(signatureUtils.trimSignature(signature)).toEqual(trimmed);
    });
});
