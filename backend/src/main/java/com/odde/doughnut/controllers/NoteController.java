package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.*;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

import javax.validation.Valid;
import java.io.IOException;

@Controller
@RequestMapping("/notes")
public class NoteController extends ApplicationMvcController  {
    private final ModelFactoryService modelFactoryService;

    public NoteController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService) {
        super(currentUserFetcher);
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("")
    public String myNotes(Model model) {
        model.addAttribute("notes", currentUserFetcher.getUser().getOwnershipModel().getOrphanedNotes());
        return "notes/index";
    }

    @GetMapping({"/new", "/{parentNote}/new"})
    public String newNote(@PathVariable(name = "parentNote", required = false) NoteEntity parentNote, Model model) throws NoAccessRightException {
        UserModel userModel = currentUserFetcher.getUser();
        if (parentNote != null) {
            userModel.assertAuthorization(parentNote);
        }
        NoteContentEntity noteContentEntity = new NoteContentEntity();
        model.addAttribute("ownershipEntity", userModel.getEntity().getOwnershipEntity());
        model.addAttribute("noteContentEntity", noteContentEntity);
        return "notes/new";
    }

    @PostMapping({"/{ownershipEntity}/create_top", "/{parentNote}/create"})
    public String createNote(@PathVariable(name = "ownershipEntity", required = false) OwnershipEntity ownershipEntity, @PathVariable(name = "parentNote", required = false) NoteEntity parentNote, @Valid NoteContentEntity noteContentEntity, BindingResult bindingResult, Model model) throws NoAccessRightException, IOException {
        if (bindingResult.hasErrors()) {
            return "notes/new";
        }
        if (parentNote != null) {
            currentUserFetcher.getUser().assertAuthorization(parentNote);
        }
        NoteEntity noteEntity = new NoteEntity();
        noteEntity.setNoteContent(noteContentEntity);
        noteEntity.setParentNote(parentNote);
        noteEntity.setOwnershipEntity(ownershipEntity);
        if(ownershipEntity == null) {
            noteEntity.setOwnershipEntity(parentNote.getOwnershipEntity());
        }

        UserModel userModel = currentUserFetcher.getUser();
        NoteContentModel noteContentModel = modelFactoryService.toNoteModel(noteEntity);
        noteContentModel.createByUser(userModel);
        return "redirect:/notes/" + noteEntity.getId();
    }

    @GetMapping("/{noteEntity}")
    public String showNote(@PathVariable(name = "noteEntity") NoteEntity noteEntity, Model model) {
        model.addAttribute("treeNodeModel", modelFactoryService.toTreeNodeModel(noteEntity));
        return "notes/show";
    }

    @GetMapping("/{noteEntity}/edit")
    public String editNote(NoteEntity noteEntity, Model model) {
        model.addAttribute("noteContentEntity", noteEntity.getNoteContent());
        return "notes/edit";
    }

    @PostMapping("/{noteEntity}")
    public String updateNote(@PathVariable(name = "noteEntity") NoteEntity noteEntity, @Valid NoteContentEntity noteContentEntity, BindingResult bindingResult) throws NoAccessRightException, IOException {
        currentUserFetcher.getUser().assertAuthorization(noteEntity);
        if (bindingResult.hasErrors()) {
            return "notes/edit";
        }
        noteEntity.setNoteContent(noteContentEntity);
        NoteContentModel noteContentModel = modelFactoryService.toNoteModel(noteEntity);
        noteContentModel.update(currentUserFetcher.getUser());
        return "redirect:/notes/" + noteEntity.getId();
    }

    @GetMapping("/{noteEntity}/move")
    public String prepareToMove(NoteEntity noteEntity, Model model) {
        model.addAttribute("noteMotion", getLeftNoteMotion(noteEntity));
        model.addAttribute("noteMotionRight", getRightNoteMotion(noteEntity));
        model.addAttribute("noteMotionUnder", new NoteMotionEntity(null, true));
        return "notes/move";
    }

    private NoteMotionEntity getLeftNoteMotion(NoteEntity noteEntity) {
        TreeNodeModel treeNodeModel = this.modelFactoryService.toTreeNodeModel(noteEntity);
        NoteEntity previousSiblingNote = treeNodeModel.getPreviousSiblingNote();
        TreeNodeModel prev = this.modelFactoryService.toTreeNodeModel(previousSiblingNote);
        NoteEntity prevprev = prev.getPreviousSiblingNote();
        if (prevprev == null) {
            return new NoteMotionEntity(noteEntity.getParentNote(), true);
        }
        return new NoteMotionEntity(prevprev, false);
    }

    private NoteMotionEntity getRightNoteMotion(NoteEntity noteEntity) {
        TreeNodeModel treeNodeModel = this.modelFactoryService.toTreeNodeModel(noteEntity);
        return new NoteMotionEntity(treeNodeModel.getNextSiblingNote(), false);
    }

    @PostMapping("/{noteEntity}/move")
    @Transactional
    public String moveNote(NoteEntity noteEntity, NoteMotionEntity noteMotionEntity) throws CyclicLinkDetectedException, NoAccessRightException {
        currentUserFetcher.getUser().assertAuthorization(noteEntity);
        currentUserFetcher.getUser().assertAuthorization(noteMotionEntity.getRelativeToNote());
        modelFactoryService.toNoteMotionModel(noteMotionEntity, noteEntity).execute();
        return "redirect:/notes/" + noteEntity.getId();
    }

    @PostMapping(value = "/{noteEntity}/delete")
    @Transactional
    public RedirectView deleteNote(@PathVariable("noteEntity") NoteEntity noteEntity) throws NoAccessRightException {
        currentUserFetcher.getUser().assertAuthorization(noteEntity);
        modelFactoryService.toTreeNodeModel(noteEntity).destroy();
        return new RedirectView("/notes");
    }

    @GetMapping("/{noteEntity}/review_setting")
    public String editReviewSetting(NoteEntity noteEntity, Model model) {
        ReviewSettingEntity reviewSettingEntity = noteEntity.getMasterReviewSettingEntity();
        if(reviewSettingEntity == null) {
            reviewSettingEntity = new ReviewSettingEntity();
        }
        model.addAttribute("reviewSettingEntity", reviewSettingEntity);
        return "notes/edit_review_setting";
    }

    @PostMapping(value = "/{noteEntity}/review_setting")
    @Transactional
    public String updateReviewSetting(@PathVariable("noteEntity") NoteEntity noteEntity, @Valid ReviewSettingEntity reviewSettingEntity, BindingResult bindingResult) throws NoAccessRightException {
        if (bindingResult.hasErrors()) {
            return "notes/edit_review_setting";
        }
        currentUserFetcher.getUser().assertAuthorization(noteEntity);
        modelFactoryService.toNoteModel(noteEntity).setAndSaveMasterReviewSetting(reviewSettingEntity);

        return "redirect:/notes/" + noteEntity.getId();
    }

    @PostMapping(value = "/{note}/share")
    public RedirectView shareNote(@PathVariable("note") NoteEntity note) throws NoAccessRightException {
        currentUserFetcher.getUser().assertAuthorization(note);
        BazaarModel bazaar = modelFactoryService.toBazaarModel();
        bazaar.shareNote(note);
        return new RedirectView("/notes");
    }

}
