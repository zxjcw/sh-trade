package com.shiguangji.modules.bounty;

import com.shiguangji.common.api.ErrorCodes;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiguangji.common.config.AppProperties;
import com.shiguangji.common.dto.Dtos.BountyDto;
import com.shiguangji.common.entity.AppUser;
import com.shiguangji.common.entity.Bounty;
import com.shiguangji.common.entity.Category;
import com.shiguangji.common.exception.BizException;
import com.shiguangji.common.mapper.BountyMapper;
import com.shiguangji.common.mapper.CategoryMapper;
import com.shiguangji.common.security.CurrentUser;
import com.shiguangji.modules.item.ItemAssembler;
import com.shiguangji.modules.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BountyService {

    private final BountyMapper bountyMapper;
    private final CategoryMapper categoryMapper;
    private final ItemAssembler assembler;
    private final UserService userService;
    private final AppProperties properties;

    /** 前端传 { categoryId, keyword, budget, condition, expiresDays }。 */
    public record CreateRequest(Long categoryId, String keyword, BigDecimal budget, String condition,
                                Integer expiresDays, BigDecimal tolerance) {
    }

    public List<BountyDto> list(String scope, Long userId) {
        LambdaQueryWrapper<Bounty> wrapper = new LambdaQueryWrapper<>();
        boolean mine = "mine".equals(scope);
        if (mine && userId == null) {
            throw new BizException(ErrorCodes.UNAUTHORIZED, "请先登录");
        }
        if (mine) {
            wrapper.eq(Bounty::getBuyerId, userId);
        } else {
            wrapper.eq(Bounty::getStatus, "open");
        }
        wrapper.orderByDesc(Bounty::getCreatedAt).orderByDesc(Bounty::getBountyId);
        return bountyMapper.selectList(wrapper).stream()
                .map(bounty -> toDto(bounty, mine || Objects.equals(bounty.getBuyerId(), userId)))
                .toList();
    }

    @Transactional
    public BountyDto create(CreateRequest request) {
        AppUser me = userService.current();
        userService.requireVerified(me);
        if (request.budget() == null || request.budget().compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.badRequest("请填写合理的预算金额");
        }
        if (request.categoryId() == null) {
            throw BizException.badRequest("请选择求购分类");
        }
        Category category = categoryMapper.selectById(request.categoryId());
        if (category == null) {
            throw BizException.badRequest("分类不存在");
        }
        long open = bountyMapper.selectCount(new LambdaQueryWrapper<Bounty>()
                .eq(Bounty::getBuyerId, me.getUserId())
                .eq(Bounty::getStatus, "open"));
        int max = properties.limits().maxOpenBounties() == null ? 3 : properties.limits().maxOpenBounties();
        if (open >= max) {
            throw BizException.conflict("同时进行中的求购最多 " + max + " 条，请先关闭一条");
        }
        int toleranceDefault = properties.limits().bountyTolerance() == null
                ? 20 : properties.limits().bountyTolerance();
        Bounty bounty = new Bounty();
        bounty.setBuyerId(me.getUserId());
        bounty.setCategoryId(request.categoryId());
        bounty.setKeyword(request.keyword());
        bounty.setBudget(request.budget());
        bounty.setConditionCode(normalizeCondition(request.condition()));
        bounty.setPriceTolerance(request.tolerance() == null
                ? BigDecimal.valueOf(toleranceDefault) : request.tolerance());
        bounty.setStatus("open");
        bounty.setMatchCount(0);
        bounty.setCreatedAt(LocalDateTime.now());
        int days = request.expiresDays() == null || request.expiresDays() < 1 ? 7
                : Math.min(request.expiresDays(), 30);
        bounty.setExpiresAt(LocalDateTime.now().plusDays(days));
        bountyMapper.insert(bounty);
        return toDto(bounty, true);
    }

    @Transactional
    public BountyDto close(Long id) {
        Long me = CurrentUser.id();
        Bounty bounty = bountyMapper.selectById(id);
        if (bounty == null || !Objects.equals(bounty.getBuyerId(), me)) {
            throw BizException.notFound("求购不存在");
        }
        if (!"open".equals(bounty.getStatus())) {
            throw BizException.badRequest("只有进行中的求购可以关闭");
        }
        bounty.setStatus("closed");
        bounty.setClosedAt(LocalDateTime.now());
        bountyMapper.updateById(bounty);
        return toDto(bounty, true);
    }

    public BountyDto toDto(Bounty bounty, boolean revealBuyer) {
        AppUser buyer = assembler.user(bounty.getBuyerId());
        String buyerName = buyer == null ? null : buyer.getNickname();
        // 大厅里隐藏买家真实昵称（与前端 mock 一致：保留 buyerId，昵称打码）
        if (!revealBuyer && buyerName != null && !buyerName.isEmpty()) {
            buyerName = buyerName.substring(0, 1) + "同学";
        }
        return new BountyDto(bounty.getBountyId(), bounty.getBuyerId(), buyerName, bounty.getCategoryId(),
                assembler.categoryName(bounty.getCategoryId()), bounty.getKeyword(), bounty.getBudget(),
                bounty.getConditionCode(), bounty.getStatus(), bounty.getCreatedAt(), bounty.getExpiresAt(),
                bounty.getMatchCount());
    }

    private static final java.util.Set<String> CONDITIONS =
            java.util.Set.of("new", "like_new", "good", "fair", "poor");

    private static String normalizeCondition(String condition) {
        return condition != null && CONDITIONS.contains(condition) ? condition : "good";
    }
}
