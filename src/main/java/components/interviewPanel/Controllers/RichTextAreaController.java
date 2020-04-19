package components.interviewPanel.Controllers;

import components.interviewPanel.appCommands.AddAnnotationCommand;
import components.interviewPanel.appCommands.RemoveAnnotationCommand;
import components.interviewPanel.utils.LetterMap;
import components.interviewPanel.utils.TextStyle;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import models.*;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.Caret;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.event.MouseOverTextEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static utils.GlobalVariables.getGlobalVariables;

public class RichTextAreaController {
    private final InlineCssTextArea area;
    private ContextMenu menu;

    private final InterviewText interviewText;
    private LetterMap letterMap;
    private final SimpleObjectProperty<IndexRange> userSelection;
    private ArrayList<Descripteme> emphasizedDescriptemes; // used temporary when over a descripteme
    private final ArrayList<Moment> emphasizedMoments = new ArrayList<>(); // used temporary when over a descripteme
    private Color toolColorSelected; // the selected tool, if null -> default tool is descripteme

    private boolean eraserToolSelected = false;

    private ListChangeListener<Annotation> onAnnotationsChangeListener = change -> {
        while (change.next()) {
            for (Annotation removed : change.getRemoved()) {
                letterMap.removeAnnotation(removed.getStartIndex(), removed.getEndIndex());
                applyStyle(removed);
            }
            for (Annotation added : change.getAddedSubList()) {
                letterMap.becomeAnnotation(added, added.getColor());
                applyStyle(added);
            }
        }
    };

    public RichTextAreaController(InterviewText interviewText) {
        this.interviewText = interviewText;
        this.letterMap = new LetterMap();
        userSelection = new SimpleObjectProperty<>();
        area = new InlineCssTextArea();
        area.setWrapText(true);
        area.setEditable(false);
        area.setParagraphGraphicFactory(LineNumberFactory.get(area));
        area.appendText(interviewText.getText());
        area.setShowCaret(Caret.CaretVisibility.ON);

        setUpClick();
        setUpPopUp();
        setUpMenu();

        // Two listeners that update the view (highlight and underline)
        this.interviewText.getAnnotationsProperty().addListener(
                new WeakListChangeListener<>(onAnnotationsChangeListener));

        this.interviewText.getDescriptemesProperty().addListener((ListChangeListener.Change<? extends Descripteme> c) -> {
            while (c.next()) {
                for (Descripteme removed : c.getRemoved()) {
                    letterMap.removeDescripteme(removed);
                    applyStyle(removed);
                }
                for (Descripteme added : c.getAddedSubList()) {
                    letterMap.becomeDescripteme(added);
                    applyStyle(added);
                }
            }
        });

        getGlobalVariables()
                .getDescriptemeChangedProperty()
                .addListener(newValue -> { this.updateDescripteme(); });


        interviewText.getAnnotationsProperty().forEach(annotation -> {
            letterMap.becomeAnnotation(annotation, annotation.getColor());
            applyStyle(annotation);
        });
    }

    private void setUpClick() {
        area.setOnMousePressed(event -> {
            Annotation previousSelected = letterMap.getSelectedAnnotation();
            if (previousSelected != null) {
                letterMap.deSelectAnnotation();
                applyStyle(previousSelected);
            }

            Annotation annotation = interviewText.getFirstAnnotationByIndex(area.getCaretPosition());
            if (annotation != null) {
                letterMap.selectAnnotation(annotation);
                applyStyle(annotation);
            }
        });

        area.setOnMouseReleased(event -> {
            userSelection.set(new IndexRange(
                    area.getSelection().getStart(),
                    area.getSelection().getEnd()
            ));

            if (toolColorSelected != null && area.getSelection().getStart() != area.getSelection().getEnd()) {
                annotate(toolColorSelected);
            }
            if (eraserToolSelected && area.getSelection().getStart() != area.getSelection().getEnd()) {
                erase();
            }
        });
    }

