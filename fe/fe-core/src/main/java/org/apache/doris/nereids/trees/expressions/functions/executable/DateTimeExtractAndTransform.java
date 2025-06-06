// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.nereids.trees.expressions.functions.executable;

import org.apache.doris.catalog.ScalarType;
import org.apache.doris.catalog.Type;
import org.apache.doris.nereids.exceptions.AnalysisException;
import org.apache.doris.nereids.rules.expression.rules.SupportJavaDateFormatter;
import org.apache.doris.nereids.trees.expressions.ExecFunction;
import org.apache.doris.nereids.trees.expressions.Expression;
import org.apache.doris.nereids.trees.expressions.literal.BigIntLiteral;
import org.apache.doris.nereids.trees.expressions.literal.BooleanLiteral;
import org.apache.doris.nereids.trees.expressions.literal.DateLiteral;
import org.apache.doris.nereids.trees.expressions.literal.DateTimeLiteral;
import org.apache.doris.nereids.trees.expressions.literal.DateTimeV2Literal;
import org.apache.doris.nereids.trees.expressions.literal.DateV2Literal;
import org.apache.doris.nereids.trees.expressions.literal.DecimalV3Literal;
import org.apache.doris.nereids.trees.expressions.literal.DoubleLiteral;
import org.apache.doris.nereids.trees.expressions.literal.IntegerLiteral;
import org.apache.doris.nereids.trees.expressions.literal.NullLiteral;
import org.apache.doris.nereids.trees.expressions.literal.SmallIntLiteral;
import org.apache.doris.nereids.trees.expressions.literal.StringLikeLiteral;
import org.apache.doris.nereids.trees.expressions.literal.StringLiteral;
import org.apache.doris.nereids.trees.expressions.literal.TinyIntLiteral;
import org.apache.doris.nereids.trees.expressions.literal.VarcharLiteral;
import org.apache.doris.nereids.types.DataType;
import org.apache.doris.nereids.types.DateTimeV2Type;
import org.apache.doris.nereids.types.DateType;
import org.apache.doris.nereids.types.DateV2Type;
import org.apache.doris.nereids.types.DecimalV3Type;
import org.apache.doris.nereids.types.VarcharType;
import org.apache.doris.nereids.util.DateUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.Locale;

/**
 * executable function:
 * year, quarter, month, week, dayOfYear, dayOfweek, dayOfMonth, hour, minute, second, microsecond
 */
public class DateTimeExtractAndTransform {

    private static final HashMap<String, Integer> DAY_OF_WEEK = new HashMap<>();

    static {
        DAY_OF_WEEK.put("MO", 1);
        DAY_OF_WEEK.put("MON", 1);
        DAY_OF_WEEK.put("MONDAY", 1);
        DAY_OF_WEEK.put("TU", 2);
        DAY_OF_WEEK.put("TUE", 2);
        DAY_OF_WEEK.put("TUESDAY", 2);
        DAY_OF_WEEK.put("WE", 3);
        DAY_OF_WEEK.put("WED", 3);
        DAY_OF_WEEK.put("WEDNESDAY", 3);
        DAY_OF_WEEK.put("TH", 4);
        DAY_OF_WEEK.put("THU", 4);
        DAY_OF_WEEK.put("THURSDAY", 4);
        DAY_OF_WEEK.put("FR", 5);
        DAY_OF_WEEK.put("FRI", 5);
        DAY_OF_WEEK.put("FRIDAY", 5);
        DAY_OF_WEEK.put("SA", 6);
        DAY_OF_WEEK.put("SAT", 6);
        DAY_OF_WEEK.put("SATURDAY", 6);
        DAY_OF_WEEK.put("SU", 7);
        DAY_OF_WEEK.put("SUN", 7);
        DAY_OF_WEEK.put("SUNDAY", 7);
    }

    /**
     * datetime arithmetic function date-v2
     */
    @ExecFunction(name = "datev2")
    public static Expression dateV2(DateTimeV2Literal dateTime) {
        return new DateV2Literal(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay());
    }

    /**
     * Executable datetime extract year
     */
    @ExecFunction(name = "year")
    public static Expression year(DateLiteral date) {
        return new SmallIntLiteral(((short) date.getYear()));
    }

    @ExecFunction(name = "year")
    public static Expression year(DateTimeLiteral date) {
        return new SmallIntLiteral(((short) date.getYear()));
    }

    @ExecFunction(name = "year")
    public static Expression year(DateV2Literal date) {
        return new SmallIntLiteral(((short) date.getYear()));
    }

    @ExecFunction(name = "year")
    public static Expression year(DateTimeV2Literal date) {
        return new SmallIntLiteral(((short) date.getYear()));
    }

    /**
     * Executable datetime extract quarter
     */
    @ExecFunction(name = "quarter")
    public static Expression quarter(DateLiteral date) {
        return new TinyIntLiteral((byte) (((byte) date.getMonth() - 1) / 3 + 1));
    }

    @ExecFunction(name = "quarter")
    public static Expression quarter(DateTimeLiteral date) {
        return new TinyIntLiteral((byte) ((date.getMonth() - 1) / 3 + 1));
    }

    @ExecFunction(name = "quarter")
    public static Expression quarter(DateV2Literal date) {
        return new TinyIntLiteral((byte) ((date.getMonth() - 1) / 3 + 1));
    }

    @ExecFunction(name = "quarter")
    public static Expression quarter(DateTimeV2Literal date) {
        return new TinyIntLiteral((byte) ((date.getMonth() - 1) / 3 + 1));
    }

    /**
     * Executable datetime extract month
     */
    @ExecFunction(name = "month")
    public static Expression month(DateLiteral date) {
        return new TinyIntLiteral((byte) date.getMonth());
    }

    @ExecFunction(name = "month")
    public static Expression month(DateTimeLiteral date) {
        return new TinyIntLiteral((byte) date.getMonth());
    }

    @ExecFunction(name = "month")
    public static Expression month(DateV2Literal date) {
        return new TinyIntLiteral((byte) date.getMonth());
    }

    @ExecFunction(name = "month")
    public static Expression month(DateTimeV2Literal date) {
        return new TinyIntLiteral((byte) date.getMonth());
    }

