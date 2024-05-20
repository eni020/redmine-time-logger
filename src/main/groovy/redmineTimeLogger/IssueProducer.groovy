package redmineTimeLogger

import com.taskadapter.redmineapi.bean.Issue
import redmineTimeLogger.redmineConnector.IssueConnector

class IssueProducer {
  
  private IssueConnector issueConnector
  private Integer newIssueStatus
  
  IssueProducer(IssueConnector issueConnector, final Integer newIssueStatus) {
    this.issueConnector = issueConnector
    this.newIssueStatus = newIssueStatus
  }
  
  List<Issue> getIssues(String updatedOn) {
    List<Integer> issueIds = issueConnector.getIssueIds(updatedOn)
    issueConnector.getDetailedIssuesByIds(issueIds)
  }
  
  Map<Integer, Integer> findParentIdsForNewIssues(List<Integer> issueIds, List<Issue> issues) {
    Map<Integer, Issue> issuesById = issues.collectEntries { it -> [it.id, it] }
    Map<Integer, Integer> issueIdMap = issueIds
        .collect { issuesById.get(it) }
        .findAll { it.statusId == newIssueStatus }
        .collectEntries { issue -> [issue.id, issue.id] }
    if (issueIdMap.isEmpty()) {
      return [:]
    }
    findParentIds(issueIdMap, issuesById, [:])
  }
  
  private Map<Integer, Integer> findParentIds(Map<Integer, Integer> parentIdsByIssueId,
                                              Map<Integer, Issue> issues,
                                              Map<Integer, Integer> foundParentIdsByIssueId) {
    if (parentIdsByIssueId.isEmpty()) {
      return foundParentIdsByIssueId
    }
    Map<Integer, Issue> issuesById = new HashMap<>(issues)
    
    issuesById.putAll(issueConnector.getIssuesByIds(parentIdsByIssueId.values().findAll { !issuesById.containsKey(it) }))
    
    Map<Integer, Integer> mapToProcess = [:]
    parentIdsByIssueId
        .findAll { issueId, parentId -> issuesById.get(parentId)?.statusId == newIssueStatus }
        .each { issueId, parentId ->
          {
            Integer parentParentId = getParent(issuesById, issuesById.get(parentId).parentId)
            if (!parentParentId) {
              foundParentIdsByIssueId.put(issueId, parentId)
            } else if (issuesById.containsKey(parentParentId)) {
              foundParentIdsByIssueId.put(issueId, parentParentId)
            } else {
              mapToProcess.put(issueId, parentParentId)
            }
          }
        }
    findParentIds(mapToProcess, issuesById, foundParentIdsByIssueId)
  }
  
  private Integer getParent(Map<Integer, Issue> issues, Integer parentId) {
    Issue parentIssue = issues.get(parentId)
    if (!parentIssue || parentIssue.statusId != newIssueStatus || !parentIssue.parentId) {
      return parentId
    } else {
      getParent(issues, parentIssue.parentId)
    }
  }
}