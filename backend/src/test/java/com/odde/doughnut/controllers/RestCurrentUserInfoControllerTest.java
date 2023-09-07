package com.odde.doughnut.controllers;

import static com.odde.doughnut.entities.json.Role.DEVELOPER;
import static com.odde.doughnut.entities.json.Role.LEARNER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.json.CurrentUserInfo;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestCurrentUserInfoControllerTest {
  @Autowired MakeMe makeMe;

  @Mock CurrentUserFetcher currentUserFetcher;

  RestCurrentUserInfoController controller(CurrentUserFetcher currentUserFetcher) {
    return new RestCurrentUserInfoController(currentUserFetcher);
  }

  @Test
  void shouldReturnUserInfoIncludingRoleForLearner() {
    UserModel userModel = makeMe.aUser().toModelPlease();
    String externalId = userModel.getEntity().getExternalIdentifier();
    when(currentUserFetcher.getExternalIdentifier()).thenReturn(externalId);
    when(currentUserFetcher.getUser()).thenReturn(userModel);
    CurrentUserInfo currentUserInfo = controller(currentUserFetcher).currentUserInfo();

    assertThat(currentUserInfo.externalIdentifier, equalTo(externalId));
    assertThat(currentUserInfo.user, equalTo(userModel.getEntity()));
    assertThat(currentUserInfo.role, equalTo(LEARNER));
  }

  @Test
  void shouldReturnUserInfoIncludingRoleForDeveloper() {
    UserModel userModel = makeMe.aDeveloper().toModelPlease();
    String externalId = userModel.getEntity().getExternalIdentifier();
    when(currentUserFetcher.getExternalIdentifier()).thenReturn(externalId);
    when(currentUserFetcher.getUser()).thenReturn(userModel);
    CurrentUserInfo currentUserInfo = controller(currentUserFetcher).currentUserInfo();

    assertThat(currentUserInfo.externalIdentifier, equalTo(externalId));
    assertThat(currentUserInfo.user, equalTo(userModel.getEntity()));
    assertThat(currentUserInfo.role, equalTo(DEVELOPER));
  }
}
