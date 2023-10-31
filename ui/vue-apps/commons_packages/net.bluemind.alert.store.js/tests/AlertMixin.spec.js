import { lastErrorReason } from "../src/AlertMixin";

describe("AlertMixin", () => {
    test("lastErrorReason", () => {
        expect(
            lastErrorReason(
                new Error(` blabla.OneException: 
                   bla.bla.ServerFault: toto: blabla.BlaError:
           blablabla.bla.TwoException:  blab.ThreeException:
                  blablablalba.FourException:   Bla Bla Error!   `)
            )
        ).toEqual("Bla Bla Error!");
    });
});
