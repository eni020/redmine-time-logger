package redmineTimeLogger.util

import com.taskadapter.redmineapi.bean.Issue
import com.taskadapter.redmineapi.bean.JournalDetail
import redmineTimeLogger.domain.RedmineUserActivity

import java.time.LocalDate

final class RedmineUserActivityUtil {
  
  static List<RedmineUserActivity> collectUserActivities(Collection<Issue> issues, LocalDate startDate, Integer currentUserId, Set<Integer> excludedIssueStatuses) {
    List<RedmineUserActivity> userActivities = []
    issues.each { issue ->
      Map<LocalDate, Integer> changesetsByDate = getChangesetCountByDate(issue, startDate, currentUserId)
      Map<LocalDate, Integer> journalCountByDate = getJournalCountByDate(issue, startDate, currentUserId, excludedIssueStatuses)
      changesetsByDate.each { date, count ->
        {
          final Integer journalCount = journalCountByDate.get(date)
          if (!journalCount || journalCount < count) {
            journalCountByDate.put(date, count)
          }
        }
      }

      journalCountByDate.each { date, count ->
        userActivities.add(new RedmineUserActivity(issue.id, date, count / 4))
      }
    }
    userActivities
  }
  
  static List<RedmineUserActivity> moveActivitiesToParentForNewIssues(Collection<RedmineUserActivity> userActivities, Map<Integer, Integer> parentIdsByIssueId) {
    if(parentIdsByIssueId.isEmpty()) {
      return userActivities
    }
    
    List<RedmineUserActivity> resolvedUserActivities = []
    userActivities.each { userActivity ->
      if (parentIdsByIssueId.containsKey(userActivity.issueId)) {
        userActivity.issueId = parentIdsByIssueId.get(userActivity.issueId)
      } else {
        resolvedUserActivities.add(userActivity)
      }
    }
    
    userActivities.findAll { a -> parentIdsByIssueId.containsValue(a.issueId) }
        .groupBy { it.date }.each { date, act ->
          act.groupBy { it.issueId }.each { parent, val ->
            resolvedUserActivities.add(new RedmineUserActivity(parent, date, val*.hours.sum() as Float))
          }
    }
    resolvedUserActivities
  }
  
  private static Map<LocalDate, Integer> getChangesetCountByDate(Issue issue, LocalDate startDate, Integer currentUserId) {
    issue.changesets
        .findAll { c -> c.user?.id == currentUserId}
        .countBy { c -> DateUtil.convertToLocalDate(c.committedOn) }
        .findAll { date, count -> !startDate.isAfter(date) }
  }
  
  private static Map<LocalDate, Integer> getJournalCountByDate(Issue issue, LocalDate startDate, Integer currentUserId, Set<Integer> excludedIssueStatuses) {
    issue.journals
        .findAll { j -> j.user.id == currentUserId
            && !j.details.isEmpty()
            && !anyDetailIsExcludedStatusOrRelation(j.details, excludedIssueStatuses)
            && !journalIsOnlyAboutAssigningToMe(j.details, currentUserId) }
        .countBy { j -> DateUtil.convertToLocalDate(j.createdOn) }
        .findAll { date, count -> !startDate.isAfter(date) }
  }
  
  private static boolean anyDetailIsExcludedStatusOrRelation(List<JournalDetail> details, Set<Integer> excludedIssueStatuses) {
    details.any { d ->
      (d.name == 'status_id' && excludedIssueStatuses.contains(d.newValue as Integer))
          || d.name == 'relates'
    }
  }
  
  private static boolean journalIsOnlyAboutAssigningToMe(List<JournalDetail> details, Integer currentUserId) {
    if (details.size() != 1 || details[0].name != 'assigned_to_id') {
      return false
    }
    JournalDetail d = details[0]
    !d.oldValue && d.newValue == currentUserId as String
  }
}
