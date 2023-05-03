package com.github.zly2006.enclosure.exceptions;

import net.minecraft.text.Text;

public class PermissionTargetException extends RuntimeException {
    final Text text;

    public PermissionTargetException(Text text) {
        super("This permission does not support the given target.");
        this.text = text;
    }

    public Text getText() {
        return text;
    }
}
