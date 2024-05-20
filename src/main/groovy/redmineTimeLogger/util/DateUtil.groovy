package redmineTimeLogger.util

import redmineTimeLogger.Constants

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

final class DateUtil {
  
  static LocalDate convertToLocalDate(Date date) {
    LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault())
  }
  
  static boolean isWorkDay(LocalDate date) {
    !(date.getDayOfWeek() in [DayOfWeek.SATURDAY, DayOfWeek.SUNDAY])
  }
  
  static String getUpdateOnParam(LocalDate startDate, boolean onlyLastWorkday) {
    String operator = onlyLastWorkday ? '=' : '>='
    operator + startDate.format(Constants.DATE_FORMATTER)
  }
  
  static LocalDateTime convertToLocalDateTime(String date) {
    LocalDateTime.parse(date, Constants.DATE_FORMATTER_Z)
  }
  
}
