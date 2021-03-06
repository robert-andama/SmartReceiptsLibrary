package co.smartreceipts.android.utils;

public enum FeatureFlags implements Feature {

    SmartReceiptsLogin(true), Ocr(true), OrganizationSyncing(false);

    private final boolean isEnabled;

    FeatureFlags(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }
}
