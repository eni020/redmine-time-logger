package redmineTimeLogger


import groovy.json.JsonSlurper

class Main {
  
  static void main(String[] args) {
    if (args.size() < 1) {
      throw new IllegalArgumentException('Needs redmine and user config files, or the common directory')
    }
    
    final Map<String, Object> config = parseArgs(args)

//    CalendarEventCollector calendarEventCollector = new CalendarEventCollector()
//    calendarEventCollector.collect(args[0] + '\\', 'hubert.annamaria@tigra.hu', '.ical.zip')
    RedmineTimeLogger redmineTimeLogger = new RedmineTimeLogger()
    redmineTimeLogger.run(config)
  }
  
  static Map<String, Object> parseArgs(String[] args) {
    final File redmineConfigFile
    final File userConfigFile
    if(args.size() == 1) {
      redmineConfigFile = new File(args[0], 'redmine-config.json')
      userConfigFile = new File(args[0], 'user-config.json')
    } else {
      redmineConfigFile = new File(args[0])
      userConfigFile = new File(args[1])
    }
    final Map<String, Object> config = [:]
    config.putAll(new JsonSlurper().parse(redmineConfigFile) as Map)
    config.putAll(new JsonSlurper().parse(userConfigFile) as Map)
    config
  }
}