/**
 * @jest-environment jsdom
 */
import { screen } from "@testing-library/vue";
import NotebooksPage from "@/pages/NotebooksPage.vue";
import store from "../../src/store/index.js";
import { renderWithStoreAndMockRoute } from "../helpers";
import flushPromises from "flush-promises";
import _ from "lodash";
import makeMe from "../fixtures/makeMe";
 
jest.mock("snackbar-vue", () => ({
    useSnackbarPlugin: () => ({
        show: jest.fn(0)
    })
}));

beforeEach(() => {
   fetch.resetMocks();
});
 
describe("Notebooks Page", () => {
   test("fetch API to be called ONCE", async () => {
        const notebook = makeMe.aNotebook.please()
        const stubResponse = {
            notebooks: [notebook],
            subscriptions: []
        };

        fetch.mockResponse(JSON.stringify(stubResponse));
        renderWithStoreAndMockRoute(store, NotebooksPage, {}, { query: { deletedNoteId: '1' } });

        await flushPromises();

        setTimeout(async () => {
            expect(fetch).toHaveBeenCalledTimes(1);
            expect(fetch).toHaveBeenCalledWith('/api/notebooks', {});
            
            await screen.findByText("Note successfully deleted");
        }, 500);
    });

    test("call /undo-delete when snackbar button is pressed", async () => {
        const notebook = makeMe.aNotebook.please()
        const stubResponse = {
            notebooks: [notebook],
            subscriptions: []
        };
        const deletedNoteId = '1';

        fetch.mockResponses([
            JSON.stringify(stubResponse),
            JSON.stringify({ deletedNoteId })
        ]);
        const { wrapper } = renderWithStoreAndMockRoute(store, NotebooksPage, {}, { query: { deletedNoteId } });

        await flushPromises();

        setTimeout(async () => {
            wrapper.find('.snackbar__action').trigger("click")
            expect(fetch).toHaveBeenCalledWith('/api/notebooks', {});
            expect(fetch).toHaveBeenCalledWith(`/api/notes/${deletedNoteId}/undo-delete`);
        }, 500);

    })
});
