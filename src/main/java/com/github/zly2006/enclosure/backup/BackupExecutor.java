package com.github.zly2006.enclosure.backup;

import java.io.IOException;

public interface BackupExecutor {
    void doBackup(int steps) throws IOException;
    void doRollback(int steps) throws IOException;
    int totalSteps();
    boolean isFinished();
}
