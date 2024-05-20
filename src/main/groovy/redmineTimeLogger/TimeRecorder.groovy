package redmineTimeLogger


import redmineTimeLogger.redmineConnector.TimeEntryConnector

class TimeRecorder {
  
  private TimeEntryConnector timeEntryConnector
  
  TimeRecorder(TimeEntryConnector timeEntryConnector) {
    this.timeEntryConnector = timeEntryConnector
  }
  
  
  
}