package com.example.attendance;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AttendanceMapper {
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "employee_id", javaType = long.class),
            @Arg(column = "business_date", javaType = LocalDate.class),
            @Arg(column = "start_at", javaType = java.time.LocalDateTime.class),
            @Arg(column = "end_at", javaType = java.time.LocalDateTime.class),
            @Arg(column = "break_minutes", javaType = int.class)
    })
    @Select("""
            SELECT id, employee_id, business_date, start_at, end_at, break_minutes
            FROM attendance_records
            WHERE business_date = #{businessDate} AND id > #{afterId}
            ORDER BY id
            LIMIT 1
            """)
    AttendanceRecord findNext(@Param("businessDate") LocalDate businessDate,
                              @Param("afterId") long afterId);

    @Insert("""
            INSERT INTO attendance_daily_summary
                (employee_id, business_date, worked_minutes, overtime_minutes)
            VALUES (#{employeeId}, #{businessDate}, #{workedMinutes}, #{overtimeMinutes})
            ON DUPLICATE KEY UPDATE
                worked_minutes = VALUES(worked_minutes),
                overtime_minutes = VALUES(overtime_minutes)
            """)
    int upsertSummary(DailySummary summary);

    @ConstructorArgs({
            @Arg(column = "employee_id", javaType = long.class),
            @Arg(column = "business_date", javaType = LocalDate.class),
            @Arg(column = "worked_minutes", javaType = long.class),
            @Arg(column = "overtime_minutes", javaType = long.class)
    })
    @Select("""
            SELECT employee_id, business_date, worked_minutes, overtime_minutes
            FROM attendance_daily_summary
            WHERE business_date = #{businessDate}
            ORDER BY employee_id
            """)
    List<DailySummary> findSummaries(LocalDate businessDate);

    @Select("SELECT COUNT(*) FROM attendance_records WHERE business_date = #{businessDate}")
    long countRecords(LocalDate businessDate);

    @Select("SELECT COUNT(*) FROM attendance_daily_summary WHERE business_date = #{businessDate}")
    long countSummaries(LocalDate businessDate);
}
