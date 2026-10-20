package com.fms.domain;

/** Why goods came back (sales) or were sent back (purchases). Texts are in messages_*.properties (reason.ret.*). */
public enum ReturnReason {
    DAMAGED, DEFECTIVE, WRONG_ITEM, NOT_NEEDED, QUALITY, OTHER
}
