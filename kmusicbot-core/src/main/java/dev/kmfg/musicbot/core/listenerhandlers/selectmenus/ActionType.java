package dev.kmfg.musicbot.core.listenerhandlers.selectmenus;

public enum ActionType {
    NONE('_'),
    ADD_TO_PLAYLIST('A');

    public static final String SEPARATOR = "`";

    public final char value;

    private ActionType(char v) {
        this.value = v;
    }

    public static ActionType fromChar(char v) {
        for(ActionType actionType : ActionType.values()) {
            if(actionType.value == v) {
                return actionType;
            }
        }
        return NONE;
    }
}
