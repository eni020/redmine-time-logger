package redmineTimeLogger.redmineConnector

import com.taskadapter.redmineapi.Include
import com.taskadapter.redmineapi.IssueManager
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.Issue
import com.taskadapter.redmineapi.internal.ResultsWrapper

class IssueConnector {
  
  private IssueManager issueManager
  Map<Integer, String> projects
  
  IssueConnector(RedmineManager redmineManager, List<String> projectNames) {
    issueManager = redmineManager.getIssueManager()
    projects = redmineManager.projectManager.projects
        .findAll { projectNames.contains(it.name) }
        .collectEntries { [(it.id): it.name] }
  }
  
  Map<String, Integer> getIssueStatusNamesWithId() {
    issueManager.statuses.collectEntries { [it.name, it.id] }
  }
  
  List<Issue> getDetailedIssuesByIds(List<Integer> issueIds) {
    issueIds.collect {
      issueManager.getIssueById(it, Include.journals, Include.changesets)
    }
  }
  
  Map<Integer, Issue> getIssuesByIds(Collection<Integer> issueIds) {
    if(issueIds.isEmpty()) {
      return [:]
    }
    
    Map params = [
        'issue_id' : issueIds.join(','),
        'status_id': '*',
        "limit"    : issueIds.size() as String
    ]
    issueManager.getIssues(params).results?.collectEntries { issue -> [issue.id, issue] } ?: [:]
  }
  
  List<Integer> getIssueIds(String updatedOn) {
    Map defaultParams = [
        'status_id' : '*',
        'updated_on': updatedOn,
        'limit'     : '100'
    ]
    List<Integer> resultIssueIds = []
    projects.each { projectId, projectName ->
      int offset = 0
      ResultsWrapper<Issue> results
      println("Processing issues in project '${projectName}'...")
      do {
        Map actParams = new HashMap(defaultParams)
        actParams.put('offset', offset as String)
        actParams.put('project_id', projectId as String)
        results = issueManager.getIssues(actParams)
        resultIssueIds.addAll(results.results*.id.asList())
        offset += resultIssueIds.size()
//        println("${offset < results.totalFoundOnServer ? offset : results.totalFoundOnServer} issues processed, total: ${results.totalFoundOnServer}")
      } while (results.totalFoundOnServer > offset)
      println('')
    }
    resultIssueIds
  }
}
