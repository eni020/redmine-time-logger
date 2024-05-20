package redmineTimeLogger

import java.time.format.DateTimeFormatter

class Constants {
  static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern('yyyy-MM-dd')
  static DateTimeFormatter DATE_FORMATTER_Z = DateTimeFormatter.ofPattern(/yyyyMMdd'T'HHmmss/)
}
