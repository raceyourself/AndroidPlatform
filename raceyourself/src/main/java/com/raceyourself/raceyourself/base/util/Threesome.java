package com.raceyourself.raceyourself.base.util;

import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 17/07/2014.
 */
@Slf4j
@EqualsAndHashCode
public class Threesome<F,S,T> {
    public F first;
    public S second;
    public T third;

    public Threesome(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public static <A,B,C> Threesome<A,B,C> create(A first, B second, C third) {
        return new Threesome<A, B, C>(first, second, third);
    }
}
