package com.odde.doughnut.entities.quizQuestions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionPresenter;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.SpellingQuizPresenter;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("2")
public class QuizQuestionSpelling extends QuizQuestionEntity {

  public QuizQuestionSpelling(Note note) {
    super(note);
  }

  @JsonIgnore
  public QuizQuestionPresenter buildPresenter() {
    return new SpellingQuizPresenter(this);
  }

  @Override
  public boolean checkAnswer(Answer answer) {
    return getNote().matchAnswer(answer.getSpellingAnswer());
  }
}