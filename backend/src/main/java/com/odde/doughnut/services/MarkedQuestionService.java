package com.odde.doughnut.services;

import com.odde.doughnut.entities.MarkedQuestion;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.MarkedQuestionRequest;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;

public class MarkedQuestionService {

  public MarkedQuestionService(
      User user, Timestamp currentUTCTimestamp, ModelFactoryService modelFactoryService) {}

  public MarkedQuestion markQuestion(MarkedQuestionRequest markedQuestionRequest) {
    MarkedQuestion markedQuestion = new MarkedQuestion();
    markedQuestion.setQuizQuestionId(markedQuestionRequest.quizQuestionId);
    markedQuestion.setNoteId(markedQuestionRequest.noteId);
    markedQuestion.setIsGood(markedQuestionRequest.isGood);
    return markedQuestion;
  }
}