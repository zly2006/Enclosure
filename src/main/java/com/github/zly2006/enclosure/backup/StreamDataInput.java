package com.github.zly2006.enclosure.backup;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtTagSizeTracker;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public class StreamDataInput extends DataInputStream {
    private static final NbtTagSizeTracker SIZE_TRACKER = new NbtTagSizeTracker(1024 * 1024); // 1MB
    /**
     * Creates a DataInputStream that uses the specified
     * underlying InputStream.
     *
     * @param in the specified input stream
     */
    public StreamDataInput(@NotNull InputStream in) {
        super(in);
    }
    public NbtCompound readNbt() throws IOException {
        int length = this.readInt();
        byte[] bytes = new byte[length];
        this.readFully(bytes);
        DataInputStream dataInputStream = new DataInputStream(new InputStream() {
            int read = 0;
            @Override
            public int read() {
                if (read >= length) {
                    return -1;
                }
                else {
                    return bytes[read++];
                }
            }
        });
        return NbtCompound.TYPE.read(dataInputStream, 0, SIZE_TRACKER);
    }
}
