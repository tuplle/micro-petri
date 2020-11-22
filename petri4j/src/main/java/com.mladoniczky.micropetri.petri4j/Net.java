package com.mladoniczky.micropetri.petri4j;

import com.mladoniczky.micropetri.petri4j.arc.Arc;
import com.mladoniczky.micropetri.petri4j.arc.input.InputArc;
import com.mladoniczky.micropetri.petri4j.arc.output.OutputArc;
import com.mladoniczky.micropetri.petri4j.place.Place;
import com.mladoniczky.micropetri.petri4j.transition.Transition;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Document
public class Net {

    @Id
    private ObjectId id;
    private String name;

    private Map<String, Place> places;
    private Map<String, Transition> transitions;
    private Map<String, Arc> arcs;

    @Transient
    @Setter(AccessLevel.NONE)
    private boolean executable;

    public Net() {
        places = new HashMap<>();
        transitions = new HashMap<>();
        arcs = new HashMap<>();
        executable = false;
    }

    public Net(String name) {
        this();
        this.name = name;
    }

    public void addArc(Arc arc) {
        arcs.put(arc.getId(), arc);
    }

    public void addTransition(Transition transition) {
        transitions.put(transition.getId(), transition);
    }

    public void addPlace(Place place) {
        places.put(place.getId(), place);
    }

    public String getStringId() {
        return id.toString();
    }

    public void setId(String id) {
        this.id = new ObjectId(id);
    }

    public long[] getMarking() {
        return places.values().stream().mapToLong(Place::getResources).toArray();
    }

    public List<Transition> getEnabledTransitions() {
        return transitions.values().stream().filter(Transition::isEnable).collect(Collectors.toList());
    }

    public long[] fireTransition(String id) {
        if (!transitions.containsKey(id))
            throw new IllegalArgumentException("Cannot find transition with id " + id);
        return fireTransition(transitions.get(id));
    }

    public long[] fireTransition(Transition transition) {
        assert transitions.containsKey(transition.getId());
        if (!executable)
            makeExecutable();
        testState();
        transition.consume();
        testState();
        transition.produce();
        return testState();
    }

    public long[] testState() {
        transitions.forEach((id, transitions) -> transitions.test());
        return getMarking();
    }

    public void makeExecutable() {
        if (executable) return;
        arcs.forEach((id, arc) -> {
            if (arc instanceof InputArc) {
                ((Place) arc.getSource()).addOutputArc((InputArc) arc);
                ((Transition) arc.getTarget()).addInputArc((InputArc) arc);
            } else if (arc instanceof OutputArc) {
                ((Transition) arc.getSource()).addOutputArc((OutputArc) arc);
                ((Place) arc.getTarget()).addInputArc((OutputArc) arc);
            }
        });
        executable = true;
    }
}
