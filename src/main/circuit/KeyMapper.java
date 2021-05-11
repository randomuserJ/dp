package main.circuit;

import main.circuit.components.GateType;

public class KeyMapper {
    private final String key;
    private final GateType gate;

    public KeyMapper(String key, GateType gate) {
        this.key = key;
        this.gate = gate;
    }

    public String getKey() {
        return key;
    }

    public GateType getGate() {
        return gate;
    }
}
