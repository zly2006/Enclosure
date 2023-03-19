package com.github.zly2006.enclosure.backup;

import com.github.zly2006.enclosure.EnclosureArea;
import org.jetbrains.annotations.Nullable;

public class BackupTask {
    EnclosureArea area;
    public final BackupExecutor executor;
    @Nullable
    StreamDataInput dataInput;
    @Nullable
    StreamDataOutput dataOutput;
    boolean isRollback;
    int maxStepPerTick;
    int currentTickExecuted = 0;
    public int totalExecuted = 0;

    public BackupTask(EnclosureArea area, BackupExecutor executor, StreamDataInput dataInput, StreamDataOutput dataOutput, boolean isRollback, int maxStepPerTick) {
        this.area = area;
        this.executor = executor;
        this.dataInput = dataInput;
        this.dataOutput = dataOutput;
        this.isRollback = isRollback;
        this.maxStepPerTick = maxStepPerTick;
    }

    public void tick() {
        currentTickExecuted = 0;
        try {
            while (!executor.isFinished() && currentTickExecuted < maxStepPerTick) {
                if (isRollback) {
                    executor.doRollback(1);
                } else {
                    executor.doBackup(1);
                }
                currentTickExecuted++;
                totalExecuted++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            area.unlock();
        }
    }
}
