package com.rabidgremlin.concord.resources;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableList;
import com.rabidgremlin.concord.api.LabelCount;
import com.rabidgremlin.concord.api.LabelCountStats;
import com.rabidgremlin.concord.api.SystemStats;
import com.rabidgremlin.concord.api.UserStats;
import com.rabidgremlin.concord.api.UserVoteCount;
import com.rabidgremlin.concord.auth.Caller;
import com.rabidgremlin.concord.dao.SystemStatsDao;
import com.rabidgremlin.concord.dao.UserStatsDao;

import io.dropwizard.auth.Auth;
import io.swagger.annotations.ApiParam;

@PermitAll
@Path("stats")
@Produces(MediaType.APPLICATION_JSON)
public class StatsResource
{

  private final UserStatsDao userStatsDao;

  private final SystemStatsDao systemStatsDao;

  private final int consensusLevel;

  private final Logger log = LoggerFactory.getLogger(StatsResource.class);

  private static final List<String> EXTRA_LABELS = ImmutableList.of("TRASH", "SKIPPED");

  public StatsResource(UserStatsDao userStatsDao, SystemStatsDao systemStatsDao, int consensusLevel)
  {
    this.userStatsDao = userStatsDao;
    this.systemStatsDao = systemStatsDao;
    this.consensusLevel = consensusLevel;
  }

  private int getVotesForUser(List<UserVoteCount> list, String userId)
  {
    return list.stream()
        .filter(userVoteCount -> userVoteCount.getUserId().equals(userId))
        .findFirst()
        .map(UserVoteCount::getVoteCount)
        .orElse(0);
  }

  @GET
  @Timed
  @Path("/user")
  public Response getUserStats(@ApiParam(hidden = true) @Auth Caller caller)
  {
    log.info("{} getting user stats.", caller);

    List<UserVoteCount> totalVoteCounts = userStatsDao.getCountOfTotalVotesPerUser();
    List<UserVoteCount> completedVoteCounts = userStatsDao.getCountOfCompletedVotesPerUser();
    List<UserVoteCount> trashedVoteCounts = userStatsDao.getCountOfTrashVotesPerUser();
    List<UserVoteCount> totalVoteCountsForPhrasesWithConsensus = userStatsDao.getCountOfTotalVotesWithConsensusPerUser();
    List<UserVoteCount> completedVoteCountsIgnoringTrash = userStatsDao.getCountOfCompletedVotesPerUserIgnoringTrash();
    List<UserVoteCount> totalVotesForPhrasesWithConsensusIgnoringTrash = userStatsDao
        .getCountOfTotalVotesWithConsensusPerUserIgnoringTrash();

    List<UserStats> userStats = totalVoteCounts.stream()
        // only include users who have voted
        .filter(userVoteCount -> userVoteCount.getVoteCount() > 0)
        .map(totalVoteCount -> {
          String userId = totalVoteCount.getUserId();
          int total = totalVoteCount.getVoteCount();
          int completed = getVotesForUser(completedVoteCounts, userId);
          int trashed = getVotesForUser(trashedVoteCounts, userId);
          int totalWithConsensus = getVotesForUser(totalVoteCountsForPhrasesWithConsensus, userId);
          int completedIgnoringTrash = getVotesForUser(completedVoteCountsIgnoringTrash, userId);
          int totalWithConsensusIgnoringTrash = getVotesForUser(totalVotesForPhrasesWithConsensusIgnoringTrash, userId);
          return new UserStats(userId, total, completed, trashed, totalWithConsensus, completedIgnoringTrash, totalWithConsensusIgnoringTrash);
        })
        .collect(Collectors.toList());

    return Response.ok().entity(userStats).build();
  }

  private int getCountsForLabel(List<LabelCount> labelCounts, String label)
  {
    return labelCounts.stream()
        .filter(labelCount -> labelCount.getLabel().equals(label))
        .findFirst()
        .map(LabelCount::getCount)
        .orElse(0);
  }

  @GET
  @Timed
  @Path("/system")
  public Response getSystemStats(@ApiParam(hidden = true) @Auth Caller caller)
  {
    log.info("{} getting system stats.", caller);

    int totalPhrases = systemStatsDao.getTotalCountOfPhrases();
    int completedPhrases = systemStatsDao.getCountOfCompletedPhrases();
    int phrasesWithConsensus = systemStatsDao.getCountOfPhrasesWithConsensus(consensusLevel);
    int phrasesWithConsensusNotCompleted = systemStatsDao.getCountOfPhrasesWithConsensusThatAreNotCompleted(consensusLevel);
    int labelsUsed = systemStatsDao.getCountOfLabelsUsed();
    int totalVotes = systemStatsDao.getCountOfVotes();
    int totalLabels = systemStatsDao.getCountOfLabels();
    int userCount = systemStatsDao.getCountOfUsers();

    List<String> labelNames = systemStatsDao.getLabelNames();
    labelNames.addAll(EXTRA_LABELS);
    List<LabelCount> labelVoteCounts = systemStatsDao.getLabelVoteCounts();
    List<LabelCount> labelCompletedPhraseCounts = systemStatsDao.getCompletedPhraseLabelCounts();
    List<LabelCountStats> labelCountStats = labelNames.stream()
        .map(label -> {
          int voteCount = getCountsForLabel(labelVoteCounts, label);
          int completedPhraseCount = getCountsForLabel(labelCompletedPhraseCounts, label);
          return new LabelCountStats(label, voteCount, completedPhraseCount);
        })
        .filter(label -> label.getVoteCount() > 0 || label.getCompletedPhraseCount() > 0)
        .collect(Collectors.toList());

    SystemStats systemStats = new SystemStats(totalPhrases, completedPhrases, phrasesWithConsensus, phrasesWithConsensusNotCompleted, labelsUsed, totalVotes,
        totalLabels, userCount, labelCountStats);

    return Response.ok().entity(systemStats).build();
  }

}
