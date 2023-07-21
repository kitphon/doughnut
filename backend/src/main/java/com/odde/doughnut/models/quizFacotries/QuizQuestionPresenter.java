package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.PictureWithMask;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.entities.json.LinksOfANote;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.util.Strings;

public interface QuizQuestionPresenter {
  String instruction();

  String mainTopic();

  boolean isAnswerCorrect(String spellingAnswer);

  default LinksOfANote hintLinks() {
    return null;
  }

  default QuizQuestion.OptionCreator optionCreator() {
    return new QuizQuestion.TitleOptionCreator();
  }

  default Optional<PictureWithMask> pictureWithMask() {
    return Optional.empty();
  }

  default List<QuizQuestion.Option> getOptions(
      QuizQuestionEntity quizQuestionEntity, ModelFactoryService modelFactoryService) {
    QuizQuestion.OptionCreator optionCreator = optionCreator();
    String optionThingIds = quizQuestionEntity.getOptionThingIds();
    if (Strings.isBlank(optionThingIds)) return List.of();
    List<Integer> idList =
        Arrays.stream(optionThingIds.split(","))
            .map(Integer::parseInt)
            .collect(Collectors.toList());
    Stream<Thing> noteStream =
        modelFactoryService
            .thingRepository
            .findAllByIds(idList)
            .sorted(Comparator.comparing(v -> idList.indexOf(v.getId())));
    return noteStream.map(optionCreator::optionFromThing).toList();
  }
}
