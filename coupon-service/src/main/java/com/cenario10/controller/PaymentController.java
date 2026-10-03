package com.cenario10.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
public class PaymentController {

    private static final Logger LOGGER = Logger.getLogger(PaymentController.class.getName());
    
    private final DataSource dataSource;
    private final HttpClient httpClient;

    // Injeção de dependências e configuração segura de timeouts para evitar bloqueios de threads
    public PaymentController(DataSource dataSource) {
        this.dataSource = dataSource;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @PostMapping("/payments")
    public ResponseEntity<Map<String, Object>> pay(@RequestBody Map<String, Object> body) {
        String orderId = (String) body.get("orderId");
        Double amount = (Double) body.get("amount");
        String card = (String) body.get("cardNumber");

        // Validação básica de payload
        if (orderId == null || amount == null || card == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Parâmetros obrigatórios em falta"));
        }

        // Mascaramento de dados sensíveis antes de registar em log (evita expor o cartão)
        String maskedCard = maskCardNumber(card);
        LOGGER.log(Level.INFO, "A processar pagamento para o pedido: {0} com cartão: {1}", new Object[]{orderId, maskedCard});

        // Leitura segura de chaves de API externas a partir de variáveis de ambiente do sistema
        String gatewayKey = System.getenv("GATEWAY_KEY");

        // Utilização de blocos try-with-resources para garantir o fecho seguro de conexões
        try (Connection conn = dataSource.getConnection()) {
            
            // Mitigação de SQL Injection: Uso de PreparedStatement com parâmetros seguros (?)
            String insertQuery = "INSERT INTO payments (order_id, amount, card_masked, status) VALUES (?, ?, ?, 'PENDING')";
            try (PreparedStatement st = conn.prepareStatement(insertQuery)) {
                st.setString(1, orderId);
                st.setDouble(2, amount);
                st.setString(3, maskedCard);
                st.executeUpdate();
            }

            // Chamada HTTP externa com timeout restrito e tratamento adequado de resposta
            String gatewayUrl = "https://gateway.parceiro.com/charge?key=" + gatewayKey + "&card=" + card + "&amount=" + amount;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(gatewayUrl))
                    .timeout(Duration.ofSeconds(3))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                LOGGER.log(Level.WARNING, "Gateway recusou o pagamento para o pedido: {0}", orderId);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "Falha ao processar pagamento no parceiro externo"));
            }

            // Atualização de status transacional com PreparedStatement
            String updateQuery = "UPDATE payments SET status = 'APPROVED' WHERE order_id = ?";
            try (PreparedStatement updateSt = conn.prepareStatement(updateQuery)) {
                updateSt.setString(1, orderId);
                updateSt.executeUpdate();
            }

            sendConfirmationEmail(orderId);
            decreaseStock(orderId);

            return ResponseEntity.ok(Map.of("status", "OK", "orderId", orderId));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro de base de dados ao processar pedido: " + orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro interno no servidor ao registar transação"));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro inesperado ao comunicar com o gateway para o pedido: " + orderId, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Serviço temporariamente indisponível"));
        }
    }

    private String maskCardNumber(String card) {
        if (card == null || card.length() < 4) {
            return "****";
        }
        return "****-****-****-" + card.substring(card.length() - 4);
    }

    private void sendConfirmationEmail(String orderId) {
        // Lógica de envio de email
    }

    private void decreaseStock(String orderId) {
        // Lógica de diminuição de stock
    }
}