# Projeto Final de Arquitetura de Software - Cenário 10

Repositório dedicado à entrega do projeto final de arquitetura de microsserviços, focado no **Cenário 10: Domínio de Cupons e Promoções** sob alto tráfego (200k requisições em 5 minutos e teto rígido de 10k usos).

## 📌 Autor
* **Jean Teixeira Barbosa**

---

## 🔍 Sobre o Repositório e Code Review (Seção 8)
Este repositório armazena a implementação e a refatoração do microsserviço de pagamentos e cupons. O histórico de commits foi estruturado para evidenciar o processo de **Code Review**, destacando a correção de dívidas técnicas críticas encontradas no código legado:

1. **Remoção de Credenciais Hardcoded:** Substituição por variáveis de ambiente seguras.
2. **Mitigação de SQL Injection:** Substituição de concatenação de strings por `PreparedStatement`.
3. **Resiliência e Tratamento de Erros:** Implementação de políticas de timeout, tratamento de exceções adequadas e prevenção de loops sem backoff.
4. **Comentários Explicativos:** O código refatorado contém anotações explícitas (marcadores `[CORREÇÃO SEÇÃO 8]`) para facilitar a auditoria e correção da banca avaliadora.

---

## ⚙️ Arquitetura Resumida
* **Controle de Estoque Atômico:** Redis Cluster para garantir o teto exato de 10.000 utilizações sem concorrência destrutiva.
* **Persistência e Auditoria:** PostgreSQL com transações ACID isoladas.
* **Mensageria Assíncrona:** Kafka para desacoplamento e eventos de estorno (caso ocorram os 10% de recusas de pagamento pós-aplicação).
