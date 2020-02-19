package persistency.newSaveSystem;

import components.interviewPanel.Models.Descripteme;
import components.modelisationSpace.justification.models.Justification;
import persistency.newSaveSystem.serialization.ObjectSerializer;
import persistency.newSaveSystem.serialization.Serializable;

import java.util.ArrayList;

public class SJustification extends Serializable<Justification> {

    //General info
    public static final int version = 1;
    public static final String modelName = "justification";

    //Fields
    public ArrayList<SDescripteme> descriptemes;

    public SJustification(ObjectSerializer serializer) {
        super(serializer);
    }

    public SJustification(Justification modelReference) {
        super(modelName, version, modelReference);

        descriptemes = new ArrayList<>();
        for(Descripteme d: modelReference.descriptemesProperty()) {
            descriptemes.add(new SDescripteme(d));
        }
    }

    @Override
    protected void addStrategies() {

    }

    @Override
    protected void read() {
        descriptemes = serializer.getArray(serializer.setListSuffix(SDescripteme.modelName), SDescripteme::new);
    }

    @Override
    protected void write(ObjectSerializer serializer) {
        serializer.writeArray(serializer.setListSuffix(SDescripteme.modelName), descriptemes);
    }

    @Override
    protected Justification createModel() {
        Justification j = new Justification();
        for(SDescripteme d: descriptemes)
            j.addDescripteme(d.createModel());
        return j;
    }
}