    private void setUpPopUp() {
        Popup popup = new Popup();
        Label popupMsg = new Label();
        popupMsg.setStyle(
                "-fx-background-color: black;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 5;");
        popup.getContent().add(popupMsg);

        area.setMouseOverTextDelay(Duration.ofMillis(500));
        area.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN, event -> {
            Point2D pos = event.getScreenPosition();

            // emphasize descripteme in modeling spac
            emphasizedDescriptemes = interviewText.getDescriptemesByIndex(event.getCharacterIndex());
            if (emphasizedDescriptemes != null) {
                String message = emphasizedDescriptemes.size() + " descripteme(s)";
                popup.show(area, pos.getX(), pos.getY() + 10);

                emphasizedMoments.clear();
                for (Descripteme descripteme : emphasizedDescriptemes) {
                    descripteme.getEmphasizeProperty().set(true);
                    emphasizedMoments.addAll(getGlobalVariables().getMomentsByDescripteme(descripteme));
                }
                for(Moment moment : emphasizedMoments) {
                    moment.getEmphasizeProperty().set(true);
                }
                message += " dans " + emphasizedMoments.size() + " moment(s)";

                popupMsg.setText(message);
                popup.show(area, pos.getX(), pos.getY() + 10);
            }
        });
        area.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_END, event -> {
            popup.hide();

            // de-emphasize descripteme in modeling space
            if (emphasizedDescriptemes != null) {
                for (Descripteme descripteme : emphasizedDescriptemes) {
                    descripteme.getEmphasizeProperty().set(false);
                }
                emphasizedDescriptemes = null;
            }
            if (!emphasizedMoments.isEmpty()) {
                for(Moment moment : emphasizedMoments) {
                    moment.getEmphasizeProperty().set(false);
                }
                emphasizedMoments.clear();
            }
        });
    }

    private  void setUpMenu() {
        menu = new ContextMenu();

        MenuItem deleteAnnotationMenuItem = new MenuItem("Delete annotation");
        deleteAnnotationMenuItem.setOnAction(event -> {
            Annotation annotation = interviewText.getFirstAnnotationByIndex(area.getCaretPosition());
            deleteAnnotation(annotation);
        });
        MenuItem item1 = new MenuItem("Yellow");
        item1.setOnAction(e -> {
            if (!area.getSelectedText().isEmpty()) {
                annotate(Color.YELLOW,
                        userSelection.get().getStart(),
                        userSelection.get().getEnd());
            }
        });
        MenuItem item2 = new MenuItem("Red");
        item2.setOnAction(e -> {
            if (!area.getSelectedText().isEmpty()) {
                annotate(Color.RED,
                        userSelection.get().getStart(),
                        userSelection.get().getEnd());
            }
        });
        MenuItem item3 = new MenuItem("Blue");
        item3.setOnAction(e -> {
            if (!area.getSelectedText().isEmpty()) {
                annotate(Color.BLUE,
                        userSelection.get().getStart(),
                        userSelection.get().getEnd());
            }
        });
        menu.getItems().addAll(deleteAnnotationMenuItem);
        area.setContextMenu(menu);
    }

    private void updateDescripteme() {
        Descripteme changedDescripteme = getGlobalVariables()
                .getDescriptemeChangedProperty().getValue();
        if (!getGlobalVariables().getAllDescripteme().contains(changedDescripteme)) {
            // the change is a deletion of the descripteme
            interviewText.getDescriptemesProperty().remove(changedDescripteme);
        }
    }

    private void applyStyle(Fragment fragment) {
        applyStyle(fragment.getStartIndex(), fragment.getEndIndex());
    }

    private void applyStyle(int start, int end) {
        for (int i = start ; i < end ; i++) {
            TextStyle style = letterMap.getStyleByIndex(i);
            if (style != null) {
                String css = "";
                if (style.getIsAnnotation()) {
                    css += "-rtfx-background-color: " + style.getCSSColor() + ";";
                }
                if (style.getIsDescripteme()) {
                    css += "-rtfx-underline-color: black; " + "-rtfx-underline-width: 1;";
                }
                else if (style.getIsSeveralDescriptemes()) {
                    css += "-rtfx-underline-color: black; " + "-rtfx-underline-width: 2;";
                }
                area.setStyle(i, i+1, css);
            }

        }
    }

    private void erase() {
        IndexRange selection = area.getSelection();
        interviewText.cutAnnotation(selection.getStart(), selection.getEnd());
        letterMap.removeAnnotation(selection.getStart(), selection.getEnd());
        applyStyle(selection.getStart(), selection.getEnd());
        List<Annotation> sortedAnnotations = interviewText.getSortedAnnotation();
        List<Annotation> annotationToDelete = new ArrayList<>();
        sortedAnnotations.forEach(annotation -> {
            if (annotation.getStartIndex() >= selection.getStart()
                    && annotation.getEndIndex() <= selection.getEnd()) {
                annotationToDelete.add(annotation);
            }

        });
        annotationToDelete.forEach(annotation -> deleteAnnotation(annotation));
    }

    private void annotate(Color color, Integer start, Integer end) {
        Annotation annotation = new Annotation(
                interviewText,
                start,
                end,
                color);
        new AddAnnotationCommand(interviewText, annotation).execute();
    }

    private void deleteAnnotation(Annotation annotation) {
        new RemoveAnnotationCommand(interviewText, annotation).execute();
        // there is a listener that apply the style
    }


    public void annotate(Color color) {
        IndexRange selection = area.getSelection();
        if (selection.getStart() != selection.getEnd()) {
            annotate(color, selection.getStart(), selection.getEnd());
            area.deselect();
        }
    }

    public String getSelectedText() {
        return area.getSelectedText();
    }

    public IndexRange getSelection() {
        return area.getSelection();
    }

    public void setEraserToolSelected(boolean eraserToolSelected) {
        this.eraserToolSelected = eraserToolSelected;
    }

    public VirtualizedScrollPane<InlineCssTextArea> getNode() {
        VirtualizedScrollPane<InlineCssTextArea> vsPane = new VirtualizedScrollPane(area);
        return vsPane;
    }

    public SimpleObjectProperty<IndexRange> getUserSelection() {
        return userSelection;
    }

    public void addDescripteme(Descripteme descripteme) {
        interviewText.addDescripteme(descripteme);
        descripteme.startIndexProperty().addListener((observable, oldValue, newValue) -> {
            // create a temporary descripteme with the shape avec the previous descripteme...
            Descripteme temp = new Descripteme(interviewText, oldValue.intValue(), descripteme.getEndIndex());
            // ... in order to be able to delete the underline
            letterMap.removeDescripteme(temp);
            applyStyle(temp);
            letterMap.becomeDescripteme(descripteme);
            applyStyle(descripteme);
        });
        descripteme.endIndexProperty().addListener((observable, oldValue, newValue) -> {
            // create a temporary descripteme with the shape avec the previous descripteme...
            Descripteme temp = new Descripteme(interviewText, descripteme.getStartIndex(), oldValue.intValue());
            // ... in order to be able to delete the underline
            letterMap.removeDescripteme(temp);
            applyStyle(temp);
            letterMap.becomeDescripteme(descripteme);
            applyStyle(descripteme);
        });
    }

    public void setToolColorSelected(Color color) {
        toolColorSelected = color;
    }

    public Color getToolColorSelected() {
        return toolColorSelected;
    }
}