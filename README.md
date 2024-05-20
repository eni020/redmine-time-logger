# Redmine Time Logger

Command-line tool to create time entries in Redmine based on Redmine issue's journal activity and changesets. Written in Groovy, runnable with Java 17.

## How to run:
After git clone, run mvn clean package in the root directory.

`java -jar "<path to root dir>\target\redmine-time-logger-1.0-SNAPSHOT.jar" "<path to redmine-config.json>" "<path to user-config.json>"`

redmine-config.json example:
```
{
  "uri": "https://www.redmine.org/",
  "newIssueStatusName": "CustomNewStatusName",
  "excludedIssueStatusesInJournal": [
    "Rejected",
    "Closed"
  ],
  "activities": [
    {
      "Meeting": "CustomMeetingName"
    }
  ]
}
```

user-config.json example:
```
{
  "apiKey": "<your own API key>",
  "projects": [
    "MyProject1",
    "MyProject2"
  ],
  "startDate": "2024-05-20",
  "generalActivity": "Development",
  "additionalActivities": [
    {
      "issue": "123456",
      "hours": "0.5",
      "activity": "CustomMeetingName"
    },
	{
      "issue": "789012",
      "hours": "2",
      "activity": "Education",
	  "day": "friday"
    }
  ]
}
```
