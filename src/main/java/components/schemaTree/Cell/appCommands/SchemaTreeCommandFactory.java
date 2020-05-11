package components.schemaTree.Cell.appCommands;

import application.configuration.Configuration;
import application.history.HistoryManager;
import components.schemaTree.Cell.SchemaTreePluggable;
import components.schemaTree.Cell.Visitors.CanTreeElementBeSafelyUpdatedVisitor;
import components.schemaTree.Cell.Visitors.CreateAddChildStrategyVisitor;
import components.schemaTree.Cell.Visitors.CreateRemovingStrategyVisitor;
import components.schemaTree.Cell.appCommands.strategies.UnremovableRemovingStrategy;
import components.schemaTree.Cell.modelCommands.RenameSchemaTreePluggable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import utils.removable.IRemovable;

public class SchemaTreeCommandFactory {

    TreeView<SchemaTreePluggable> view;
    TreeItem<SchemaTreePluggable> item;

    public SchemaTreeCommandFactory(TreeView<SchemaTreePluggable> view, TreeItem<SchemaTreePluggable> item) {
        this.view = view;
        this.item = item;
    }

    public AddChildStrategy addSchemaTreeChild(SchemaTreePluggable newModel) {
        CreateAddChildStrategyVisitor v = new CreateAddChildStrategyVisitor(view, item, newModel);
        item.getValue().accept(v);
        return v.getResultStrategy();
    }
    public <E extends SchemaTreePluggable&IRemovable> RemovingStrategy removeTreeElement(E element) {
        CreateRemovingStrategyVisitor v = new CreateRemovingStrategyVisitor<>(view, item.getParent().getValue(), element);
        element.accept(v);

        CanTreeElementBeSafelyUpdatedVisitor safeDelete = new CanTreeElementBeSafelyUpdatedVisitor();
        element.accept(safeDelete);

        if(safeDelete.elementCanBeSafelyDeleted()) {
            return v.getResultStrategy();
        }
        else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, Configuration.langBundle.getString("schemaTree_deletion_prevent"), ButtonType.YES, ButtonType.NO);
            alert.showAndWait();
            if (alert.getResult() == ButtonType.YES) {
                return v.getResultStrategy();
            }
            else
                return new UnremovableRemovingStrategy();
        }
    }

    public <E extends SchemaTreePluggable> void renameTreeElement(E element, String newName) {

        RenameSchemaTreePluggable cmd = new RenameSchemaTreePluggable(element, newName);

        CanTreeElementBeSafelyUpdatedVisitor safeDelete = new CanTreeElementBeSafelyUpdatedVisitor();
        element.accept(safeDelete);

        if(safeDelete.elementCanBeSafelyDeleted()) {
            HistoryManager.addCommand(cmd, !element.mustBeRenamed());
        }
        else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, Configuration.langBundle.getString("schemaTree_renaming_prevent"), ButtonType.YES, ButtonType.NO);
            alert.showAndWait();
            if (alert.getResult() == ButtonType.YES) {
                HistoryManager.addCommand(cmd, !element.mustBeRenamed());
            }
        }
    }
}
