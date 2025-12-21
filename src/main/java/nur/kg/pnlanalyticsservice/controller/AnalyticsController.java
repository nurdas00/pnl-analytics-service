package nur.kg.pnlanalyticsservice.controller;


import lombok.RequiredArgsConstructor;
import nur.kg.pnlanalyticsservice.dto.OrderDto;
import nur.kg.pnlanalyticsservice.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AnalyticsController {

    private final OrderService orderService;

    @PostMapping("/orders")
    public ResponseEntity<?> order(@RequestBody OrderDto order){
        orderService.processOrder(order);

        return ResponseEntity.ok().build();
    }
}