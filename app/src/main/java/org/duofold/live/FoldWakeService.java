package org.duofold.live;
/** Separate Shizuku component so settings refresh cannot tear down the warm wake connection. */
public final class FoldWakeService extends FoldSettingsService {
 public FoldWakeService(){super();}
}
