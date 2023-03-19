package com.github.zly2006.enclosure.backup;


import net.minecraft.nbt.NbtCompound;

import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class StreamDataOutput extends DataOutputStream {
    /**
     * Creates a new data output stream to write data to the specified
     * underlying output stream. The counter {@code written} is
     * set to zero.
     *
     * @param out the underlying output stream, to be saved for later
     *            use.
     * @see FilterOutputStream#out
     */
    public StreamDataOutput(OutputStream out) {
        super(out);
    }
    public void writeNbt(NbtCompound compound) throws IOException {
        byte[] bytes = new byte[1024 * 1024]; // 1MB
        DataOutputStream dataOutputStream = new StreamDataOutput(new OutputStream() {
            int written = 0;
            @Override
            public void write(int b) throws IOException {
                bytes[written] = (byte) b;
                written++;
            }
        });
        compound.write(dataOutputStream);
        this.writeInt(dataOutputStream.size());
        this.write(bytes, 0, dataOutputStream.size());
    }
}
