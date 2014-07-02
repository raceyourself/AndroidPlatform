package com.raceyourself.raceyourself.home;

import java.math.BigDecimal;
import java.util.Calendar;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 27/06/2014.
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper=true)
public class DurationChallengeBean extends ChallengeBean {
    private Calendar duration;
    private double distanceMetres;
}
