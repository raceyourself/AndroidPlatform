package com.raceyourself.raceyourself.home;

import android.content.Context;

import java.util.Set;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 27/06/2014.
 */
@Slf4j
@Data
public abstract class ChallengeBean {
    private Set<ChallengeAttemptBean> attempts;

    public abstract String getName(Context context);
}
