package redmineTimeLogger

import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.RedmineManagerFactory
import com.taskadapter.redmineapi.bean.Issue
import com.taskadapter.redmineapi.bean.TimeEntry
import redmineTimeLogger.domain.RedmineUserActivity
import redmineTimeLogger.domain.Day
import redmineTimeLogger.redmineConnector.IssueConnector
import redmineTimeLogger.redmineConnector.TimeEntryConnector
import redmineTimeLogger.util.DateUtil
import redmineTimeLogger.util.RedmineUserActivityUtil
import redmineTimeLogger.util.StartDateUtil

import java.time.DayOfWeek
import java.time.LocalDate

class RedmineTimeLogger {
  
  private boolean onlyLastWorkday
  private LocalDate startDate
  
  void run(Map<String, Object> config) {
    final RedmineManager redmineManager = RedmineManagerFactory.createWithApiKey(config['uri'] as String, config['apiKey'] as String)
    final Integer currentUserId = redmineManager.userManager.currentUser.id
    final TimeEntryConnector timeEntryConnector = new TimeEntryConnector(redmineManager, currentUserId)
    final IssueConnector issueConnector = new IssueConnector(redmineManager, config['projects'] as List)
    final String startDateParam = config['startDate']
    onlyLastWorkday = !startDateParam
    
    startDate = StartDateUtil.calculateStartDate(startDateParam)
    
    Map<LocalDate, List<TimeEntry>> timeEntriesByDate = timeEntryConnector.processTrackedTimeEntries(startDate)
    List<Day> days = timeEntriesByDate.collect { k, v -> new Day(k, v*.issueId.toSet(), (v*.hours.sum() as Float).round(2))}
    startDate = StartDateUtil.reCalculateStartDate(days, startDate, onlyLastWorkday)
    if (!startDate) {
      return
    }
    
    Map<String, Integer> issueStatuses = issueConnector.getIssueStatusNamesWithId()
    final Set<Integer> excludedIssueStatuses = issueStatuses
        .findAll {(config['excludedIssueStatusesInJournal'] as List).contains(it.key) }.values()
    final Integer newIssueStatus = issueStatuses.get(config['newIssueStatusName'] as String)
    
    IssueProducer issueProducer = new IssueProducer(issueConnector, newIssueStatus)
    List<Issue> issues = issueProducer.getIssues(DateUtil.getUpdateOnParam(startDate, onlyLastWorkday))
    List<RedmineUserActivity> userActivities = RedmineUserActivityUtil.collectUserActivities(issues, startDate, currentUserId, excludedIssueStatuses)
    Map<Integer, Integer> parentIdsByIssueIds = issueProducer.findParentIdsForNewIssues(userActivities.collect { it.issueId }, issues)
    List<RedmineUserActivity> resolvedUserActivities = RedmineUserActivityUtil.moveActivitiesToParentForNewIssues(userActivities, parentIdsByIssueIds)
    
    final Map<String, Integer> activityIdsByName = timeEntryConnector.activityNamesWithId
    Integer generalActivityId = activityIdsByName.get(config['generalActivity'])
    resolvedUserActivities.each { it.activityId = generalActivityId }
    
    resolvedUserActivities.addAll(collectAdditionalUserActivities(config, activityIdsByName))
    
    List<LocalDate> fullTimeDays = days.findAll { it.sum == 7.5 }.collect { it.date }
    def entriesToRecord = resolvedUserActivities.findAll {
      !fullTimeDays.contains(it.date)
          && !days.find { d -> d.date == it.date }?.issues?.contains(it.issueId)
    }
    timeEntryConnector.createTimeEntries(entriesToRecord)
  }
  
  private List<RedmineUserActivity> collectAdditionalUserActivities(Map<String, Object> config, Map<String, Integer> activityIdsByName) {
    (config['additionalActivities'] as List).collectMany { additonalActivity ->
      createAdditionalUserActivity(
          additonalActivity['issue'] as Integer,
          additonalActivity['hours'] as Float,
          activityIdsByName[additonalActivity['activity']] as Integer,
          additonalActivity['day'] as String)
    }
  }
  
  private List<RedmineUserActivity> createAdditionalUserActivity(Integer issueId, Float hours, Integer activityId, String day) {
    LocalDate date = startDate
    LocalDate endDate = onlyLastWorkday ? startDate : LocalDate.now()
    List<RedmineUserActivity> meetingTimeEntries = []
    DayOfWeek dayOfWeek = day ? DayOfWeek.valueOf(day.toUpperCase()) : null
    while (date <= endDate) {
      if (DateUtil.isWorkDay(date) && (!dayOfWeek || date.getDayOfWeek() == dayOfWeek)) {
        RedmineUserActivity userActivity = new RedmineUserActivity(issueId, date, hours, activityId)
        meetingTimeEntries.add(userActivity)
      }
      date = date.plusDays(1)
    }
    meetingTimeEntries
  }
  
}