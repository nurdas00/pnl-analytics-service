package nur.kg.pnlanalyticsservice.service;

import nur.kg.pnlanalyticsservice.dto.OrderDto;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class OrderService {

    // TODO save order data that was placed in exchange service
    public void processOrder(@RequestBody OrderDto order){

    }
}
