package org.example.moliyaapp.enums;

public enum Months {
    YANVAR,
    FEVRAL,
    MART,
    APREL,
    MAY,
    IYUN,
    IYUL,
    AVGUST,
    SENTABR,
    OKTABR,
    NOYABR,
    DEKABR;

    public Months nextMonth(){
        return values()[(this.ordinal()+1)%values().length];
    }
    public Months lastMonth(){
        return values()[(this.ordinal() - 1 + values().length) % values().length];
    }

}
