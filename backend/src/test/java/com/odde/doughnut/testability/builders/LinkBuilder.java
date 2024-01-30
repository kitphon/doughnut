package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;

public class LinkBuilder extends EntityBuilder<Thing> {
  public LinkBuilder(MakeMe makeMe) {
    super(
        makeMe,
        Thing.createThing(null, new Link(), new Timestamp(System.currentTimeMillis())).getThing());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getSourceNote() == null) return;
    makeMe
        .aNote()
        .creatorAndOwner(entity.getCreator())
        .under(entity.getSourceNote())
        .target(entity.getTargetNote(), entity.getLinkType())
        .please(needPersist);
  }

  public LinkBuilder creator(User user) {
    entity.setCreator(user);
    return this;
  }

  public LinkBuilder between(Note from, Note to, Link.LinkType linkType) {
    entity.getLink().setTargetNote(to);
    entity.getLink().setSourceNote(from);
    entity.setLinkType(linkType);
    creator(from.getThing().getCreator());
    from.getLinks().add(entity.getLink());
    to.getRefers().add(entity.getLink());
    return this;
  }

  public LinkBuilder between(Note from, Note to) {
    return between(from, to, Link.LinkType.SPECIALIZE);
  }
}
