package beyond.ordersystem.ordering.controller;

import beyond.ordersystem.common.dto.CommonResDto;
import beyond.ordersystem.member.domain.Member;
import beyond.ordersystem.member.repository.MemberRepository;
import beyond.ordersystem.ordering.domain.Ordering;
import beyond.ordersystem.ordering.dto.OrderCreateReqDto;
import beyond.ordersystem.ordering.dto.OrderListResDto;
import beyond.ordersystem.ordering.repository.OrderingRepository;
import beyond.ordersystem.ordering.service.OrderingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.awt.print.Pageable;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderingController {

    private final OrderingService orderingService;
    private final OrderingRepository orderingRepository;
    private final MemberRepository memberRepository;

    @Autowired
    public OrderingController(OrderingService orderingService, OrderingRepository orderingRepository, MemberRepository memberRepository) {
        this.orderingService = orderingService;
        this.orderingRepository = orderingRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * 주문 등록
     */
    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody List<OrderCreateReqDto> dto) {

        Ordering ordering = orderingService.createOrder(dto);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED, "정상완료", ordering.getId());
        return new ResponseEntity<>(commonResDto, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public ResponseEntity<?> orderList() {

        List<OrderListResDto> orderListResDtos = orderingService.orderList();

        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "정상 조회 완료", orderListResDtos);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }
    // 내 주문만 볼 수 있는 myOrders : order/myorders
    @GetMapping("/myorders")
    public ResponseEntity<?> myOrders() {
        List<OrderListResDto> orderList = orderingService.myOrders();
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK,"정상 조회 완료", orderList);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }



    // admin사용자의 주문취소 : /order/{id}/cancel -> orderstatus 만 변경
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> orderCancel (@PathVariable Long id) {
        Ordering ordering = orderingService.orderCancel(id);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK,"정상 취소 완료", ordering.getId());
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }
}
