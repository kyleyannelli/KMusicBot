package Exceptions;

public class AlreadyAccessedException extends Exception {
    public AlreadyAccessedException() {
        super("Value was already accessed! SingleUse object may only be accessed once!");
    }
}