    /**
     * Executable datetime extract day
     */
    @ExecFunction(name = "day")
    public static Expression day(DateLiteral date) {
        return new TinyIntLiteral((byte) date.getDay());
    }

    @ExecFunction(name = "day")
    public static Expression day(DateTimeLiteral date) {
        return new TinyIntLiteral((byte) date.getDay());
    }

    @ExecFunction(name = "day")
    public static Expression day(DateV2Literal date) {
        return new TinyIntLiteral((byte) date.getDay());
    }

    @ExecFunction(name = "day")
    public static Expression day(DateTimeV2Literal date) {
        return new TinyIntLiteral((byte) date.getDay());
    }

    /**
     * Executable datetime extract hour
     */
    @ExecFunction(name = "hour")
    public static Expression hour(DateTimeLiteral date) {
        return new TinyIntLiteral(((byte) date.getHour()));
    }

    @ExecFunction(name = "hour")
    public static Expression hour(DateTimeV2Literal date) {
        return new TinyIntLiteral(((byte) date.getHour()));
    }

    /**
     * Executable datetime extract hour
     */
    @ExecFunction(name = "minute")
    public static Expression minute(DateTimeLiteral date) {
        return new TinyIntLiteral(((byte) date.getMinute()));
    }

    @ExecFunction(name = "minute")
    public static Expression minute(DateTimeV2Literal date) {
        return new TinyIntLiteral(((byte) date.getMinute()));
    }

    /**
     * Executable datetime extract second
     */
    @ExecFunction(name = "second")
    public static Expression second(DateTimeLiteral date) {
        return new TinyIntLiteral(((byte) date.getSecond()));
    }

    @ExecFunction(name = "second")
    public static Expression second(DateTimeV2Literal date) {
        return new TinyIntLiteral(((byte) date.getSecond()));
    }

    /**
     * Executable datetime extract microsecond
     */
    @ExecFunction(name = "microsecond")
    public static Expression microsecond(DateTimeV2Literal date) {
        return new IntegerLiteral(((int) date.getMicroSecond()));
    }

    /**
     * Executable datetime extract dayofyear
     */
    @ExecFunction(name = "dayofyear")
    public static Expression dayOfYear(DateLiteral date) {
        return new SmallIntLiteral((short) date.getDayOfYear());
    }

    @ExecFunction(name = "dayofyear")
    public static Expression dayOfYear(DateTimeLiteral date) {
        return new SmallIntLiteral((short) date.getDayOfYear());
    }

    @ExecFunction(name = "dayofyear")
    public static Expression dayOfYear(DateV2Literal date) {
        return new SmallIntLiteral((short) date.getDayOfYear());
    }

    @ExecFunction(name = "dayofyear")
    public static Expression dayOfYear(DateTimeV2Literal date) {
        return new SmallIntLiteral((short) date.getDayOfYear());
    }

    /**
     * Executable datetime extract dayofmonth
     */
    @ExecFunction(name = "dayofmonth")
    public static Expression dayOfMonth(DateLiteral date) {
        return new TinyIntLiteral((byte) date.toJavaDateType().getDayOfMonth());
    }

    @ExecFunction(name = "dayofmonth")
    public static Expression dayOfMonth(DateTimeLiteral date) {
        return new TinyIntLiteral((byte) date.toJavaDateType().getDayOfMonth());
    }

    @ExecFunction(name = "dayofmonth")
    public static Expression dayOfMonth(DateV2Literal date) {
        return new TinyIntLiteral((byte) date.toJavaDateType().getDayOfMonth());
    }

    @ExecFunction(name = "dayofmonth")
    public static Expression dayOfMonth(DateTimeV2Literal date) {
        return new TinyIntLiteral((byte) date.toJavaDateType().getDayOfMonth());
    }

    /**
     * Executable datetime extract dayofweek
     */
    @ExecFunction(name = "dayofweek")
    public static Expression dayOfWeek(DateLiteral date) {
        return new TinyIntLiteral((byte) (date.getDayOfWeek() % 7 + 1));
    }

    @ExecFunction(name = "dayofweek")
    public static Expression dayOfWeek(DateTimeLiteral date) {
        return new TinyIntLiteral((byte) (date.getDayOfWeek() % 7 + 1));
    }

    @ExecFunction(name = "dayofweek")
    public static Expression dayOfWeek(DateV2Literal date) {
        return new TinyIntLiteral((byte) (date.getDayOfWeek() % 7 + 1));
    }

    @ExecFunction(name = "dayofweek")
    public static Expression dayOfWeek(DateTimeV2Literal date) {
        return new TinyIntLiteral((byte) (date.getDayOfWeek() % 7 + 1));
    }

    private static int distanceToFirstDayOfWeek(LocalDateTime dateTime) {
        return dateTime.getDayOfWeek().getValue() - 1;
    }

    private static LocalDateTime firstDayOfWeek(LocalDateTime dateTime) {
        return dateTime.plusDays(-distanceToFirstDayOfWeek(dateTime));
    }

    /**
     * datetime arithmetic function date-format
     */
    @ExecFunction(name = "date_format")
    public static Expression dateFormat(DateLiteral date, StringLikeLiteral format) {
        format = (StringLikeLiteral) SupportJavaDateFormatter.translateJavaFormatter(format);
        return new VarcharLiteral(DateUtils.dateTimeFormatter(format.getValue()).format(
                java.time.LocalDate.of(((int) date.getYear()), ((int) date.getMonth()), ((int) date.getDay()))));
    }

    @ExecFunction(name = "date_format")
    public static Expression dateFormat(DateTimeLiteral date, StringLikeLiteral format) {
        format = (StringLikeLiteral) SupportJavaDateFormatter.translateJavaFormatter(format);
        return new VarcharLiteral(DateUtils.dateTimeFormatter(format.getValue()).format(
                java.time.LocalDateTime.of(((int) date.getYear()), ((int) date.getMonth()), ((int) date.getDay()),
                        ((int) date.getHour()), ((int) date.getMinute()), ((int) date.getSecond()))));
    }

    @ExecFunction(name = "date_format")
    public static Expression dateFormat(DateV2Literal date, StringLikeLiteral format) {
        format = (StringLikeLiteral) SupportJavaDateFormatter.translateJavaFormatter(format);
        return new VarcharLiteral(DateUtils.dateTimeFormatter(format.getValue()).format(
                java.time.LocalDate.of(((int) date.getYear()), ((int) date.getMonth()), ((int) date.getDay()))));
    }

