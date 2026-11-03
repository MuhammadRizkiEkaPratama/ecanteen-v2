package com.canteen.service;

import com.canteen.entity.SecurityLog;
import com.canteen.mapper.LogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogService {

    private final LogMapper logMapper;

    public LogService(LogMapper logMapper) {
        this.logMapper = logMapper;
    }

    // Plain insert of one log row. Called directly from controllers (NOT an Observer).
    public void record(String username, String action, String detail) {
        SecurityLog log = new SecurityLog();
        log.setUsername(username);
        log.setAction(action);
        log.setDetail(detail);
        logMapper.insertLog(log);
    }

    public List<SecurityLog> getRecentLogs() {
        return logMapper.findRecentLogs();
    }
}
