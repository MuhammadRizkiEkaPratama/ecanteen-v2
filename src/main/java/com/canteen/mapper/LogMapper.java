package com.canteen.mapper;

import com.canteen.entity.SecurityLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LogMapper {

    int insertLog(SecurityLog log);

    List<SecurityLog> findRecentLogs();
}
