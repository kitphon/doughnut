import { flushPromises } from "@vue/test-utils";
import { afterEach, beforeEach, describe, it } from "vitest";
import SuggestQuestionForFineTuning from "@/components/ai/SuggestQuestionForFineTuning.vue";
import { QuizQuestion } from "@/generated/backend";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("QuizQuestion", () => {
  describe("suggest question for fine tuning AI", () => {
    const quizQuestion: QuizQuestion = makeMe.aQuizQuestion.please();

    let wrapper;

    beforeEach(() => {
      wrapper = helper
        .component(SuggestQuestionForFineTuning)
        .withProps({ quizQuestion })
        .mount();
    });

    it("should be able to suggest a question as good example", async () => {
      helper.apiMock.expectingPost(
        `/api/quiz-questions/${quizQuestion.id}/suggest-fine-tuning`,
      );
      wrapper.get(".negative-feedback-btn").trigger("click");
      wrapper.get("button.btn-success").trigger("click");
      await flushPromises();
    });
  });
});
