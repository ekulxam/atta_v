package survivalblock.atmosphere.atta_v.common.entity;

// reference to the ShieldboardEntity from my mod Shield Surfing
public interface ControlBoarder {

    default void setInputs(){
        this.setInputs(false, false, false, false);
    }

    void setInputs(boolean pressingLeft, boolean pressingRight, boolean pressingForward, boolean pressingBack);
}
