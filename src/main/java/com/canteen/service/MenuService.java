package com.canteen.service;

import com.canteen.entity.MenuItem;
import com.canteen.entity.Stall;
import com.canteen.mapper.MenuMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuService {

    private final MenuMapper menuMapper;

    public MenuService(MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    public List<Stall> getActiveStalls() {
        return menuMapper.findActiveStalls();
    }

    public List<MenuItem> getItemsByStall(Integer stallId) {
        return menuMapper.findItemsByStall(stallId);
    }

    // #6 search: keyword may be empty (show all).
    public List<MenuItem> searchItemsByStall(Integer stallId, String keyword) {
        return menuMapper.searchItemsByStall(stallId, keyword);
    }

    public MenuItem getItemById(Integer id) {
        return menuMapper.findItemById(id);
    }

    public Stall getStallById(Integer id) {
        return menuMapper.findStallById(id);
    }
}
