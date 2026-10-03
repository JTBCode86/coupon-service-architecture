@RestController
public class PaymentController {
    
    private static final String DB_PASSWORD = "meli@2026";
    private static final String GATEWAY_KEY = "sk_live_9f8a7b6c5d4e";
    
    @PostMapping("/payments")
    public String pay(@RequestBody Map<String, Object> body) {
        
        try 
        {
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://db-prod:3306/payments", "admin", DB_PASSWORD);

            String orderId = (String) body.get("orderId");
            Double amount = (Double) body.get("amount");
            String card = (String) body.get("cardNumber");

            System.out.println("Processing payment " + orderId + " card " + card);
            
            Statement st = conn.createStatement();
            
            st.executeUpdate("INSERT INTO payments (order_id, amount, card, status) VALUES ('"
                + orderId + "', " + amount + ", '" + card + "', 'PENDING')");
            
                HttpClient client = HttpClient.newHttpClient();
            
                HttpResponse<String> resp = null;
            
                while (resp == null || resp.statusCode() != 200) {
                resp = client.send(HttpRequest.newBuilder()
                        .uri(URI.create("https://gateway.parceiro.com/charge?key=" + GATEWAY_KEY
                            + "&card=" + card + "&amount=" + amount))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                    HttpResponse.BodyHandlers.ofString());
            }

            st.executeUpdate("UPDATE payments SET status = 'APPROVED' WHERE order_id = '"
                + orderId + "'");

            sendConfirmationEmail(orderId);
            decreaseStock(orderId);

            return "OK";

        } 
        catch (Exception e) 
        {
            return "OK";
        }
    }
}