    @ExecFunction(name = "date_format")
    public static Expression dateFormat(DateTimeV2Literal date, StringLikeLiteral format) {
        format = (StringLikeLiteral) SupportJavaDateFormatter.translateJavaFormatter(format);
        return new VarcharLiteral(DateUtils.dateTimeFormatter(format.getValue()).format(
                java.time.LocalDateTime.of(((int) date.getYear()), ((int) date.getMonth()), ((int) date.getDay()),
                        ((int) date.getHour()), ((int) date.getMinute()), ((int) date.getSecond()))));
    }

    /**
     * datetime arithmetic function date
     */
    @ExecFunction(name = "date")
    public static Expression date(DateTimeLiteral dateTime) throws AnalysisException {
        return new DateLiteral(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay());
    }

    @ExecFunction(name = "date")
    public static Expression date(DateTimeV2Literal dateTime) throws AnalysisException {
        return new DateV2Literal(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay());
    }

    /**
     * datetime arithmetic function date-trunc
     */
    @ExecFunction(name = "date_trunc")
    public static Expression dateTrunc(DateTimeLiteral date, StringLikeLiteral trunc) {
        return DateTimeLiteral.fromJavaDateType(dateTruncHelper(date.toJavaDateType(), trunc.getValue()));
    }

    @ExecFunction(name = "date_trunc")
    public static Expression dateTrunc(DateTimeV2Literal date, StringLikeLiteral trunc) {
        return DateTimeV2Literal.fromJavaDateType(dateTruncHelper(date.toJavaDateType(), trunc.getValue()));
    }

    @ExecFunction(name = "date_trunc")
    public static Expression dateTrunc(DateLiteral date, StringLikeLiteral trunc) {
        return DateLiteral.fromJavaDateType(dateTruncHelper(date.toJavaDateType(), trunc.getValue()));
    }

    @ExecFunction(name = "date_trunc")
    public static Expression dateTrunc(DateV2Literal date, StringLikeLiteral trunc) {
        return DateV2Literal.fromJavaDateType(dateTruncHelper(date.toJavaDateType(), trunc.getValue()));
    }

    @ExecFunction(name = "date_trunc")
    public static Expression dateTrunc(StringLikeLiteral trunc, DateTimeLiteral date) {
        return DateTimeLiteral.fromJavaDateType(dateTruncHelper(date.toJavaDateType(), trunc.getValue()));
    }

    @ExecFunction(name = "date_trunc")
    public static Expression dateTrunc(StringLikeLiteral trunc, DateTimeV2Literal date) {
        return DateTimeV2Literal.fromJavaDateType(dateTruncHelper(date.toJavaDateType(), trunc.getValue()));
    }

    @ExecFunction(name = "date_trunc")
    public static Expression dateTrunc(StringLikeLiteral trunc, DateLiteral date) {
        return DateLiteral.fromJavaDateType(dateTruncHelper(date.toJavaDateType(), trunc.getValue()));
    }

    @ExecFunction(name = "date_trunc")
    public static Expression dateTrunc(StringLikeLiteral trunc, DateV2Literal date) {
        return DateV2Literal.fromJavaDateType(dateTruncHelper(date.toJavaDateType(), trunc.getValue()));
    }

    private static LocalDateTime dateTruncHelper(LocalDateTime dateTime, String trunc) {
        int year = dateTime.getYear();
        int month = dateTime.getMonthValue();
        int day = dateTime.getDayOfMonth();
        int hour = dateTime.getHour();
        int minute = dateTime.getMinute();
        int second = dateTime.getSecond();
        switch (trunc.toLowerCase()) {
            case "year":
                month = 0;
            case "quarter": // CHECKSTYLE IGNORE THIS LINE
                month = ((month - 1) / 3) * 3 + 1;
            case "month": // CHECKSTYLE IGNORE THIS LINE
                day = 1;
                break;
            case "week":
                LocalDateTime firstDayOfWeek = firstDayOfWeek(dateTime);
                year = firstDayOfWeek.getYear();
                month = firstDayOfWeek.getMonthValue();
                day = firstDayOfWeek.getDayOfMonth();
            default: // CHECKSTYLE IGNORE THIS LINE
                break;
        }
        switch (trunc.toLowerCase()) {
            case "year":
            case "quarter":
            case "month":
            case "week":
            case "day": // CHECKSTYLE IGNORE THIS LINE
                hour = 0;
            case "hour": // CHECKSTYLE IGNORE THIS LINE
                minute = 0;
            case "minute": // CHECKSTYLE IGNORE THIS LINE
                second = 0;
            default: // CHECKSTYLE IGNORE THIS LINE
        }
        return LocalDateTime.of(year, month, day, hour, minute, second);
    }

    /**
     * from_days.
     */
    @ExecFunction(name = "from_days")
    public static Expression fromDays(IntegerLiteral n) {
        // doris treat 0000AD as ordinary year but java LocalDateTime treat it as lunar year.
        LocalDateTime res = LocalDateTime.of(0, 1, 1, 0, 0, 0)
                .plusDays(n.getValue());
        if (res.isBefore(LocalDateTime.of(0, 3, 1, 0, 0, 0))) {
            res = res.plusDays(-1);
        }
        return DateV2Literal.fromJavaDateType(res);
    }

    @ExecFunction(name = "last_day")
    public static Expression lastDay(DateLiteral date) {
        LocalDateTime nextMonthFirstDay = LocalDateTime.of((int) date.getYear(), (int) date.getMonth(), 1,
                0, 0, 0).plusMonths(1);
        return DateLiteral.fromJavaDateType(nextMonthFirstDay.minusDays(1));
    }

    @ExecFunction(name = "last_day")
    public static Expression lastDay(DateTimeLiteral date) {
        LocalDateTime nextMonthFirstDay = LocalDateTime.of((int) date.getYear(), (int) date.getMonth(), 1,
                0, 0, 0).plusMonths(1);
        return DateLiteral.fromJavaDateType(nextMonthFirstDay.minusDays(1));
    }

