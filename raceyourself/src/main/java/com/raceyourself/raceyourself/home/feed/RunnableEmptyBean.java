package com.raceyourself.raceyourself.home.feed;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class RunnableEmptyBean extends ChallengeNotificationBean implements HomeFeedRowBean {
}
