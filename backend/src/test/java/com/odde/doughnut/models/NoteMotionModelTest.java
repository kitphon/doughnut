package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.NoteMotionEntity;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class NoteMotionModelTest {
    @Autowired
    ModelFactoryService modelFactoryService;

    @Autowired MakeMe makeMe;
    NoteEntity topNote;
    NoteEntity firstChild;
    NoteEntity secondChild;

    @BeforeEach
    void setup() {
        topNote = makeMe.aNote().please();
        firstChild = makeMe.aNote().under(topNote).please();
        secondChild = makeMe.aNote().under(topNote).please();
    }

    void move(NoteEntity subject, NoteEntity relativeNote, boolean asFirstChildOfNote) throws CyclicLinkDetectedException {
        NoteMotionEntity motion = new NoteMotionEntity(relativeNote, asFirstChildOfNote);
        NoteMotionModel noteMotionModel = modelFactoryService.toNoteMotionModel(motion, subject);
        noteMotionModel.execute();
    }

    @Test
    void moveBehind() throws CyclicLinkDetectedException {
        move(firstChild, secondChild, false);
        assertThat(secondChild.getSiblingOrder(), is(lessThan(firstChild.getSiblingOrder())));
    }

    @Test
    void moveSecondBehindFirst() throws CyclicLinkDetectedException {
        move(secondChild, firstChild, false);
        assertThat(firstChild.getSiblingOrder(), is(lessThan(secondChild.getSiblingOrder())));
    }

    @Test
    void moveSecondToBeTheFirstSibling() throws CyclicLinkDetectedException {
        move(secondChild, topNote, true);
        assertThat(secondChild.getSiblingOrder(), is(lessThan(firstChild.getSiblingOrder())));
    }

    @Test
    void moveUnder() throws CyclicLinkDetectedException {
        move(firstChild, secondChild, true);
        assertThat(firstChild.getParentNote(), equalTo(secondChild));
    }

    @Test
    void moveBothToTheEndInSequence() throws CyclicLinkDetectedException {
        move(firstChild, secondChild, false);
        move(secondChild, firstChild, false);
        assertThat(firstChild.getSiblingOrder(), is(lessThan(secondChild.getSiblingOrder())));
    }

    @Nested
    class WhenThereIsAThirdLevel {
        NoteEntity thirdLevel;

        @BeforeEach
        void setup() {
            thirdLevel = makeMe.aNote().under(firstChild).please();
        }

        @Test
        void moveAfterNoteOfDifferentLevel() throws CyclicLinkDetectedException {
            move(secondChild, thirdLevel, false);
            assertThat(secondChild.getParentNote(), equalTo(firstChild));
        }

        @Test
        void moveToOwnDescendentIsNotAllowed() {
            assertThrows(CyclicLinkDetectedException.class, ()->
                move(topNote, thirdLevel, false)
            );
        }
    }

    @Nested
    class WhenThereIsAThirdChild {
        NoteEntity thirdChild;

        @BeforeEach
        void setup() {
            thirdChild = makeMe.aNote().under(topNote).please();
        }

        @Test
        void moveBetweenSecondAndThird() throws CyclicLinkDetectedException {
            move(firstChild, secondChild, false);
            assertThat(secondChild.getSiblingOrder(), is(lessThan(firstChild.getSiblingOrder())));
            assertThat(firstChild.getSiblingOrder(), is(lessThan(thirdChild.getSiblingOrder())));
        }
    }

}