    @ExecFunction(name = "last_day")
    public static Expression lastDay(DateV2Literal date) {
        LocalDateTime nextMonthFirstDay = LocalDateTime.of((int) date.getYear(), (int) date.getMonth(), 1,
                0, 0, 0).plusMonths(1);
        return DateV2Literal.fromJavaDateType(nextMonthFirstDay.minusDays(1));
    }

    @ExecFunction(name = "last_day")
    public static Expression lastDay(DateTimeV2Literal date) {
        LocalDateTime nextMonthFirstDay = LocalDateTime.of((int) date.getYear(), (int) date.getMonth(), 1,
                0, 0, 0).plusMonths(1);
        return DateV2Literal.fromJavaDateType(nextMonthFirstDay.minusDays(1));
    }

    /**
     * datetime transformation function: to_monday
     */
    @ExecFunction(name = "to_monday")
    public static Expression toMonday(DateLiteral date) {
        return DateLiteral.fromJavaDateType(toMonday(date.toJavaDateType()));
    }

    @ExecFunction(name = "to_monday")
    public static Expression toMonday(DateTimeLiteral date) {
        return DateLiteral.fromJavaDateType(toMonday(date.toJavaDateType()));
    }

    @ExecFunction(name = "to_monday")
    public static Expression toMonday(DateV2Literal date) {
        return DateV2Literal.fromJavaDateType(toMonday(date.toJavaDateType()));
    }

    @ExecFunction(name = "to_monday")
    public static Expression toMonday(DateTimeV2Literal date) {
        return DateV2Literal.fromJavaDateType(toMonday(date.toJavaDateType()));
    }

    private static LocalDateTime toMonday(LocalDateTime dateTime) {
        LocalDateTime specialUpperBound = LocalDateTime.of(1970, 1, 4, 23, 59, 59, 999_999_999);
        LocalDateTime specialLowerBound = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        if (dateTime.isAfter(specialUpperBound) || dateTime.isBefore(specialLowerBound)) {
            return dateTime.plusDays(-dateTime.getDayOfWeek().getValue() + 1);
        }
        return specialLowerBound;
    }

    /**
     * date transformation function: from_unixtime
     */
    @ExecFunction(name = "from_unixtime")
    public static Expression fromUnixTime(BigIntLiteral second) {
        return fromUnixTime(second, new VarcharLiteral("%Y-%m-%d %H:%i:%s"));
    }

