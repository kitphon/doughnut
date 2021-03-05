package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.services.ModelFactoryService;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class UserModel extends ModelForEntity<UserEntity>{
    public UserModel(UserEntity userEntity, ModelFactoryService modelFactoryService) {
        super(userEntity, modelFactoryService);
    }

    public String getName() {
        return entity.getName();
    }

    public void setAndSaveDailyNewNotesCount(Integer dailyNewNotesCount) {
        entity.setDailyNewNotesCount(dailyNewNotesCount);
        save();
    }

    public void setAndSaveSpaceIntervals(String spaceIntervals) {
        entity.setSpaceIntervals(spaceIntervals);
        save();
    }

    public List<NoteEntity> getOrphanedNotes() {
        return entity.getOrphanedNotes();
    }

    public void assertAuthorization(NoteEntity noteEntity) throws NoAccessRightException {
        if (! entity.owns(noteEntity)) {
            throw new NoAccessRightException();
        }
    }

    public List<NoteEntity> filterLinkableNotes(NoteEntity noteEntity, String searchTerm) {
        List<NoteEntity> linkableNotes = getAllLinkableNotes(noteEntity);
        if (searchTerm != null) {
            return linkableNotes.stream()
                    .filter(note -> note.getTitle().contains(searchTerm))
                    .collect(Collectors.toList());
        }
        return linkableNotes;
    }

    public List<NoteEntity> getNewNotesToReview(Timestamp currentTime) {
        int count = getNewNotesCountForToday(currentTime);
        return getNotesHaveNotBeenReviewedAtAll().stream().limit(count).collect(Collectors.toList());
    }

    private List<NoteEntity> getNotesHaveNotBeenReviewedAtAll() {
        return modelFactoryService.noteRepository.findByUserWhereThereIsNoReviewPoint(entity.getId());
    }

    private List<ReviewPointEntity> getRecentReviewPoints(Timestamp currentTime) {
        ZonedDateTime zonedDateTime = currentTime.toInstant().atZone(ZoneId.of("UTC"));
        Timestamp oneDayAgo = Timestamp.from(zonedDateTime.minus(1, ChronoUnit.DAYS).toInstant());
        return modelFactoryService.reviewPointRepository.findAllByUserEntityAndInitialReviewedAtGreaterThan(entity, oneDayAgo);
    }

    private int getNewNotesCountForToday(Timestamp currentTime) {
        long sameDayCount = getRecentReviewPoints(currentTime).stream().filter(p -> p.isInitialReviewOnSameDay(currentTime, getTimeZone())).count();
        return (int) (entity.getDailyNewNotesCount() - sameDayCount);
    }

    private List<NoteEntity> getAllLinkableNotes(NoteEntity source) {
        List<NoteEntity> targetNotes = source.getTargetNotes();
        List<NoteEntity> allNotes = entity.getNotes();
        return allNotes.stream()
                .filter(i -> !targetNotes.contains(i))
                .filter(i -> !i.equals(source))
                .collect(Collectors.toList());
    }

    public ZoneId getTimeZone() {
        return ZoneId.of("Asia/Shanghai");
    }
}
