package com.odde.doughnut.models;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Thing;
import java.util.stream.Stream;

public interface ReviewScope {
  Stream<Thing> getNotesHaveNotBeenReviewedAtAll();

  int getNotesHaveNotBeenReviewedAtAllCount();

  Stream<Link> getLinksHaveNotBeenReviewedAtAll();

  int getLinksHaveNotBeenReviewedAtAllCount();

  Stream<Thing> getThingHaveNotBeenReviewedAtAll();
}
