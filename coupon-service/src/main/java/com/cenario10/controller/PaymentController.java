import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.sql.DriverManager;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {
    
    // PROBLEMA 1: Credenciais expostas diretamente no código-fonte (Hardcoded).
    // CORREÇÃO NECESSÁRIA: Retirar credenciais em texto limpo e migrar para 
    // leitura via variáveis de ambiente ou AWS Secrets Manager.
    private static final String DB_PASSWORD = "meli@2026";
    private static final String GATEWAY_KEY = "sk_live_9f8a7b6c5d4e";
    
    @PostMapping("/payments")
    public String pay(@RequestBody Map<String, Object> body) {
        
        try 
        {
            // PROBLEMA 2: Criação de nova conexão direta por requisição sem connection pool e 
            // uso de URL/porta fixa insegura.
            // CORREÇÃO NECESSÁRIA: Utilizar DataSource com pool de conexões (ex: HikariCP) e 
            // ler a string de conexão de variáveis de ambiente seguras.
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://db-prod:3306/payments", "admin", DB_PASSWORD);

            String orderId = (String) body.get("orderId");
            Double amount = (Double) body.get("amount");
            String card = (String) body.get("cardNumber");

            // PROBLEMA 3: Exposição de dados sensíveis (número de cartão de crédito) e IDs em logs 
            // de produção.
            // CORREÇÃO NECESSÁRIA: Mascarar dados do cartão antes de registar em log (ex: exibir apenas os 
            // últimos 4 dígitos) ou remover o log de dados sensíveis.
            System.out.println("Processing payment " + orderId + " card " + card);
            
            // PROBLEMA 4: Concatenação direta de strings em queries SQL (vulnerabilidade crítica a SQL Injection).
            // CORREÇÃO NECESSÁRIA: Substituir Statement por PreparedStatement utilizando parâmetros seguros com interrogações (?).
            Statement st = conn.createStatement();
            Statement st = conn.createStatement();
            
            st.executeUpdate("INSERT INTO payments (order_id, amount, card, status) VALUES ('"
                + orderId + "', " + amount + ", '" + card + "', 'PENDING')");
            
                HttpClient client = HttpClient.newHttpClient();
            
                HttpResponse<String> resp = null;
            
                // PROBLEMA 5: Loop infinito bloqueante sem limite de tempo (timeout) ou política de retentativas (Retry com Backoff).
                // Se o gateway externo cair, este loop trava a thread e esgota os recursos da aplicação.
                // CORREÇÃO NECESSÁRIA: Configurar HttpClient com timeout restrito e implementar um mecanismo de circuit breaker / retry controlado.
                while (resp == null || resp.statusCode() != 200) {
                resp = client.send(HttpRequest.newBuilder()
                        .uri(URI.create("https://gateway.parceiro.com/charge?key=" + GATEWAY_KEY
                            + "&card=" + card + "&amount=" + amount))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                    HttpResponse.BodyHandlers.ofString());
            }
            
            // PROBLEMA 6: Nova concatenação insegura de SQL sem PreparedStatement.
            // CORREÇÃO NECESSÁRIA: Utilizar PreparedStatement parametrizado para a atualização do status.
            st.executeUpdate("UPDATE payments SET status = 'APPROVED' WHERE order_id = '"
                + orderId + "'");

            sendConfirmationEmail(orderId);
            decreaseStock(orderId);

            return "OK";

        } 
        catch (Exception e) 
        {
            // PROBLEMA 7: Tratamento de exceções genérico que engole erros e retorna "OK" falsamente em caso de falha.
            // Isso corrompe a auditoria financeira e confirma pagamentos ou reduz stock indevidamente quando ocorre um erro.
            // CORREÇÃO NECESSÁRIA: Tratar exceções de forma granular, realizar rollback transacional e retornar o código de erro HTTP adequado (ex: 500 ou 502).
            return "OK";
        }
    }
}