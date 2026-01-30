package nur.kg.pnlanalyticsservice.service;

import com.bybit.api.client.config.BybitApiConfig;
import com.bybit.api.client.service.BybitApiClientFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nur.kg.pnlanalyticsservice.config.BybitProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class BybitExecutionPnlService {

    private final BybitProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, BigDecimal> pnlByOrderId = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> pnlBySymbol = new ConcurrentHashMap<>();
    private volatile BigDecimal totalPnl = BigDecimal.ZERO;

    public BybitExecutionPnlService(BybitProperties props) {
        this.props = props;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        String streamDomain = props.domain().equals("TESTNET")
                ? BybitApiConfig.STREAM_TESTNET_DOMAIN
                : BybitApiConfig.STREAM_MAINNET_DOMAIN;

        var wsClient = BybitApiClientFactory
                .newInstance(props.apiKey(), props.apiSecret(), streamDomain, false)
                .newWebsocketClient(
                        5,
                        "60s",
                        this::onWsMessage
                );

        wsClient.getPrivateChannelStream(List.of("execution"), BybitApiConfig.V5_PRIVATE);

        System.out.println("[BybitExecutionPnlService] subscribed to execution");
    }

    private void onWsMessage(String json) {
        log.info("[BybitExecutionPnlService] onWsMessage: {}", json);
        try {
            JsonNode root = mapper.readTree(json);

            JsonNode topicNode = root.get("topic");
            if (topicNode == null || !"execution".equals(topicNode.asText())) {
                return;
            }

            JsonNode data = root.get("data");

            for (JsonNode exec : data) {
                processExecution(exec);
            }
        } catch (Exception e) {
            System.err.println("WS parse error: " + e.getMessage());
        }
    }

    private void processExecution(JsonNode exec) {

        String category = asText(exec, "category");
        String orderId  = asText(exec, "orderId");
        String symbol   = asText(exec, "symbol");
        String side     = asText(exec, "side");

        BigDecimal execQty   = asBig(exec, "execQty");
        BigDecimal execValue = asBig(exec, "execValue");
        BigDecimal execFee   = asBig(exec, "execFee");
        if (execFee == null) execFee = BigDecimal.ZERO;

        BigDecimal netPnl;

        if (!"spot".equalsIgnoreCase(category)) {

            BigDecimal execPnl = asBig(exec, "execPnl");
            if (execPnl == null) return;

            netPnl = execPnl.subtract(execFee);
        }

        else {

            if ("Sell".equalsIgnoreCase(side)) {
                netPnl = execValue.subtract(execFee);
            }
            else if ("Buy".equalsIgnoreCase(side)) {
                netPnl = execValue.negate().subtract(execFee);
            }
            else return;
        }

        if (orderId != null) {
            pnlByOrderId.merge(orderId, netPnl, BigDecimal::add);
        }
        if (symbol != null) {
            pnlBySymbol.merge(symbol, netPnl, BigDecimal::add);
        }
        synchronized (this) {
            totalPnl = totalPnl.add(netPnl);
        }

        System.out.printf(
                "EXECUTION: [%s] %s %s qty=%s value=%s fee=%s pnl=%s%n",
                category, symbol, side, execQty, execValue, execFee, netPnl
        );
    }


    private String asText(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v != null && !v.isNull()) ? v.asText() : null;
    }

    private BigDecimal asBig(JsonNode node, String field) {
        String s = asText(node, field);
        if (s == null || s.isEmpty()) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public BigDecimal getRealisedPnlForOrder(String orderId) {
        return pnlByOrderId.getOrDefault(orderId, BigDecimal.ZERO);
    }

    public BigDecimal getRealisedPnlForSymbol(String symbol) {
        return pnlBySymbol.getOrDefault(symbol, BigDecimal.ZERO);
    }

    public BigDecimal getTotalRealisedPnl() {
        return totalPnl;
    }
}
