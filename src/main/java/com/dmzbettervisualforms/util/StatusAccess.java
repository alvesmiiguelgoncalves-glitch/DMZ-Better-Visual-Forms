package com.dmzbettervisualforms.util;


/** Implementada pelo {@code StatusMixin}: guarda se o Estado de Aura do addon esta ligado (so visual, so no cliente). */
public interface StatusAccess {

    boolean bvf$auraOverride();

    void bvf$setAuraOverride(boolean value);

    static StatusAccess of(Object status) {
        return status instanceof StatusAccess access ? access : null;
    }
}
