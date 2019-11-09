package tinker.sample.android.eventsmodel;

public class MessageEvent {

    private boolean isPatchInstalled = false;

    public MessageEvent(boolean isPatchInstalled) {
        this.isPatchInstalled = isPatchInstalled;
    }

    public boolean isPatchInstalled() {
        return isPatchInstalled;
    }

    public void setPatchInstalled(boolean patchInstalled) {
        isPatchInstalled = patchInstalled;
    }
}