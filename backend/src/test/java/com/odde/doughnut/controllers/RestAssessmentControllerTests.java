package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.AssessmentResult;
import com.odde.doughnut.controllers.dto.NoteIdAndTitle;
import com.odde.doughnut.controllers.dto.QuestionAnswerPair;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.builders.NoteBuilder;
import com.theokanning.openai.client.OpenAiApi;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RestAssessmentControllerTests {
  @Mock OpenAiApi openAiApi;
  @Autowired MakeMe makeMe;
  private UserModel currentUser;
  private RestAssessmentController controller;

  @BeforeEach
  void setup() {
    currentUser = makeMe.aUser().toModelPlease();
    controller = new RestAssessmentController(openAiApi, makeMe.modelFactoryService, currentUser);
  }

  @Nested
  class assessmentQuestionOrderTest {
    private Notebook notebook;
    private Note topNote;

    @BeforeEach
    void setup() {
      topNote = makeMe.aHeadNote("OnlineAssessment").creatorAndOwner(currentUser).please();
      notebook = topNote.getNotebook();
    }

    @Test
    void shouldPickRandomNotesForAssessment() throws UnexpectedNoAccessRightException {
      makeMe.theNote(topNote).withNChildrenThat(10, NoteBuilder::hasAnApprovedQuestion).please();
      notebook.getNotebookSettings().setNumberOfQuestionsInAssessment(1);

      Set<Integer> questionIds = new HashSet<>();
      for (int i = 0; i < 3; i++) {
        List<QuizQuestion> assessment = controller.generateAssessmentQuestions(notebook);
        Integer questionId = assessment.get(0).getId();
        questionIds.add(questionId);
      }

      assertTrue(questionIds.size() > 1, "Expected questions from different notes.");
    }

    @Test
    void shouldPickRandomQuestionsFromTheSameNote() throws UnexpectedNoAccessRightException {
      int numberOfQuestions = 3;

      Consumer<NoteBuilder> multipleApprovedQuestionsForNote =
          noteBuilder -> noteBuilder.hasApprovedQuestions(numberOfQuestions);

      makeMe.theNote(topNote).withNChildrenThat(1, multipleApprovedQuestionsForNote).please();
      notebook.getNotebookSettings().setNumberOfQuestionsInAssessment(1);

      Set<Integer> questionIds = new HashSet<>();
      for (int i = 0; i < 10; i++) {
        List<QuizQuestion> assessment = controller.generateAssessmentQuestions(notebook);
        Integer questionId = assessment.get(0).getId();
        questionIds.add(questionId);
      }

      assertEquals(numberOfQuestions, questionIds.size(), "Expected questions from the same note.");
    }
  }

  @Nested
  class generateOnlineAssessmentTest {
    private Notebook notebook;
    private Note topNote;

    @BeforeEach
    void setup() {
      topNote = makeMe.aHeadNote("OnlineAssessment").creatorAndOwner(currentUser).please();
      notebook = topNote.getNotebook();
    }

    @Test
    void whenNotLogin() {
      controller =
          new RestAssessmentController(
              openAiApi, makeMe.modelFactoryService, makeMe.aNullUserModelPlease());
      assertThrows(
          ResponseStatusException.class, () -> controller.generateAssessmentQuestions(notebook));
    }

    @Test
    void shouldNotBeAbleToAccessNotebookWhenUserHasNoPermission() {
      User anotherUser = makeMe.aUser().please();
      notebook.setOwnership(anotherUser.getOwnership());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.generateAssessmentQuestions(notebook));
    }

    @Test
    void shouldBeAbleToAccessNotebookThatIsInTheBazaar() throws UnexpectedNoAccessRightException {
      Note noteOwnedByOtherUser = makeMe.aNote().please();
      makeMe
          .theNote(noteOwnedByOtherUser)
          .withNChildrenThat(6, NoteBuilder::hasAnApprovedQuestion)
          .please();
      noteOwnedByOtherUser.getNotebook().getNotebookSettings().setNumberOfQuestionsInAssessment(5);
      BazaarNotebook bazaarNotebook =
          makeMe.aBazaarNotebook(noteOwnedByOtherUser.getNotebook()).please();
      List<QuizQuestion> assessment =
          controller.generateAssessmentQuestions(bazaarNotebook.getNotebook());
      assertEquals(5, assessment.size());
    }

    @Test
    void shouldReturn5QuestionsWhenThereAreMoreThan5NotesWithQuestions()
        throws UnexpectedNoAccessRightException {
      makeMe.theNote(topNote).withNChildrenThat(5, NoteBuilder::hasAnApprovedQuestion).please();
      notebook.getNotebookSettings().setNumberOfQuestionsInAssessment(5);
      List<QuizQuestion> assessment = controller.generateAssessmentQuestions(notebook);
      assertEquals(5, assessment.size());
    }

    @Test
    void shouldThrowExceptionWhenThereAreNotEnoughQuestions() {
      makeMe.theNote(topNote).withNChildrenThat(4, NoteBuilder::hasAnApprovedQuestion).please();
      assertThrows(ApiException.class, () -> controller.generateAssessmentQuestions(notebook));
    }

    @Test
    void shouldGetOneQuestionFromEachNoteOnly() {
      makeMe
          .theNote(topNote)
          .withNChildrenThat(
              3,
              noteBuilder -> {
                noteBuilder.hasAnApprovedQuestion();
                noteBuilder.hasAnApprovedQuestion();
                noteBuilder.hasAnApprovedQuestion();
              })
          .please();

      assertThrows(ApiException.class, () -> controller.generateAssessmentQuestions(notebook));
    }
  }

  @Nested
  class completeAssessmentTest {
    private Notebook notebook;
    private Note topNote;
    private List<QuestionAnswerPair> questionsAnswerPairs;

    @BeforeEach
    void setup() {
      topNote = makeMe.aHeadNote("OnlineAssessment").creatorAndOwner(currentUser).please();
      notebook = topNote.getNotebook();

      makeMe.theNote(topNote).withNChildrenThat(2, NoteBuilder::hasAnApprovedQuestion).please();
      notebook.getNotebookSettings().setNumberOfQuestionsInAssessment(2);
      questionsAnswerPairs = new ArrayList<>();

      for (Note note : notebook.getNotes()) {
        QuizQuestionAndAnswer quizQuestionAndAnswer = note.getQuizQuestionAndAnswers().getFirst();
        QuestionAnswerPair questionAnswerPair = new QuestionAnswerPair();
        questionAnswerPair.setQuestionId(quizQuestionAndAnswer.getId());
        quizQuestionAndAnswer.setCorrectAnswerIndex(1);
        questionAnswerPair.setAnswerId(0);
        questionsAnswerPairs.add(questionAnswerPair);
      }
    }

    @Disabled
    @Test
    void submitAssessmentResultCheckScore() throws UnexpectedNoAccessRightException {
      AssessmentResult assessmentResult =
          controller.submitAssessmentResult(notebook, questionsAnswerPairs);

      assertEquals(questionsAnswerPairs.size(), assessmentResult.getTotalCount());
      assertEquals(0, assessmentResult.getCorrectCount());
    }

    @Disabled
    @Test
    void submitAssessmentResultCheckScore() throws UnexpectedNoAccessRightException {
      AssessmentResult assessmentResult =
          controller.submitAssessmentResult(notebook, questionsAnswerPairs);

      assertEquals(questionsAnswerPairs.size(), assessmentResult.getNoteIdAndTitles().length);
      NoteIdAndTitle noteIdAndTitle = assessmentResult.getNoteIdAndTitles()[0];
      assertEquals(2, noteIdAndTitle.getId());
      assertEquals("Singapore", noteIdAndTitle.getTitle());
    }
  }
}
