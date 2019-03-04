package com.rabidgremlin.concord.dao;

import java.util.List;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import com.rabidgremlin.concord.api.UserVoteCount;

public interface StatsDao
{

  /**
   * Returns the raw total count of votes made per user.
   */
  @SqlQuery("SELECT userId, COUNT(*) voteCount " +
      "FROM votes " +
      "GROUP BY userId " +
      "ORDER BY voteCount DESC")
  @RegisterBeanMapper(UserVoteCount.class)
  List<UserVoteCount> getTotalCountOfVotesMadePerUser();

  /*
   * TODO filter out SKIP and TRASH ???? They are considered complete with less than consensus level.
   */

  /**
   * Returns the count of votes made per user, for phrases which have been marked complete with the same label the user
   * voted for.
   */
  @SqlQuery("SELECT userId, COUNT(*) voteCount " +
      "FROM votes JOIN phrases ON phrases.phraseId=votes.phraseId " +
      "WHERE completed = true AND phrases.label=votes.label " +
      "GROUP BY userId " +
      "ORDER BY voteCount DESC")
  @RegisterBeanMapper(UserVoteCount.class)
  List<UserVoteCount> getCompletedCountOfVotesMadePerUser();

  /**
   * Returns the total count of votes made per user, for phrases which have been voted on more or equal times of the
   * margin.
   *
   * @param margin the amount of times a phrase must be voted on to be considered in the query. Recommend setting to the
   *          consensus level.
   */
  @SqlQuery("SELECT userId, COUNT(*) voteCount " +
      "FROM votes " +
      "WHERE phraseId in (SELECT phraseId FROM votes GROUP BY phraseId HAVING COUNT(phraseId) >= :margin) " +
      "GROUP BY userId " +
      "ORDER BY voteCount DESC")
  @RegisterBeanMapper(UserVoteCount.class)
  List<UserVoteCount> getCountOfVotesMadePerUserForPhrasesBeyondVoteMargin(@Bind("margin") int margin);

}
