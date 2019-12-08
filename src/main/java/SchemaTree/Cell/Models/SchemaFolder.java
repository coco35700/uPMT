package SchemaTree.Cell.Models;

import NewModel.IRemovable;
import SchemaTree.Cell.SchemaTreePluggable;
import SchemaTree.Cell.Utils;
import SchemaTree.Cell.Visitors.SchemaTreePluggableVisitor;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.DataFormat;

import java.util.LinkedList;

public class SchemaFolder extends SchemaElement implements IRemovable {

    public static final DataFormat format = new DataFormat("SchemaFolder");
    private ListProperty<SchemaCategory> categories;
    private ListProperty<SchemaFolder> folders;
    private SimpleBooleanProperty exists;

    public SchemaFolder(String name) {
        super(name);
        this.categories = new SimpleListProperty<SchemaCategory>(FXCollections.observableList(new LinkedList<SchemaCategory>()));
        this.folders = new SimpleListProperty<SchemaFolder>(FXCollections.observableList(new LinkedList<SchemaFolder>()));
        this.exists = new SimpleBooleanProperty(true);
    }

    public final ObservableList<SchemaCategory> categoriesProperty() { return categories; }
    public final ObservableList<SchemaFolder> foldersProperty() { return folders; }


    @Override
    public DataFormat getDataFormat() {
        return SchemaFolder.format;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    public boolean canContain(SchemaTreePluggable item) {
        return (Utils.IsSchemaTreeCategory(item) || Utils.IsSchemaTreeFolder(item));
    }

    @Override
    public void addChild(SchemaTreePluggable item) {
        if(Utils.IsSchemaTreeCategory(item))
            addCategory((SchemaCategory) item);
        else if(Utils.IsSchemaTreeFolder(item))
            addFolder((SchemaFolder) item);
        else
            throw new IllegalArgumentException("Can't receive this kind of child ! ");
    }

    @Override
    public void removeChild(SchemaTreePluggable item) {
        if(Utils.IsSchemaTreeCategory(item))
            removeCategory((SchemaCategory) item);
        else if(Utils.IsSchemaTreeFolder(item))
            removeFolder((SchemaFolder) item);
        else
            throw new IllegalArgumentException("Can't remove this kind of child !");
    }

    @Override
    public String getIconPath() {
        return "folder.png";
    }

    @Override
    public void accept(SchemaTreePluggableVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public BooleanProperty existsProperty() {
        return exists;
    }

    private void addCategory(SchemaCategory c){
        categories.add(c);
        Utils.setupListenerOnChildRemoving(this, c);
    }
    private void removeCategory(SchemaCategory c){
        categories.remove(c);
    }

    private void addFolder(SchemaFolder f){
        folders.add(f);
        Utils.setupListenerOnChildRemoving(this, f);
    }
    private void removeFolder(SchemaFolder f){
        folders.remove(f);
    }
}
