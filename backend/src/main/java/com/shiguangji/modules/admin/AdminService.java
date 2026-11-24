package com.shiguangji.modules.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiguangji.common.dto.Dtos.CategoryDto;
import com.shiguangji.common.dto.Dtos.PickupPointDto;
import com.shiguangji.common.dto.Dtos.RosterEntryDto;
import com.shiguangji.common.dto.Dtos.UserDto;
import com.shiguangji.common.entity.AppUser;
import com.shiguangji.common.entity.Category;
import com.shiguangji.common.entity.Item;
import com.shiguangji.common.entity.PickupPoint;
import com.shiguangji.common.entity.StudentRoster;
import com.shiguangji.common.entity.TradeOrder;
import com.shiguangji.common.exception.BizException;
import com.shiguangji.common.mapper.AppUserMapper;
import com.shiguangji.common.mapper.CategoryMapper;
import com.shiguangji.common.mapper.ItemMapper;
import com.shiguangji.common.mapper.PickupPointMapper;
import com.shiguangji.common.mapper.StudentRosterMapper;
import com.shiguangji.common.mapper.TradeOrderMapper;
import com.shiguangji.modules.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final StudentRosterMapper rosterMapper;
    private final CategoryMapper categoryMapper;
    private final PickupPointMapper pickupPointMapper;
    private final ItemMapper itemMapper;
    private final TradeOrderMapper orderMapper;
    private final AppUserMapper userMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public record RosterEntry(String studentNo, String name, String college, String major, String grade) {
    }

    /** 前端传 { rows: [...] }，也兼容 { entries: [...] }。 */
    public record RosterImportRequest(List<RosterEntry> rows, List<RosterEntry> entries) {
        List<RosterEntry> list() {
            if (rows != null && !rows.isEmpty()) {
                return rows;
            }
            return entries;
        }
    }

    public record CategoryRequest(String name, Long parentId, Boolean enabled) {
    }

    public record PickupPointRequest(String name, String address, String coordinate) {
    }

    public record OperatorRequest(String username, String password, String nickname, String studentNo) {
    }

    public List<RosterEntryDto> roster(String keyword) {
        LambdaQueryWrapper<StudentRoster> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(StudentRoster::getStudentNo, kw)
                    .or().like(StudentRoster::getName, kw)
                    .or().like(StudentRoster::getCollege, kw));
        }
        wrapper.orderByAsc(StudentRoster::getStudentNo);
        return rosterMapper.selectList(wrapper).stream()
                .map(r -> new RosterEntryDto(r.getStudentNo(), r.getName(), r.getCollege(), r.getMajor(), r.getGrade()))
                .toList();
    }

    @Transactional
    public Map<String, Object> importRoster(RosterImportRequest request) {
        List<RosterEntry> entries = request == null ? null : request.list();
        if (entries == null || entries.isEmpty()) {
            throw BizException.badRequest("导入内容不能为空");
        }
        int inserted = 0;
        int updated = 0;
        LocalDateTime now = LocalDateTime.now();
        for (RosterEntry entry : entries) {
            if (entry == null || entry.studentNo() == null || entry.studentNo().isBlank()
                    || entry.name() == null || entry.name().isBlank()) {
                continue;
            }
            StudentRoster exists = rosterMapper.selectById(entry.studentNo().trim());
            if (exists == null) {
                StudentRoster roster = new StudentRoster();
                roster.setStudentNo(entry.studentNo().trim());
                roster.setName(entry.name().trim());
                roster.setCollege(entry.college());
                roster.setMajor(entry.major());
                roster.setGrade(entry.grade());
                roster.setCreatedAt(now);
                roster.setUpdatedAt(now);
                rosterMapper.insert(roster);
                inserted++;
            } else {
                exists.setName(entry.name().trim());
                exists.setCollege(entry.college());
                exists.setMajor(entry.major());
                exists.setGrade(entry.grade());
                exists.setUpdatedAt(now);
                rosterMapper.updateById(exists);
                updated++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        // 前端只读 count，其余字段保留便于调试。
        result.put("count", inserted + updated);
        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("total", rosterMapper.selectCount(null));
        return result;
    }

    public List<CategoryDto> categoryTree() {
        List<Category> all = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getCategoryId));
        Map<Long, List<Category>> children = new LinkedHashMap<>();
        for (Category category : all) {
            children.computeIfAbsent(category.getParentId(), key -> new ArrayList<>()).add(category);
        }
        return buildCategories(all, children, null);
    }

    private List<CategoryDto> buildCategories(List<Category> all, Map<Long, List<Category>> children, Long parentId) {
        List<Category> level = children.getOrDefault(parentId, List.of());
        List<CategoryDto> result = new ArrayList<>();
        for (Category category : level) {
            List<CategoryDto> sub = buildCategories(all, children, category.getCategoryId());
            result.add(new CategoryDto(category.getCategoryId(), category.getName(), category.getParentId(),
                    Boolean.TRUE.equals(category.getEnabled()), sub));
        }
        return result;
    }

    @Transactional
    public CategoryDto createCategory(CategoryRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw BizException.badRequest("分类名称不能为空");
        }
        String name = request.name().trim();
        Long parentId = request.parentId();
        if (parentId != null) {
            Category parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                throw BizException.badRequest("上级分类不存在");
            }
            if (parent.getParentId() != null) {
                throw BizException.badRequest("分类最多支持两级");
            }
        }
        Long conflict = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, name)
                .eq(parentId == null, Category::getParentId, null)
                .eq(parentId != null, Category::getParentId, parentId));
        if (conflict != null && conflict > 0) {
            throw BizException.conflict("同级下已存在同名分类");
        }
        Category category = new Category();
        category.setName(name);
        category.setParentId(parentId);
        category.setEnabled(request.enabled() == null || request.enabled());
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.insert(category);
        return new CategoryDto(category.getCategoryId(), category.getName(), category.getParentId(),
                Boolean.TRUE.equals(category.getEnabled()), List.of());
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryRequest request) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw BizException.notFound("分类不存在");
        }
        if (request != null && request.name() != null && !request.name().isBlank()) {
            String name = request.name().trim();
            Long conflict = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                    .eq(Category::getName, name)
                    .ne(Category::getCategoryId, id)
                    .eq(category.getParentId() == null, Category::getParentId, null)
                    .eq(category.getParentId() != null, Category::getParentId, category.getParentId()));
            if (conflict != null && conflict > 0) {
                throw BizException.conflict("同级下已存在同名分类");
            }
            category.setName(name);
        }
        if (request != null && request.enabled() != null) {
            category.setEnabled(request.enabled());
        }
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(category);
        return new CategoryDto(category.getCategoryId(), category.getName(), category.getParentId(),
                Boolean.TRUE.equals(category.getEnabled()), List.of());
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw BizException.notFound("分类不存在");
        }
        Long children = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, id));
        if (children != null && children > 0) {
            throw BizException.conflict("该分类下还有子分类，无法删除");
        }
        Long items = itemMapper.selectCount(new LambdaQueryWrapper<Item>().eq(Item::getCategoryId, id));
        if (items != null && items > 0) {
            throw BizException.conflict("该分类下还有物品，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    public List<PickupPointDto> pickupPoints() {
        return pickupPointMapper.selectList(new LambdaQueryWrapper<PickupPoint>()
                        .orderByAsc(PickupPoint::getPickupPointId))
                .stream().map(p -> new PickupPointDto(p.getPickupPointId(), p.getName(), p.getAddress())).toList();
    }

    @Transactional
    public PickupPointDto createPickupPoint(PickupPointRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw BizException.badRequest("自提点名称不能为空");
        }
        PickupPoint point = new PickupPoint();
        point.setName(request.name().trim());
        point.setAddress(request.address());
        point.setCoordinate(request.coordinate());
        pickupPointMapper.insert(point);
        return new PickupPointDto(point.getPickupPointId(), point.getName(), point.getAddress());
    }

    @Transactional
    public PickupPointDto updatePickupPoint(Long id, PickupPointRequest request) {
        PickupPoint point = pickupPointMapper.selectById(id);
        if (point == null) {
            throw BizException.notFound("自提点不存在");
        }
        if (request != null && request.name() != null && !request.name().isBlank()) {
            point.setName(request.name().trim());
        }
        if (request != null && request.address() != null) {
            point.setAddress(request.address());
        }
        if (request != null && request.coordinate() != null) {
            point.setCoordinate(request.coordinate());
        }
        pickupPointMapper.updateById(point);
        return new PickupPointDto(point.getPickupPointId(), point.getName(), point.getAddress());
    }

    @Transactional
    public void deletePickupPoint(Long id) {
        long used = orderMapper.selectCount(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getPickupPointId, id));
        if (used > 0) {
            throw BizException.conflict("该自提点已被使用，无法删除");
        }
        pickupPointMapper.deleteById(id);
    }

    public List<UserDto> operators() {
        return userMapper.selectList(new LambdaQueryWrapper<AppUser>()
                        .eq(AppUser::getRole, "operator")
                        .orderByAsc(AppUser::getUserId))
                .stream().map(userService::toDto)
                .sorted(Comparator.comparing(UserDto::id))
                .toList();
    }

    @Transactional
    public UserDto createOperator(OperatorRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().length() < 6) {
            throw BizException.badRequest("请输入 3~20 位用户名和至少 6 位密码");
        }
        String username = request.username().trim();
        if (username.length() < 3 || username.length() > 20) {
            throw BizException.badRequest("用户名长度需在 3~20 位之间");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<AppUser>().eq(AppUser::getUsername, username));
        if (exists != null && exists > 0) {
            throw BizException.conflict("用户名已被占用");
        }
        AppUser operator = new AppUser();
        operator.setUsername(username);
        operator.setPasswordHash(passwordEncoder.encode(request.password()));
        operator.setNickname(request.nickname() == null || request.nickname().isBlank()
                ? username : request.nickname().trim());
        operator.setStudentNo(request.studentNo());
        operator.setRole("operator");
        operator.setVerifyStatus("verified");
        operator.setCreditScore(100);
        operator.setBanned(false);
        operator.setBalance(java.math.BigDecimal.ZERO);
        operator.setCreatedAt(LocalDateTime.now());
        operator.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(operator);
        return userService.toDto(operator);
    }
}
