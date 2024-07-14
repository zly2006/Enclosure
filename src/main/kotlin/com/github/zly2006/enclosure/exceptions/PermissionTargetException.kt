package com.github.zly2006.enclosure.exceptions

import net.minecraft.text.Text

class PermissionTargetException(val text: Text)
    : RuntimeException("This permission does not support the given target.")