    /**
     * date transformation function: from_unixtime
     */
    @ExecFunction(name = "from_unixtime")
    public static Expression fromUnixTime(BigIntLiteral second, StringLikeLiteral format) {
        format = (StringLikeLiteral) SupportJavaDateFormatter.translateJavaFormatter(format);

        // 32536771199L is max valid timestamp of mysql from_unix_time
        if (second.getValue() < 0 || second.getValue() > 32536771199L) {
            return new NullLiteral(VarcharType.SYSTEM_DEFAULT);
        }

        ZonedDateTime dateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0)
                .plusSeconds(second.getValue())
                .atZone(ZoneId.of("UTC+0"))
                .toOffsetDateTime()
                .atZoneSameInstant(DateUtils.getTimeZone());
        return dateFormat(new DateTimeLiteral(dateTime.getYear(), dateTime.getMonthValue(),
                        dateTime.getDayOfMonth(), dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond()),
                format);
    }

    /**
     * date transformation function: unix_timestamp
     */
    @ExecFunction(name = "unix_timestamp")
    public static Expression unixTimestamp(DateLiteral date) {
        return new IntegerLiteral(Integer.parseInt(getTimestamp(date.toJavaDateType())));
    }

    @ExecFunction(name = "unix_timestamp")
    public static Expression unixTimestamp(DateTimeLiteral date) {
        return new IntegerLiteral(Integer.parseInt(getTimestamp(date.toJavaDateType())));
    }

    @ExecFunction(name = "unix_timestamp")
    public static Expression unixTimestamp(DateV2Literal date) {
        return new IntegerLiteral(Integer.parseInt(getTimestamp(date.toJavaDateType())));
    }

    /**
     * date transformation function: unix_timestamp
     */
    @ExecFunction(name = "unix_timestamp")
    public static Expression unixTimestamp(DateTimeV2Literal date) {
        if (date.getMicroSecond() == 0) {
            return new DecimalV3Literal(DecimalV3Type.createDecimalV3TypeLooseCheck(10, 0),
                    new BigDecimal(getTimestamp(date.toJavaDateType())));
        }
        int scale = date.getDataType().getScale();
        return new DecimalV3Literal(DecimalV3Type.createDecimalV3TypeLooseCheck(10 + scale, scale),
                new BigDecimal(getTimestamp(date.toJavaDateType())));
    }

    /**
     * date transformation function: unix_timestamp
     */
    @ExecFunction(name = "unix_timestamp")
    public static Expression unixTimestamp(StringLikeLiteral date, StringLikeLiteral format) {
        format = (StringLikeLiteral) SupportJavaDateFormatter.translateJavaFormatter(format);
        DateTimeFormatter formatter = DateUtils.dateTimeFormatter(format.getValue());
        LocalDateTime dateObj;
        try {
            dateObj = LocalDateTime.parse(date.getValue(), formatter);
        } catch (DateTimeParseException e) {
            // means the date string doesn't contain time fields.
            dateObj = LocalDate.parse(date.getValue(), formatter).atStartOfDay();
        }
        return new DecimalV3Literal(DecimalV3Type.createDecimalV3TypeLooseCheck(16, 6),
                new BigDecimal(getTimestamp(dateObj)));
    }

    private static String getTimestamp(LocalDateTime dateTime) {
        LocalDateTime specialUpperBound = LocalDateTime.of(2038, 1, 19, 3, 14, 7);
        LocalDateTime specialLowerBound = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        dateTime = dateTime.atZone(DateUtils.getTimeZone())
                        .toOffsetDateTime().atZoneSameInstant(ZoneId.of("UTC+0"))
                        .toLocalDateTime();
        if (dateTime.isBefore(specialLowerBound) || dateTime.isAfter(specialUpperBound)) {
            return "0";
        }
        Duration duration = Duration.between(
                specialLowerBound,
                dateTime
                );
        if (duration.getNano() == 0) {
            return String.valueOf(duration.getSeconds());
        } else {
            return duration.getSeconds() + "." + (duration.getNano() / 1000);
        }
    }

    /**
     * date transformation function: to_date
     */
    @ExecFunction(name = "to_date")
    public static Expression toDate(DateTimeLiteral date) {
        return new DateLiteral(date.getYear(), date.getMonth(), date.getDay());
    }

    @ExecFunction(name = "to_date")
    public static Expression toDate(DateTimeV2Literal date) {
        return new DateV2Literal(date.getYear(), date.getMonth(), date.getDay());
    }

    /**
     * date transformation function: to_days
     */
    @ExecFunction(name = "to_days")
    public static Expression toDays(DateLiteral date) {
        return new IntegerLiteral(((int) Duration.between(
                LocalDateTime.of(0, 1, 1, 0, 0, 0), date.toJavaDateType()).toDays()));
    }

    @ExecFunction(name = "to_days")
    public static Expression toDays(DateTimeLiteral date) {
        return new IntegerLiteral(((int) Duration.between(
                LocalDateTime.of(0, 1, 1, 0, 0, 0), date.toJavaDateType()).toDays()));
    }

    @ExecFunction(name = "to_days")
    public static Expression toDays(DateV2Literal date) {
        return new IntegerLiteral(((int) Duration.between(
                LocalDateTime.of(0, 1, 1, 0, 0, 0), date.toJavaDateType()).toDays()));
    }

    @ExecFunction(name = "to_days")
    public static Expression toDays(DateTimeV2Literal date) {
        return new IntegerLiteral(((int) Duration.between(
                LocalDateTime.of(0, 1, 1, 0, 0, 0), date.toJavaDateType()).toDays()));
    }

    /**
     * date transformation function: makedate
     */
    @ExecFunction(name = "makedate")
    public static Expression makeDate(IntegerLiteral year, IntegerLiteral dayOfYear) {
        int day = dayOfYear.getValue();
        return day > 0 ? DateLiteral.fromJavaDateType(LocalDateTime.of(year.getValue(), 1, 1, 0, 0, 0)
                .plusDays(day - 1)) : new NullLiteral(DateType.INSTANCE);
    }

    /**
     * date transformation function: str_to_date
     */
    @ExecFunction(name = "str_to_date")
    public static Expression strToDate(StringLikeLiteral str, StringLikeLiteral format) {
        format = (StringLikeLiteral) SupportJavaDateFormatter.translateJavaFormatter(format);
        if (org.apache.doris.analysis.DateLiteral.hasTimePart(format.getStringValue())) {
            DataType returnType = DataType.fromCatalogType(ScalarType.getDefaultDateType(Type.DATETIME));
            if (returnType instanceof DateTimeV2Type) {
                boolean hasMicroPart = org.apache.doris.analysis.DateLiteral
                        .hasMicroSecondPart(format.getStringValue());
                return DateTimeV2Literal.fromJavaDateType(DateUtils.getTime(DateUtils
                        .dateTimeFormatter(format.getValue()), str.getValue()), hasMicroPart ? 6 : 0);
            } else {
                return DateTimeLiteral.fromJavaDateType(DateUtils.getTime(DateUtils
                        .dateTimeFormatter(format.getValue()), str.getValue()));
            }
        } else {
            DataType returnType = DataType.fromCatalogType(ScalarType.getDefaultDateType(Type.DATE));
            if (returnType instanceof DateV2Type) {
                return DateV2Literal.fromJavaDateType(DateUtils.getTime(DateUtils.dateTimeFormatter(format.getValue()),
                        str.getValue()));
            } else {
                return DateLiteral.fromJavaDateType(DateUtils.getTime(DateUtils.dateTimeFormatter(format.getValue()),
                        str.getValue()));
            }
        }
    }

    @ExecFunction(name = "timestamp")
    public static Expression timestamp(DateTimeLiteral datetime) {
        return datetime;
    }

    @ExecFunction(name = "timestamp")
    public static Expression timestamp(DateTimeV2Literal datetime) {
        return datetime;
    }

    /**
     * convert_tz
     */
    @ExecFunction(name = "convert_tz")
    public static Expression convertTz(DateTimeV2Literal datetime, StringLikeLiteral fromTz, StringLikeLiteral toTz) {
        DateTimeFormatter zoneFormatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendZoneOrOffsetId()
                .toFormatter()
                .withResolverStyle(ResolverStyle.STRICT);
        ZoneId fromZone = ZoneId.from(zoneFormatter.parse(fromTz.getStringValue()));
        ZoneId toZone = ZoneId.from(zoneFormatter.parse(toTz.getStringValue()));

        LocalDateTime localDateTime = datetime.toJavaDateType();
        ZonedDateTime resultDateTime = localDateTime.atZone(fromZone).withZoneSameInstant(toZone);
        return DateTimeV2Literal.fromJavaDateType(resultDateTime.toLocalDateTime(), datetime.getDataType().getScale());
    }

    @ExecFunction(name = "weekday")
    public static Expression weekDay(DateLiteral date) {
        return new TinyIntLiteral((byte) ((date.toJavaDateType().getDayOfWeek().getValue() + 6) % 7));
    }

    @ExecFunction(name = "weekday")
    public static Expression weekDay(DateTimeLiteral date) {
        return new TinyIntLiteral((byte) ((date.toJavaDateType().getDayOfWeek().getValue() + 6) % 7));
    }

    @ExecFunction(name = "weekday")
    public static Expression weekDay(DateV2Literal date) {
        return new TinyIntLiteral((byte) ((date.toJavaDateType().getDayOfWeek().getValue() + 6) % 7));
    }

    @ExecFunction(name = "weekday")
    public static Expression weekDay(DateTimeV2Literal date) {
        return new TinyIntLiteral((byte) ((date.toJavaDateType().getDayOfWeek().getValue() + 6) % 7));
    }

    @ExecFunction(name = "week")
    public static Expression week(DateTimeV2Literal dateTime) {
        return week(dateTime.toJavaDateType(), 0);
    }

    @ExecFunction(name = "week")
    public static Expression week(DateTimeV2Literal dateTime, IntegerLiteral mode) {
        return week(dateTime.toJavaDateType(), mode.getIntValue());
    }

    @ExecFunction(name = "week")
    public static Expression week(DateTimeLiteral dateTime) {
        return week(dateTime.toJavaDateType(), 0);
    }

    @ExecFunction(name = "week")
    public static Expression week(DateTimeLiteral dateTime, IntegerLiteral mode) {
        return week(dateTime.toJavaDateType(), mode.getIntValue());
    }

    @ExecFunction(name = "week")
    public static Expression week(DateV2Literal date) {
        return week(date.toJavaDateType(), 0);
    }

    @ExecFunction(name = "week")
    public static Expression week(DateV2Literal date, IntegerLiteral mode) {
        return week(date.toJavaDateType(), mode.getIntValue());
    }

    /**
     * the impl of function week(date/datetime, mode)
     */
    public static Expression week(LocalDateTime localDateTime, int mode) {
        final byte[] resultOfFirstDayBC1 = new byte[] { 1, 0, 1, 52, 1, 0, 1, 52 };
        if (isSpecificDate(localDateTime) && mode >= 0 && mode <= 7) { // 0000-01-01/02
            if (localDateTime.getDayOfMonth() == 1) {
                return new TinyIntLiteral(resultOfFirstDayBC1[mode]);
            } else { // 0001-01-02
                return new TinyIntLiteral((byte) 1);
            }
        }

        switch (mode) {
            case 0: {
                return new TinyIntLiteral(
                        (byte) localDateTime.get(WeekFields.of(DayOfWeek.SUNDAY, 7).weekOfYear()));
            }
            case 1: {
                return new TinyIntLiteral((byte) localDateTime.get(WeekFields.ISO.weekOfYear()));
            }
            case 2: {
                return new TinyIntLiteral(
                        (byte) localDateTime.get(WeekFields.of(DayOfWeek.SUNDAY, 7).weekOfWeekBasedYear()));
            }
            case 3: {
                return new TinyIntLiteral(
                        (byte) localDateTime.get(WeekFields.ISO.weekOfWeekBasedYear()));
            }
            case 4: {
                return new TinyIntLiteral((byte) localDateTime
                        .get(WeekFields.of(DayOfWeek.SUNDAY, 4).weekOfYear()));
            }
            case 5: {
                return new TinyIntLiteral((byte) localDateTime
                        .get(WeekFields.of(DayOfWeek.MONDAY, 7).weekOfYear()));
            }
            case 6: {
                return new TinyIntLiteral((byte) localDateTime
                        .get(WeekFields.of(DayOfWeek.SUNDAY, 4).weekOfWeekBasedYear()));
            }
            case 7: {
                return new TinyIntLiteral((byte) localDateTime
                        .get(WeekFields.of(DayOfWeek.MONDAY, 7).weekOfWeekBasedYear()));
            }
            default: {
                throw new AnalysisException(
                        String.format("unknown mode %d in week function", mode));
            }
        }
    }

    /**
     * 0000-01-01/02 are specific dates, sometime need handle them alone.
     */
    private static boolean isSpecificDate(LocalDateTime localDateTime) {
        return localDateTime.getYear() == 0 && localDateTime.getMonthValue() == 1
                && (localDateTime.getDayOfMonth() == 1 || localDateTime.getDayOfMonth() == 2);
    }

    @ExecFunction(name = "yearweek")
    public static Expression yearWeek(DateV2Literal date, IntegerLiteral mode) {
        return yearWeek(date.toJavaDateType(), mode.getIntValue());
    }

    @ExecFunction(name = "yearweek")
    public static Expression yearWeek(DateTimeV2Literal dateTime, IntegerLiteral mode) {
        return yearWeek(dateTime.toJavaDateType(), mode.getIntValue());
    }

    @ExecFunction(name = "yearweek")
    public static Expression yearWeek(DateTimeLiteral dateTime, IntegerLiteral mode) {
        return yearWeek(dateTime.toJavaDateType(), mode.getIntValue());
    }

    @ExecFunction(name = "yearweek")
    public static Expression yearWeek(DateV2Literal date) {
        return yearWeek(date.toJavaDateType(), 0);
    }

    @ExecFunction(name = "yearweek")
    public static Expression yearWeek(DateTimeV2Literal dateTime) {
        return yearWeek(dateTime.toJavaDateType(), 0);
    }

    @ExecFunction(name = "yearweek")
    public static Expression yearWeek(DateTimeLiteral dateTime) {
        return yearWeek(dateTime.toJavaDateType(), 0);
    }

    /**
     * the impl of function yearWeek(date/datetime, mode)
     */
    public static Expression yearWeek(LocalDateTime localDateTime, int mode) {
        if (localDateTime.getYear() == 0) {
            return week(localDateTime, mode);
        }

        switch (mode) {
            case 0: {
                return new IntegerLiteral(
                        localDateTime.get(WeekFields.of(DayOfWeek.SUNDAY, 7).weekBasedYear()) * 100
                                + localDateTime.get(
                                WeekFields.of(DayOfWeek.SUNDAY, 7).weekOfWeekBasedYear()));
            }
            case 1: {
                return new IntegerLiteral(localDateTime.get(WeekFields.ISO.weekBasedYear()) * 100
                        + localDateTime.get(WeekFields.ISO.weekOfWeekBasedYear()));
            }
            case 2: {
                return new IntegerLiteral(
                        localDateTime.get(WeekFields.of(DayOfWeek.SUNDAY, 7).weekBasedYear()) * 100
                                + localDateTime.get(
                                WeekFields.of(DayOfWeek.SUNDAY, 7).weekOfWeekBasedYear()));
            }
            case 3: {
                return new IntegerLiteral(localDateTime.get(WeekFields.ISO.weekBasedYear()) * 100
                        + localDateTime.get(WeekFields.ISO.weekOfWeekBasedYear()));
            }
            case 4: {
                return new IntegerLiteral(
                        localDateTime.get(WeekFields.of(DayOfWeek.SUNDAY, 4).weekBasedYear()) * 100
                                + localDateTime
                                .get(WeekFields.of(DayOfWeek.SUNDAY, 4).weekOfWeekBasedYear()));
            }
            case 5: {
                return new IntegerLiteral(
                        localDateTime.get(WeekFields.of(DayOfWeek.MONDAY, 7).weekBasedYear()) * 100
                                + localDateTime
                                .get(WeekFields.of(DayOfWeek.MONDAY, 7).weekOfWeekBasedYear()));
            }
            case 6: {
                return new IntegerLiteral(
                        localDateTime.get(WeekFields.of(DayOfWeek.SUNDAY, 4).weekBasedYear()) * 100
                                + localDateTime.get(
                                WeekFields.of(DayOfWeek.SUNDAY, 4).weekOfWeekBasedYear()));
            }
            case 7: {
                return new IntegerLiteral(
                        localDateTime.get(WeekFields.of(DayOfWeek.MONDAY, 7).weekBasedYear()) * 100
                                + localDateTime.get(
                                WeekFields.of(DayOfWeek.MONDAY, 7).weekOfWeekBasedYear()));
            }
            default: {
                throw new AnalysisException(
                        String.format("unknown mode %d in yearweek function", mode));
            }
        }
    }

    /**
     * weekofyear
     */
    @ExecFunction(name = "weekofyear")
    public static Expression weekOfYear(DateTimeV2Literal dateTime) {
        if (dateTime.getYear() == 0 && dateTime.getDayOfWeek() == 1) {
            if (dateTime.getMonth() == 1 && dateTime.getDay() == 2) {
                return new TinyIntLiteral((byte) 1);
            }
            return new TinyIntLiteral(
                    (byte) (dateTime.toJavaDateType().get(WeekFields.ISO.weekOfWeekBasedYear()) + 1));
        }
        return new TinyIntLiteral((byte) dateTime.toJavaDateType().get(WeekFields.ISO.weekOfWeekBasedYear()));
    }

    /**
     * weekofyear
     */
    @ExecFunction(name = "weekofyear")
    public static Expression weekOfYear(DateTimeLiteral dateTime) {
        if (dateTime.getYear() == 0 && dateTime.getDayOfWeek() == 1) {
            if (dateTime.getMonth() == 1 && dateTime.getDay() == 2) {
                return new TinyIntLiteral((byte) 1);
            }
            return new TinyIntLiteral(
                    (byte) (dateTime.toJavaDateType().get(WeekFields.ISO.weekOfWeekBasedYear()) + 1));
        }
        return new TinyIntLiteral((byte) dateTime.toJavaDateType().get(WeekFields.ISO.weekOfWeekBasedYear()));
    }

    /**
     * weekofyear
     */
    @ExecFunction(name = "weekofyear")
    public static Expression weekOfYear(DateV2Literal date) {
        if (date.getYear() == 0 && date.getDayOfWeek() == 1) {
            if (date.getMonth() == 1 && date.getDay() == 2) {
                return new TinyIntLiteral((byte) 1);
            }
            return new TinyIntLiteral((byte) (date.toJavaDateType().get(WeekFields.ISO.weekOfWeekBasedYear()) + 1));
        }
        return new TinyIntLiteral((byte) date.toJavaDateType().get(WeekFields.ISO.weekOfWeekBasedYear()));
    }

    @ExecFunction(name = "dayname")
    public static Expression dayName(DateTimeV2Literal dateTime) {
        return new VarcharLiteral(dateTime.toJavaDateType().getDayOfWeek().getDisplayName(TextStyle.FULL,
                Locale.getDefault()));
    }

    @ExecFunction(name = "dayname")
    public static Expression dayName(DateTimeLiteral dateTime) {
        return new VarcharLiteral(dateTime.toJavaDateType().getDayOfWeek().getDisplayName(TextStyle.FULL,
                Locale.getDefault()));
    }

    @ExecFunction(name = "dayname")
    public static Expression dayName(DateV2Literal date) {
        return new VarcharLiteral(date.toJavaDateType().getDayOfWeek().getDisplayName(TextStyle.FULL,
                Locale.getDefault()));
    }

    @ExecFunction(name = "monthname")
    public static Expression monthName(DateTimeV2Literal dateTime) {
        return new VarcharLiteral(dateTime.toJavaDateType().getMonth().getDisplayName(TextStyle.FULL,
                Locale.getDefault()));
    }

    @ExecFunction(name = "monthname")
    public static Expression monthName(DateTimeLiteral dateTime) {
        return new VarcharLiteral(dateTime.toJavaDateType().getMonth().getDisplayName(TextStyle.FULL,
                Locale.getDefault()));
    }

    @ExecFunction(name = "monthname")
    public static Expression monthName(DateV2Literal date) {
        return new VarcharLiteral(date.toJavaDateType().getMonth().getDisplayName(TextStyle.FULL,
                Locale.getDefault()));
    }

    @ExecFunction(name = "from_second")
    public static Expression fromSecond(BigIntLiteral second) {
        return fromMicroSecond(second.getValue() * 1000 * 1000);
    }

    @ExecFunction(name = "from_millisecond")
    public static Expression fromMilliSecond(BigIntLiteral milliSecond) {
        return fromMicroSecond(milliSecond.getValue() * 1000);
    }

    @ExecFunction(name = "from_microsecond")
    public static Expression fromMicroSecond(BigIntLiteral microSecond) {
        return fromMicroSecond(microSecond.getValue());
    }

    private static Expression fromMicroSecond(long microSecond) {
        if (microSecond < 0 || microSecond > 253402271999999999L) {
            return new NullLiteral(DateTimeV2Type.SYSTEM_DEFAULT);
        }
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(microSecond / 1000).plusNanos(microSecond % 1000 * 1000),
                DateUtils.getTimeZone());
        return new DateTimeV2Literal(DateTimeV2Type.MAX, dateTime.getYear(),
                dateTime.getMonthValue(), dateTime.getDayOfMonth(), dateTime.getHour(),
                dateTime.getMinute(), dateTime.getSecond(), dateTime.getNano() / 1000);
    }

    @ExecFunction(name = "microseconds_diff")
    public static Expression microsecondsDiff(DateTimeV2Literal t1, DateTimeV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.MICROS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "milliseconds_diff")
    public static Expression millisecondsDiff(DateTimeV2Literal t1, DateTimeV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.MILLIS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "seconds_diff")
    public static Expression secondsDiff(DateTimeV2Literal t1, DateTimeV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.SECONDS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "seconds_diff")
    public static Expression secondsDiff(DateTimeV2Literal t1, DateV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.SECONDS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "seconds_diff")
    public static Expression secondsDiff(DateV2Literal t1, DateTimeV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.SECONDS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "seconds_diff")
    public static Expression secondsDiff(DateV2Literal t1, DateV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.SECONDS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "seconds_diff")
    public static Expression secondsDiff(DateTimeLiteral t1, DateTimeLiteral t2) {
        return new BigIntLiteral(ChronoUnit.SECONDS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "minutes_diff")
    public static Expression minutesDiff(DateTimeV2Literal t1, DateTimeV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.MINUTES.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "minutes_diff")
    public static Expression minutesDiff(DateTimeV2Literal t1, DateV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.MINUTES.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "minutes_diff")
    public static Expression minutesDiff(DateV2Literal t1, DateTimeV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.MINUTES.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "minutes_diff")
    public static Expression minutesDiff(DateV2Literal t1, DateV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.MINUTES.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "minutes_diff")
    public static Expression minutesDiff(DateTimeLiteral t1, DateTimeLiteral t2) {
        return new BigIntLiteral(ChronoUnit.MINUTES.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "hours_diff")
    public static Expression hoursDiff(DateTimeV2Literal t1, DateTimeV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.HOURS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "hours_diff")
    public static Expression hoursDiff(DateTimeV2Literal t1, DateV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.HOURS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "hours_diff")
    public static Expression hoursDiff(DateV2Literal t1, DateTimeV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.HOURS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "hours_diff")
    public static Expression hoursDiff(DateV2Literal t1, DateV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.HOURS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "hours_diff")
    public static Expression hoursDiff(DateTimeLiteral t1, DateTimeLiteral t2) {
        return new BigIntLiteral(ChronoUnit.HOURS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "days_diff")
    public static Expression daysDiff(DateTimeV2Literal t1, DateTimeV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.DAYS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "days_diff")
    public static Expression daysDiff(DateTimeV2Literal t1, DateV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.DAYS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "days_diff")
    public static Expression daysDiff(DateV2Literal t1, DateTimeV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.DAYS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "days_diff")
    public static Expression daysDiff(DateV2Literal t1, DateV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.DAYS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "days_diff")
    public static Expression daysDiff(DateTimeLiteral t1, DateTimeLiteral t2) {
        return new BigIntLiteral(ChronoUnit.DAYS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "weeks_diff")
    public static Expression weeksDiff(DateTimeV2Literal t1, DateTimeV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.WEEKS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "weeks_diff")
    public static Expression weeksDiff(DateTimeV2Literal t1, DateV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.WEEKS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "weeks_diff")
    public static Expression weeksDiff(DateV2Literal t1, DateTimeV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.WEEKS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "weeks_diff")
    public static Expression weeksDiff(DateV2Literal t1, DateV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.WEEKS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "weeks_diff")
    public static Expression weeksDiff(DateTimeLiteral t1, DateTimeLiteral t2) {
        return new BigIntLiteral(ChronoUnit.WEEKS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "months_diff")
    public static Expression monthsDiff(DateTimeV2Literal t1, DateTimeV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.MONTHS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "months_diff")
    public static Expression monthsDiff(DateTimeV2Literal t1, DateV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.MONTHS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "months_diff")
    public static Expression monthsDiff(DateV2Literal t1, DateTimeV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.MONTHS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "months_diff")
    public static Expression monthsDiff(DateV2Literal t1, DateV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.MONTHS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "months_diff")
    public static Expression monthsDiff(DateTimeLiteral t1, DateTimeLiteral t2) {
        return new BigIntLiteral(ChronoUnit.MONTHS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "years_diff")
    public static Expression yearsDiff(DateTimeV2Literal t1, DateTimeV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.YEARS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "years_diff")
    public static Expression yearsDiff(DateTimeV2Literal t1, DateV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.YEARS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "years_diff")
    public static Expression yearsDiff(DateV2Literal t1, DateTimeV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.YEARS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "years_diff")
    public static Expression yearsDiff(DateV2Literal t1, DateV2Literal t2) {
        return new BigIntLiteral(ChronoUnit.YEARS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    @ExecFunction(name = "years_diff")
    public static Expression yearsDiff(DateTimeLiteral t1, DateTimeLiteral t2) {
        return new BigIntLiteral(ChronoUnit.YEARS.between(t2.toJavaDateType(), t1.toJavaDateType()));
    }

    /**
     * months_between(date1, date2, round_off)
     */
    @ExecFunction(name = "months_between")
    public static Expression monthsBetween(DateV2Literal t1, DateV2Literal t2, BooleanLiteral roundOff) {
        long yearBetween = t1.getYear() - t2.getYear();
        long monthBetween = t1.getMonth() - t2.getMonth();
        int daysInMonth1 = YearMonth.of((int) t1.getYear(), (int) t1.getMonth()).lengthOfMonth();
        int daysInMonth2 = YearMonth.of((int) t2.getYear(), (int) t2.getMonth()).lengthOfMonth();
        double dayBetween = 0;
        if (t1.getDay() == daysInMonth1 && t2.getDay() == daysInMonth2) {
            dayBetween = 0;
        } else {
            dayBetween = (t1.getDay() - t2.getDay()) / 31.0;
        }
        double result = yearBetween * 12 + monthBetween + dayBetween;
        // rounded to 8 digits unless roundOff=false.
        if (roundOff.getValue()) {
            result = new BigDecimal(result).setScale(8, RoundingMode.HALF_UP).doubleValue();
        }
        return new DoubleLiteral(result);
    }

    private static int getDayOfWeek(String day) {
        Integer dayOfWeek = DAY_OF_WEEK.get(day.toUpperCase());
        if (dayOfWeek == null) {
            return 0;
        }
        return dayOfWeek;
    }

    /**
     * date arithmetic function next_day
     */
    @ExecFunction(name = "next_day")
    public static Expression nextDay(DateV2Literal date, StringLiteral day) {
        int dayOfWeek = getDayOfWeek(day.getValue());
        if (dayOfWeek == 0) {
            throw new RuntimeException("Invalid day of week: " + day.getValue());
        }
        int daysToAdd = (dayOfWeek - date.getDayOfWeek() + 7) % 7;
        daysToAdd = daysToAdd == 0 ? 7 : daysToAdd;
        return date.plusDays(daysToAdd);
    }
}
